package com.ccds.water.water.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源出水视频。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterVideoDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 消防站主键。
     */
    private Long stationId;

    /**
     * 水源主键。
     */
    private Long waterId;

    /**
     * 视频文件主键。
     */
    private Long fileId;

    /**
     * 客户端上报时长毫秒。
     */
    private Integer durationMs;

    /**
     * 三帧文件主键。
     */
    private String frameFileIds;

    /**
     * 判定结果。
     */
    private String checkStatus;

    /**
     * 不通过原因。
     */
    private String checkReason;

    /**
     * 是否客户端上报。
     */
    private Integer clientReported;

    /**
     * 上传人账号主键。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
