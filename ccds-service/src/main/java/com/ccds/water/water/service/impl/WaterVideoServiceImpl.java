package com.ccds.water.water.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.water.water.constant.WaterRuleConstant;
import com.ccds.water.water.dto.WaterVideoCommand;
import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.entity.WaterVideoDO;
import com.ccds.water.water.mapper.WaterSourceMapper;
import com.ccds.water.water.mapper.WaterVideoMapper;
import com.ccds.water.water.service.WaterAccessService;
import com.ccds.water.water.service.WaterVideoService;
import com.ccds.water.water.vo.WaterVideoVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 水源出水视频登记。
 *
 * <p>服务端没有媒体库，也无法执行外部进程，因此时长校验与三帧抽取由客户端完成后上报。
 * 服务端只按上报时长做 10 秒闸门，并把判定结论原样记录（标记为客户端上报）。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaterVideoServiceImpl implements WaterVideoService {

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_WATER_MISSING = "水源档案不存在";

    private static final String MSG_VIDEO_INVALID = "出水视频信息不合法";

    private final AccountMapper accountMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final WaterVideoMapper waterVideoMapper;

    private final WaterAccessService waterAccessService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterVideoVO submit(AuthPrincipal principal, Long stationId, Long waterId,
                               WaterVideoCommand command) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireVisibleStation(account, stationId);
        requireWater(stationId, waterId);
        if (command == null || command.getFileId() == null || command.getDurationMs() == null) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, MSG_VIDEO_INVALID);
        }
        int durationMs = command.getDurationMs().intValue();
        if (durationMs <= 0) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, MSG_VIDEO_INVALID);
        }
        if (durationMs > WaterRuleConstant.VIDEO_MAX_DURATION_MS) {
            // 超 10 秒拒收，原视频仍在（本方法不触碰已上传文件）
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID,
                    WaterRuleConstant.MSG_VIDEO_TOO_LONG + (durationMs / 1000) + " 秒");
        }
        String status = normalizeStatus(command.getCheckStatus());
        String reason = trimToNull(command.getCheckReason());
        if (WaterRuleConstant.VIDEO_REJECTED.equals(status) && reason == null) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID,
                    WaterRuleConstant.MSG_VIDEO_REASON_REQUIRED);
        }
        WaterVideoDO video = WaterVideoDO.builder()
                .stationId(stationId)
                .waterId(waterId)
                .fileId(command.getFileId())
                .durationMs(Integer.valueOf(durationMs))
                .frameFileIds(trimToNull(command.getFrameFileIds()))
                .checkStatus(status)
                .checkReason(reason)
                // 时长与帧来自客户端，落库标记，避免被当作服务端确证
                .clientReported(Integer.valueOf(1))
                .createdBy(account.getId())
                .build();
        waterVideoMapper.insert(video);
        log.info("water video submitted stationId={} waterId={} videoId={} status={}",
                stationId, waterId, video.getId(), status);
        return toVo(video);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WaterVideoVO> list(AuthPrincipal principal, Long stationId, Long waterId) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireVisibleStation(account, stationId);
        requireWater(stationId, waterId);
        List<WaterVideoVO> out = new ArrayList<WaterVideoVO>();
        for (WaterVideoDO video : waterVideoMapper.selectByWaterId(waterId)) {
            out.add(toVo(video));
        }
        return out;
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return WaterRuleConstant.VIDEO_FAILED;
        }
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (WaterRuleConstant.VIDEO_PASSED.equals(value)) {
            return WaterRuleConstant.VIDEO_PASSED;
        }
        if (WaterRuleConstant.VIDEO_REJECTED.equals(value)) {
            return WaterRuleConstant.VIDEO_REJECTED;
        }
        return WaterRuleConstant.VIDEO_FAILED;
    }

    private WaterVideoVO toVo(WaterVideoDO video) {
        return WaterVideoVO.builder()
                .id(video.getId())
                .waterId(video.getWaterId())
                .fileId(video.getFileId())
                .durationMs(video.getDurationMs())
                .frameFileIds(video.getFrameFileIds())
                .checkStatus(video.getCheckStatus())
                .checkReason(video.getCheckReason())
                .clientReported(Boolean.valueOf(video.getClientReported() != null
                        && video.getClientReported().intValue() == 1))
                .gmtCreate(video.getGmtCreate())
                .build();
    }

    private WaterSourceDO requireWater(Long stationId, Long waterId) {
        WaterSourceDO water = waterSourceMapper.selectAliveById(waterId);
        if (water == null || !stationId.equals(water.getStationId())) {
            throw new BizException(ErrorCodeConstant.WATER_NOT_FOUND, MSG_WATER_MISSING);
        }
        return water;
    }

    private AccountDO requireAccount(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        return account;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
