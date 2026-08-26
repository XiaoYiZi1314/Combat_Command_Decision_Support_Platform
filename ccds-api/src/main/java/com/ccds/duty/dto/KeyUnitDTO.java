package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 重点单位预案DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeyUnitDTO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 所属消防站ID，以路径为准。
     */
    private Long stationId;

    /**
     * 单位名称
     */
    @NotBlank(message = "单位名称不能为空")
    @Size(max = 200, message = "单位名称最多200字符")
    private String name;

    /**
     * 地址
     */
    @Size(max = 500, message = "地址最多500字符")
    private String address;

    /**
     * 类别
     */
    @NotBlank(message = "类别不能为空")
    private String category;

    /**
     * 联系人
     */
    @Size(max = 50, message = "联系人最多50字符")
    private String contact;

    /**
     * 联系电话明文，仅本站可写账号返回。
     */
    @Size(max = 20, message = "联系电话最多20字符")
    @ToString.Exclude
    private String phone;

    /**
     * 联系电话脱敏值。
     */
    private String phoneMasked;

    /**
     * 备注
     */
    @Size(max = 1000, message = "备注最多1000字符")
    private String notes;

    /**
     * 预案正文
     */
    private String planText;

    /**
     * 预案文件列表
     */
    private List<FileObjectDTO> planFiles;

    /**
     * 平面图列表
     */
    private List<FileObjectDTO> floorPlans;
}
