package com.ccds.iam.identity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 登录成功结果。不含口令。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"accessToken", "refreshToken"})
public class LoginVO {

    /**
     * 短寿访问令牌。
     */
    private String accessToken;

    /**
     * 刷新令牌。
     */
    private String refreshToken;

    /**
     * 访问令牌剩余秒数。
     */
    private Long accessExpiresIn;

    /**
     * 是否必须改密。
     */
    private Boolean mustChangePassword;

    /**
     * 当前账号摘要。
     */
    private MeVO me;
}
