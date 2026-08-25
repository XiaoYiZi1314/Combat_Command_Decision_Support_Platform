package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建共享令牌响应DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShareTokenResponseDTO {

    /**
     * 共享令牌ID
     */
    private Long tokenId;

    /**
     * 共享令牌(明文，仅创建时返回)
     */
    private String token;

    /**
     * 完整共享URL
     */
    private String shareUrl;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;
}
