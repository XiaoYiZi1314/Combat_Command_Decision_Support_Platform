package com.ccds.infra.weather;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ccds.duty.constant.WeatherRuleConstant;
import com.ccds.duty.dto.WeatherDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Open-Meteo 天气代理客户端。仅向业务层返回白名单天气字段。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
public class WeatherClientImpl implements WeatherClient {

    private static final String MSG_NOT_CONFIGURED = "weather proxy not configured";
    private static final String MSG_CALL_FAILED = "weather proxy failed";
    private static final String TIMEZONE = "Asia/Shanghai";
    private static final String CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,weather_code,"
            + "surface_pressure,wind_speed_10m,wind_direction_10m";
    private static final String HOURLY_FIELDS = "temperature_2m,weather_code,precipitation_probability";
    private static final String DAILY_FIELDS = "sunrise,sunset";
    private static final String[] WIND_DIRECTIONS = {
            "北", "东北", "东", "东南", "南", "西南", "西", "西北"
    };

    private final WeatherProperties weatherProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Object cacheLock = new Object();
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>();

    /**
     * 构造天气客户端。
     *
     * @param weatherProperties 天气上游配置
     * @param objectMapper      JSON 解析器
     */
    public WeatherClientImpl(WeatherProperties weatherProperties, ObjectMapper objectMapper) {
        this.weatherProperties = weatherProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(WeatherRuleConstant.CONNECT_TIMEOUT_MS))
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public WeatherDTO getWeather(Double lng, Double lat) {
        if (!hasText(weatherProperties.getBaseUrl())) {
            throw new IllegalStateException(MSG_NOT_CONFIGURED);
        }
        String cacheKey = cacheKey(lng, lat);
        synchronized (cacheLock) {
            CacheEntry hit = cache.get(cacheKey);
            if (hit != null && Instant.now().isBefore(hit.expireAt)) {
                return hit.weather;
            }
        }
        WeatherDTO weather = fetchOnceAndLog(lng, lat);
        synchronized (cacheLock) {
            cache.put(cacheKey, new CacheEntry(weather,
                    Instant.now().plusSeconds(WeatherRuleConstant.CACHE_TTL_SECONDS)));
            while (cache.size() > WeatherRuleConstant.CACHE_MAX_ENTRIES) {
                cache.remove(cache.keySet().iterator().next());
            }
        }
        return weather;
    }

    private WeatherDTO fetchOnceAndLog(Double lng, Double lat) {
        long started = System.currentTimeMillis();
        WeatherDTO weather = fetchOnce(lng, lat);
        log.info("天气代理调用成功 elapsedMs={}", System.currentTimeMillis() - started);
        return weather;
    }

    private WeatherDTO fetchOnce(Double lng, Double lat) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(buildUrl(lng, lat)))
                    .timeout(Duration.ofMillis(WeatherRuleConstant.READ_TIMEOUT_MS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        }
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("weather http " + status);
            }
            return parseBody(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        } catch (IOException ex) {
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        }
    }

    private String buildUrl(Double lng, Double lat) {
        String base = weatherProperties.getBaseUrl().trim();
        String joiner = base.contains("?") ? "&" : "?";
        return base + joiner
                + "latitude=" + encode(lat)
                + "&longitude=" + encode(lng)
                + "&current=" + encode(CURRENT_FIELDS)
                + "&hourly=" + encode(HOURLY_FIELDS)
                + "&daily=" + encode(DAILY_FIELDS)
                + "&timezone=" + encode(TIMEZONE)
                + "&forecast_days=2";
    }

    private String encode(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

    private WeatherDTO parseBody(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode current = root.path("current");
            if (!current.isObject() || current.path("temperature_2m").isMissingNode()) {
                throw new IllegalStateException(MSG_CALL_FAILED);
            }
            double windDeg = current.path("wind_direction_10m").asDouble();
            return WeatherDTO.builder()
                    .location("定位点附近")
                    .temperature(number(current, "temperature_2m"))
                    .description(weatherDescription(current.path("weather_code").asInt(-1)))
                    .windDirection(windDirection(windDeg))
                    .windDeg(windDeg)
                    .windSpeed(toMetresPerSecond(number(current, "wind_speed_10m")))
                    .humidity(integer(current, "relative_humidity_2m"))
                    .pressure(integer(current, "surface_pressure"))
                    .sunrise(firstDateTime(root.path("daily").path("sunrise")))
                    .sunset(firstDateTime(root.path("daily").path("sunset")))
                    .hourlyForecast(hourly(root))
                    .updateTime(parseDateTime(current.path("time").asText(null)))
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        }
    }

    private List<WeatherDTO.HourlyForecast> hourly(JsonNode root) {
        JsonNode hourly = root.path("hourly");
        JsonNode times = hourly.path("time");
        JsonNode temperatures = hourly.path("temperature_2m");
        JsonNode weatherCodes = hourly.path("weather_code");
        JsonNode precipitation = hourly.path("precipitation_probability");
        LocalDateTime currentTime = parseDateTime(root.path("current").path("time").asText(null));
        int start = firstForecastIndex(times, currentTime);
        int end = Math.min(times.size(), start + WeatherRuleConstant.HOURLY_LIMIT);
        List<WeatherDTO.HourlyForecast> result = new ArrayList<>(Math.max(0, end - start));
        for (int i = start; i < end; i++) {
            result.add(WeatherDTO.HourlyForecast.builder()
                    .time(parseDateTime(times.path(i).asText(null)))
                    .temperature(nodeNumber(temperatures.path(i)))
                    .description(weatherDescription(weatherCodes.path(i).asInt(-1)))
                    .precipProbability(nodeInteger(precipitation.path(i)))
                    .build());
        }
        return result;
    }

    private int firstForecastIndex(JsonNode times, LocalDateTime currentTime) {
        if (!times.isArray() || currentTime == null) {
            return 0;
        }
        for (int i = 0; i < times.size(); i++) {
            LocalDateTime time = parseDateTime(times.path(i).asText(null));
            if (time != null && !time.isBefore(currentTime)) {
                return i;
            }
        }
        return 0;
    }

    private Double toMetresPerSecond(Double kilometresPerHour) {
        if (kilometresPerHour == null) {
            return null;
        }
        return BigDecimal.valueOf(kilometresPerHour
                        / WeatherRuleConstant.KILOMETRES_PER_HOUR_TO_METRES_PER_SECOND)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String windDirection(double degrees) {
        double normalized = ((degrees % 360D) + 360D) % 360D;
        int index = (int) Math.round(normalized / 45D) % WIND_DIRECTIONS.length;
        return WIND_DIRECTIONS[index];
    }

    private String weatherDescription(int code) {
        if (code == 0) {
            return "晴";
        }
        if (code >= 1 && code <= 3) {
            return "多云";
        }
        if (code == 45 || code == 48) {
            return "雾";
        }
        if (code >= 51 && code <= 57) {
            return "毛毛雨";
        }
        if (code >= 61 && code <= 67) {
            return "雨";
        }
        if (code >= 71 && code <= 77) {
            return "雪";
        }
        if (code >= 80 && code <= 82) {
            return "阵雨";
        }
        if (code == 85 || code == 86) {
            return "阵雪";
        }
        if (code >= 95 && code <= 99) {
            return "雷暴";
        }
        return "未知天气";
    }

    private String cacheKey(Double lng, Double lat) {
        BigDecimal safeLng = BigDecimal.valueOf(lng)
                .setScale(WeatherRuleConstant.CACHE_COORD_SCALE, RoundingMode.HALF_UP);
        BigDecimal safeLat = BigDecimal.valueOf(lat)
                .setScale(WeatherRuleConstant.CACHE_COORD_SCALE, RoundingMode.HALF_UP);
        return safeLng.toPlainString() + ":" + safeLat.toPlainString();
    }

    private Double number(JsonNode parent, String field) {
        return nodeNumber(parent.path(field));
    }

    private Double nodeNumber(JsonNode node) {
        return node.isNumber() ? node.asDouble() : null;
    }

    private Integer integer(JsonNode parent, String field) {
        return nodeInteger(parent.path(field));
    }

    private Integer nodeInteger(JsonNode node) {
        return node.isNumber() ? (int) Math.round(node.asDouble()) : null;
    }

    private LocalDateTime firstDateTime(JsonNode values) {
        return values.isArray() && !values.isEmpty()
                ? parseDateTime(values.path(0).asText(null)) : null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class CacheEntry {

        private final WeatherDTO weather;
        private final Instant expireAt;

        private CacheEntry(WeatherDTO weather, Instant expireAt) {
            this.weather = weather;
            this.expireAt = expireAt;
        }
    }
}
