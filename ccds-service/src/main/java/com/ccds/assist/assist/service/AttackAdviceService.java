package com.ccds.assist.assist.service;

import com.ccds.assist.assist.dto.AttackAdviceCommand;
import com.ccds.assist.assist.dto.AttackAdviceResultDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 内攻 AI 研判。人员字段走白名单，禁止电话与完整花名册。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AttackAdviceService {

    /**
     * 对本站未撤出人员做辅助研判。
     *
     * @param principal 当前身份
     * @param command   研判入参
     * @return 轮换、撤离、风险提示
     */
    AttackAdviceResultDTO advise(AuthPrincipal principal, AttackAdviceCommand command);
}
