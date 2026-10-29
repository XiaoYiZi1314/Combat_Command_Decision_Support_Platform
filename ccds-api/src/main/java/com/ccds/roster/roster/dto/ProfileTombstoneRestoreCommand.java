package com.ccds.roster.roster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 恢复已删除人员记录。
 *
 * <p>按姓名误删只允许解开姓名那一条（scope=name），不得把整张卡的档案号与 NFC 记录一起清掉；
 * scope=nfc 必须显式确认；scope=all 仅用于用户主动恢复整卡。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTombstoneRestoreCommand {

    /**
     * 恢复范围：name / nfc / all。
     */
    @NotBlank
    @Pattern(regexp = "^(name|nfc|all)$")
    private String scope;

    /**
     * 姓名。
     */
    @NotBlank
    @Size(max = 32)
    private String name;

    /**
     * NFC 编号，scope=nfc 时必填。
     */
    @Size(max = 64)
    private String nfcTag;
}
