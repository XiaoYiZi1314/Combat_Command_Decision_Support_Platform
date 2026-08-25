package com.ccds.infra.weather;

import com.ccds.duty.dto.WeatherDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 天气API客户端（代理第三方天气服务）
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Service
public class WeatherClient {

    private final RestTemplate restTemplate;

    public WeatherClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 获取天气信息
     * 
     * @param lng 经度
     * @param lat 纬度
     * @return 天气信息
     */
    public WeatherDTO getWeather(Double lng, Double lat) {
        try {
            // TODO: 接入实际天气API（如和风天气、心知天气等）
            // 此处为示例实现，实际应替换为真实API
            log.info("查询天气：lng={}, lat={}", lng, lat);
            
            // 模拟返回数据
            return createMockWeather(lng, lat);
        } catch (Exception e) {
            log.error("获取天气失败：lng={}, lat={}", lng, lat, e);
            throw new RuntimeException("获取天气信息失败", e);
        }
    }

    /**
     * 创建模拟天气数据（仅用于开发测试）
     */
    private WeatherDTO createMockWeather(Double lng, Double lat) {
        WeatherDTO weather = new WeatherDTO();
        weather.setLocation("当前位置");
        weather.setTemperature(18.0);
        weather.setDescription("多云");
        weather.setWindDirection("东北风");
        weather.setWindSpeed(3.5);
        weather.setHumidity(65);
        weather.setPressure(1013);
        weather.setSunrise(LocalDateTime.now().withHour(6).withMinute(30));
        weather.setSunset(LocalDateTime.now().withHour(18).withMinute(30));
        weather.setUpdateTime(LocalDateTime.now());

        // 模拟小时预报
        List<WeatherDTO.HourlyForecast> hourlyForecast = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            WeatherDTO.HourlyForecast forecast = new WeatherDTO.HourlyForecast();
            forecast.setTime(LocalDateTime.now().plusHours(i));
            forecast.setTemperature(18.0 + i * 0.5);
            forecast.setDescription(i % 3 == 0 ? "小雨" : "多云");
            forecast.setPrecipProbability(i % 3 == 0 ? 30 : 10);
            hourlyForecast.add(forecast);
        }
        weather.setHourlyForecast(hourlyForecast);

        return weather;
    }
}
