package com.ccds.roster.roster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交人员跨站调动。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTransferCommand {

    /**
     * 调出站主键。
     */
    @NotNull
    private Long fromStationId;

    /**
     * 调入站主键。
     */
    @NotNull
    private Long toStationId;

    /**
     * 调出站人员主键。
     */
    @NotNull
    private Long profileId;

    /**
     * 并卡目标人员主键；只有调入站已有同名卡时才允许填写。
     */
    private Long targetProfileId;

    /**
     * 调动原因。
     */
    @NotBlank
    @Size(max = 255)
    private String reason;

    /**
     * 客户端请求号，用于提交幂等，可空。
     */
    @Size(max = 64)
    private String clientRequestId;
}
