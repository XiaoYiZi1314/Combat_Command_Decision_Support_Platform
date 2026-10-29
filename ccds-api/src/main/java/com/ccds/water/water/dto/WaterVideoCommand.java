package com.ccds.water.water.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登记水源出水视频。
 *
 * <p>时长与三帧由客户端抽取后上报：服务端没有媒体库，且仓库红线禁止 {@code Runtime.exec}，
 * 因此时长校验与抽帧只能在壳/H5 完成，服务端只做入库与结论记录。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterVideoCommand {

    /**
     * 视频文件主键（COS 三步上传后的 fileId）。
     */
    @NotNull
    private Long fileId;

    /**
     * 客户端上报时长毫秒。
     */
    @NotNull
    private Integer durationMs;

    /**
     * 开头/中间/结尾三帧文件主键，逗号分隔。
     */
    @Size(max = 255)
    private String frameFileIds;

    /**
     * AI 判定结果：passed / rejected / failed，可空（空视为 failed）。
     */
    @Size(max = 16)
    private String checkStatus;

    /**
     * 不通过原因，rejected 时必填。
     */
    @Size(max = 255)
    private String checkReason;
}
