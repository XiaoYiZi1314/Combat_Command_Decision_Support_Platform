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
 * 改密入参。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"oldPassword", "newPassword"})
public class ChangePasswordCommand {

    /**
     * 当前口令。
     */
    @NotBlank
    @Size(max = AuthRuleConstant.PASSWORD_MAX_LENGTH)
    private String oldPassword;

    /**
     * 新口令，不少于 8 位。
     */
    @NotBlank
    @Size(min = AuthRuleConstant.MIN_PASSWORD_LENGTH, max = AuthRuleConstant.PASSWORD_MAX_LENGTH)
    private String newPassword;
}
