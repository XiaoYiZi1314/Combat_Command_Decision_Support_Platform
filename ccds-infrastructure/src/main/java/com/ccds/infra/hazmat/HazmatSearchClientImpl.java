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
        int safeLimit = Math.min(Math.max(limit, 1), AssistRuleConstant.SEARCH_MAX_LIMIT);
        if (!hasText(hazmatProperties.getSearchUrl())) {
            log.warn("危化品检索未配置外部库，使用本地常见物质数据");
            return fallback(query, safeLimit);
        }
        String cacheKey = query.trim().toLowerCase(Locale.ROOT) + ":" + safeLimit;
        synchronized (cacheLock) {
            CacheEntry hit = cache.get(cacheKey);
            if (hit != null && Instant.now().isBefore(hit.expireAt)) {
                return hit.result;
            }
        }
        HazmatSearchResultDTO result;
        try {
            result = fetchWithRetry(query.trim(), safeLimit);
        } catch (IllegalStateException ex) {
            log.warn("危化品检索上游不可用，使用本地常见物质数据", ex);
            result = fallback(query.trim(), safeLimit);
        }
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

    private HazmatSearchResultDTO fallback(String query, int limit) {
        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<HazmatSearchItemDTO> matched = fallbackItems().stream()
                .filter(item -> contains(item.getName(), keyword)
                        || contains(item.getAlias(), keyword)
                        || contains(item.getUnNumber(), keyword)
                        || contains(item.getCasNumber(), keyword))
                .limit(limit)
                .toList();
        return HazmatSearchResultDTO.builder()
                .query(query)
                .count(matched.size())
                .items(matched)
                .build();
    }

    private List<HazmatSearchItemDTO> fallbackItems() {
        return List.of(
                item("汽油", "车用汽油", "1203", "8006-61-9", "高度易燃液体，蒸气可与空气形成爆炸性混合物。", "切断火源，禁用非防爆设备；冷却容器并防止流入下水道。"),
                item("柴油", "轻柴油", "1202", "68334-30-5", "可燃液体，受热产生可燃蒸气。", "隔离火源，使用泡沫或干粉灭火，吸附收集泄漏物。"),
                item("液化石油气", "LPG", "1075", "68476-85-7", "极易燃压缩液化气体，受热容器有爆炸危险。", "上风向警戒，关闭阀门并持续冷却，严禁直接接触泄漏气体。"),
                item("氯气", "液氯", "1017", "7782-50-5", "剧毒、强氧化性气体，吸入有致命危险。", "人员向上风向撤离，专业人员佩戴正压空气呼吸器处置。"),
                item("氨", "液氨", "1005", "7664-41-7", "有毒、腐蚀性气体，具有刺激性气味。", "上风向疏散，水雾吸收蒸气，避免进入低洼和密闭空间。"),
                item("盐酸", "氢氯酸", "1789", "7647-01-0", "腐蚀性液体，与金属反应产生氢气。", "佩戴防酸装备，使用惰性材料围堵，严禁与碱类混合。"),
                item("硫酸", "浓硫酸", "1830", "7664-93-9", "强腐蚀性和强脱水性液体，与水混合剧烈放热。", "耐酸防护，少量多次沿器壁处理，严禁将水直接倒入酸中。"),
                item("甲醇", "木醇", "1230", "67-56-1", "高度易燃且有毒，蒸气可造成中毒。", "切断火源并加强通风，使用抗溶性泡沫、干粉或二氧化碳灭火。"));
    }

    private HazmatSearchItemDTO item(String name, String alias, String un, String cas,
                                     String hazard, String response) {
        return HazmatSearchItemDTO.builder().name(name).alias(alias).unNumber(un).casNumber(cas)
                .hazardSummary(hazard).responseHint(response).build();
    }

    private boolean contains(String value, String keyword) {
        return value != null && !keyword.isEmpty() && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
    private HazmatSearchResultDTO emptyResult(String query) {
        return HazmatSearchResultDTO.builder()
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
