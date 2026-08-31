package com.ccds.attack.attack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 内攻归档入参：事件类型、名称与地点。
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
     * 类型：drill演练 / dispatch出警。
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
}
