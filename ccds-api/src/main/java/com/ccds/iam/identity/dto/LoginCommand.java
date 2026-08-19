package com.ccds.iam.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.ccds.iam.identity.constant.AuthRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 登录入参。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class LoginCommand {

    /**
     * 登录名。
     */
    @NotBlank
    @Size(max = AuthRuleConstant.USERNAME_MAX_LENGTH)
    private String username;

    /**
     * 明文口令，仅传输，禁止落日志。
     */
    @NotBlank
    @Size(max = AuthRuleConstant.PASSWORD_MAX_LENGTH)
    private String password;

    /**
     * 设备提示，不含通信标识。
     */
    @Size(max = AuthRuleConstant.DEVICE_HINT_MAX_LENGTH)
    private String deviceHint;
}
