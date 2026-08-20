package com.ccds.attack.attack.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.attack.attack.dto.AttackEventCommand;
import com.ccds.attack.attack.service.AttackService;
import com.ccds.attack.attack.vo.AttackEventResultVO;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.attack.attack.vo.StationAttackVO;
import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 内攻卡片、事件与指挥快照接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class AttackController {

    private final AttackService attackService;

    /**
     * 本站卡片与快照。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 本站内攻
     */
    @GetMapping(ApiPathConstant.STATION_ATTACK)
    public ApiResultVO<StationAttackVO> stationAttack(HttpServletRequest request, @PathVariable Long stationId) {
        return ApiResultVO.ok(attackService.getStationAttack(current(request), stationId));
    }

    /**
     * 预录入 / 入场 / 复测 / 撤出 / 删除。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   事件
     * @return 写结果
     */
    @PostMapping(ApiPathConstant.STATION_ATTACK_EVENTS)
    public ApiResultVO<AttackEventResultVO> submitEvent(HttpServletRequest request,
                                                        @PathVariable Long stationId,
                                                        @Valid @RequestBody AttackEventCommand command) {
        return ApiResultVO.ok(attackService.submitEvent(current(request), stationId, command));
    }

    /**
     * 指挥端快照。仅可见站。无 since 时只返回正在内攻人数大于 0 的站。
     *
     * @param request HTTP 请求
     * @param since   增量起点，ISO-8601 本地时间
     * @return 快照列表
     */
    @GetMapping(ApiPathConstant.ATTACK_SNAPSHOTS)
    public ApiResultVO<List<AttackSnapshotVO>> snapshots(
            HttpServletRequest request,
            @RequestParam(value = "since", required = false) String since) {
        return ApiResultVO.ok(attackService.listSnapshots(current(request), since));
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
