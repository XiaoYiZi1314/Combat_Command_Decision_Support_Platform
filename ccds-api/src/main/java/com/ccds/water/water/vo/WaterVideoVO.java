package com.ccds.water.water.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源出水视频视图。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterVideoVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 水源主键。
     */
    private Long waterId;

    /**
     * 视频文件主键。
     */
    private Long fileId;

    /**
     * 视频时长毫秒。
     */
    private Integer durationMs;

    /**
     * 三帧文件主键。
     */
    private String frameFileIds;

    /**
     * 判定结果 passed/rejected/failed。
     */
    private String checkStatus;

    /**
     * 不通过原因。
     */
    private String checkReason;

    /**
     * 是否由客户端上报（本期恒 true）。
     */
    private Boolean clientReported;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;
}
