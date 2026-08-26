package com.ccds.infra.hazmat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.HazmatSearchItemDTO;
import com.ccds.assist.assist.dto.HazmatSearchResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 危化品检索：后端转发上游并做短时缓存。未配置则拒绝，禁止返回假条目。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
public class HazmatSearchClientImpl implements HazmatSearchClient {

    private static final String MSG_NOT_CONFIGURED = "hazmat search not configured";

    private static final String MSG_CALL_FAILED = "hazmat search failed";

    private final HazmatProperties hazmatProperties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final Object cacheLock = new Object();

    private final Map<String, CacheEntry> cache = new LinkedHashMap<>();

    /**
     * 构造检索客户端。
     *
     * @param hazmatProperties 上游地址
     * @param objectMapper     JSON
     */
    public HazmatSearchClientImpl(HazmatProperties hazmatProperties, ObjectMapper objectMapper) {
        this.hazmatProperties = hazmatProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(AssistRuleConstant.SEARCH_CONNECT_TIMEOUT_MS))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HazmatSearchResultDTO search(String query, int limit) {
        if (!hasText(hazmatProperties.getSearchUrl())) {
            log.warn("危化品检索未配置外部库");
            throw new IllegalStateException(MSG_NOT_CONFIGURED);
        }
        int safeLimit = Math.min(Math.max(limit, 1), AssistRuleConstant.SEARCH_MAX_LIMIT);
        String cacheKey = query.trim().toLowerCase(Locale.ROOT) + ":" + safeLimit;
        synchronized (cacheLock) {
            CacheEntry hit = cache.get(cacheKey);
            if (hit != null && Instant.now().isBefore(hit.expireAt)) {
                return hit.result;
            }
        }
        HazmatSearchResultDTO result = fetchWithRetry(query.trim(), safeLimit);
        synchronized (cacheLock) {
            cache.put(cacheKey, new CacheEntry(result,
                    Instant.now().plusSeconds(AssistRuleConstant.SEARCH_CACHE_TTL_SECONDS)));
            while (cache.size() > AssistRuleConstant.SEARCH_CACHE_MAX_ENTRIES) {
                String eldest = cache.keySet().iterator().next();
                cache.remove(eldest);
            }
        }
        return result;
    }

    private HazmatSearchResultDTO fetchWithRetry(String query, int limit) {
        IllegalStateException last = null;
        for (int attempt = 1; attempt <= AssistRuleConstant.SEARCH_RETRY_MAX; attempt++) {
            long started = System.currentTimeMillis();
            try {
                HazmatSearchResultDTO result = fetchOnce(query, limit);
                log.info("危化品检索成功 queryLength={} count={} attempt={} elapsedMs={}",
                        query.length(), result.getCount(), attempt, System.currentTimeMillis() - started);
                return result;
            } catch (RetryableCallException ex) {
                last = ex;
                log.warn("危化品检索可重试失败 attempt={}", attempt, ex);
            }
        }
        if (last == null) {
            throw new IllegalStateException(MSG_CALL_FAILED);
        }
        throw last;
    }

    private HazmatSearchResultDTO fetchOnce(String query, int limit) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(buildUrl(query, limit)))
                    .timeout(Duration.ofMillis(AssistRuleConstant.SEARCH_READ_TIMEOUT_MS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        }
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return parseBody(query, response.body(), limit);
            }
            if (status == 429 || status >= 500) {
                throw new RetryableCallException("hazmat http " + status);
            }
            log.error("危化品检索失败 status={}", status);
            throw new IllegalStateException(MSG_CALL_FAILED);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        } catch (IOException ex) {
            throw new RetryableCallException(MSG_CALL_FAILED, ex);
        }
    }

    private String buildUrl(String query, int limit) {
        String base = hazmatProperties.getSearchUrl().trim();
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String joiner = base.contains("?") ? "&" : "?";
        return base + joiner + "q=" + encoded + "&limit=" + limit;
    }

    private HazmatSearchResultDTO parseBody(String query, String body, int limit) {
        List<HazmatSearchItemDTO> items = new ArrayList<>();
        if (!hasText(body)) {
            return emptyResult(query);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode list = extractList(root);
            if (list != null && list.isArray()) {
                int max = Math.min(list.size(), limit);
                for (int i = 0; i < max; i++) {
                    HazmatSearchItemDTO item = toItem(list.get(i));
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        }
        return HazmatSearchResultDTO.builder()
                .query(query)
                .count(items.size())
                .items(items)
                .build();
    }

    private JsonNode extractList(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.has("items") && root.get("items").isArray()) {
            return root.get("items");
        }
        if (root.has("data") && root.get("data").isArray()) {
            return root.get("data");
        }
        if (root.has("list") && root.get("list").isArray()) {
            return root.get("list");
        }
        return null;
    }

    private HazmatSearchItemDTO toItem(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String name = firstText(node, "name", "chemicalName", "cnName");
        if (!hasText(name)) {
            return null;
        }
        return HazmatSearchItemDTO.builder()
                .name(name)
                .alias(firstText(node, "alias", "synonym", "popularName"))
                .unNumber(firstText(node, "unNumber", "un", "unNo", "un_no"))
                .casNumber(firstText(node, "casNumber", "cas", "casNo", "cas_no"))
                .hazardSummary(firstText(node, "hazardSummary", "hazard", "danger"))
                .responseHint(firstText(node, "responseHint", "advice", "disposal"))
                .build();
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isValueNode() && hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private HazmatSearchResultDTO emptyResult(String query) {
        return HazmatSearchResultDTO.builder()
                .query(query)
                .count(0)
                .items(List.of())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class CacheEntry {

        private final HazmatSearchResultDTO result;

        private final Instant expireAt;

        private CacheEntry(HazmatSearchResultDTO result, Instant expireAt) {
            this.result = result;
            this.expireAt = expireAt;
        }
    }

    private static final class RetryableCallException extends IllegalStateException {

        private RetryableCallException(String message) {
            super(message);
        }

        private RetryableCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
