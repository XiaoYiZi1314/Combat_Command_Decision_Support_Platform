package com.ccds.attack.attack.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.attack.attack.dto.AttackEventCommand;
import com.ccds.attack.attack.service.AttackService;
import com.ccds.attack.attack.service.StationStateService;
import com.ccds.attack.attack.vo.AttackEventResultVO;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.attack.attack.vo.ScbaThresholdVO;
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

    private final StationStateService stationStateService;

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
     * 指挥端快照。仅可见站。无 since 时只返回有未撤出卡片的站。
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

    /**
     * 读取站级作业模式。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 模式 test/training/dispatch
     */
    @GetMapping(ApiPathConstant.STATION_WORK_MODE)
    public ApiResultVO<String> workMode(HttpServletRequest request, @PathVariable Long stationId) {
        stationStateService.requireVisible(current(request), stationId);
        return ApiResultVO.ok(stationStateService.getWorkMode(stationId));
    }

    /**
     * 站级改作业模式。指挥端只读，会返回 403。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param body      请求体，含 workMode
     * @return 保存后的模式
     */
    @PutMapping(ApiPathConstant.STATION_WORK_MODE)
    public ApiResultVO<String> updateWorkMode(HttpServletRequest request,
                                              @PathVariable Long stationId,
                                              @RequestBody java.util.Map<String, String> body) {
        String mode = body == null ? null : body.get("workMode");
        return ApiResultVO.ok(stationStateService.updateWorkMode(current(request), stationId, mode));
    }

    /**
     * 读取站级空呼阈值。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 阈值
     */
    @GetMapping(ApiPathConstant.STATION_SCBA_SETTINGS)
    public ApiResultVO<ScbaThresholdVO> scbaSettings(HttpServletRequest request, @PathVariable Long stationId) {
        stationStateService.requireVisible(current(request), stationId);
        return ApiResultVO.ok(stationStateService.getThresholdVo(stationId));
    }

    /**
     * 保存站级空呼阈值。指挥端只读，会返回 403。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   阈值
     * @return 保存后的阈值
     */
    @PutMapping(ApiPathConstant.STATION_SCBA_SETTINGS)
    public ApiResultVO<ScbaThresholdVO> saveScbaSettings(HttpServletRequest request,
                                                         @PathVariable Long stationId,
                                                         @Valid @RequestBody ScbaThresholdVO command) {
        return ApiResultVO.ok(stationStateService.saveThresholds(current(request), stationId, command));
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
