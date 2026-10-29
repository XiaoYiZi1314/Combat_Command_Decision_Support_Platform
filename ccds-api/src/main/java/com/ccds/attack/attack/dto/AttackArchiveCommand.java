package com.ccds.attack.attack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 内攻归档入参：事件类型、名称、地点与本次行动的作业模式。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttackArchiveCommand {

    /**
     * 类型：training训练 / dispatch出警。drill 作为旧端别名兼容。
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventKind;

    /**
     * 事件名称。
     */
    @Size(max = 100, message = "事件名称最多100字符")
    private String eventName;

    /**
     * 地点。
     */
    @Size(max = 200, message = "地点最多200字符")
    private String location;

    /**
     * 归档时的作业模式：test / training / dispatch。缺省按 test。
     */
    @Pattern(regexp = "^(test|training|dispatch)$", message = "作业模式须为 test/training/dispatch")
    private String mode;
}
