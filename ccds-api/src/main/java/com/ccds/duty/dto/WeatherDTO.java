package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 天气信息DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherDTO {

    /**
     * 位置名称
     */
    private String location;

    /**
     * 当前温度(摄氏度)
     */
    private Double temperature;

    /**
     * 天气描述
     */
    private String description;

    /**
     * 风向
     */
    private String windDirection;

    /**
     * 风向角度。
     */
    private Double windDeg;

    /**
     * 风速(m/s)
     */
    private Double windSpeed;

    /**
     * 湿度(%)
     */
    private Integer humidity;

    /**
     * 气压(hPa)
     */
    private Integer pressure;

    /**
     * 日出时间
     */
    private LocalDateTime sunrise;

    /**
     * 日落时间
     */
    private LocalDateTime sunset;

    /**
     * 小时预报
     */
    private List<HourlyForecast> hourlyForecast;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 小时预报
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyForecast {
        /**
         * 时间
         */
        private LocalDateTime time;

        /**
         * 温度(摄氏度)
         */
        private Double temperature;

        /**
         * 天气描述
         */
        private String description;

        /**
         * 降水概率(%)
         */
        private Integer precipProbability;
    }
}
