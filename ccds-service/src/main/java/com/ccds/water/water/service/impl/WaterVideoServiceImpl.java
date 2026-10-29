package com.ccds.water.water.service.impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.constant.FileBizTypeConstant;
import com.ccds.duty.entity.FileObjectDO;
import com.ccds.duty.mapper.FileObjectMapper;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.cloud.cos.COSService;
import com.ccds.infra.cloud.cos.Mp4DurationProbe;
import com.ccds.infra.model.ModelGateway;
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

    private static final String MSG_VIDEO_FILE_INVALID = "文件未上传成功或不属于当前水源";

    private static final String MSG_VIDEO_FRAMES_INVALID = "请提供视频开头、中间、结尾三帧";

    private static final int FRAME_COUNT = 3;

    private final AccountMapper accountMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final WaterVideoMapper waterVideoMapper;

    private final WaterAccessService waterAccessService;

    private final FileObjectMapper fileObjectMapper;

    private final COSService cosService;

    private final ModelGateway modelGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    public WaterVideoVO submit(AuthPrincipal principal, Long stationId, Long waterId,
                               WaterVideoCommand command) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireWritableStation(account, stationId);
        requireWater(stationId, waterId);
        if (command == null || command.getFileId() == null || command.getDurationMs() == null) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, MSG_VIDEO_INVALID);
        }
        FileObjectDO videoFile = requireUploadedFile(command.getFileId(), waterId,
                account.getId(), FileBizTypeConstant.WATER_VIDEO);
        byte[] videoBytes = cosService.getObjectContent(videoFile.getCosKey());
        long verifiedDuration = Mp4DurationProbe.durationMillis(videoBytes);
        if (verifiedDuration <= 0L) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, "无法核验 MP4 视频实际时长");
        }
        int durationMs = (int) Math.min(verifiedDuration, Integer.MAX_VALUE);
        if (durationMs > WaterRuleConstant.VIDEO_MAX_DURATION_MS) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID,
                    WaterRuleConstant.MSG_VIDEO_TOO_LONG + (durationMs / 1000) + " 秒");
        }
        // 客户端可辅助采帧，但不能自行声明识别通过。
        String[] frameIds = command.getFrameFileIds() == null
                ? new String[0] : command.getFrameFileIds().split(",", -1);
        if (frameIds.length != FRAME_COUNT) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, MSG_VIDEO_FRAMES_INVALID);
        }
        List<FileObjectDO> frames = new ArrayList<FileObjectDO>(FRAME_COUNT);
        List<Long> uniqueIds = new ArrayList<Long>(FRAME_COUNT);
        for (String text : frameIds) {
            Long id = parseFileId(text);
            if (uniqueIds.contains(id)) {
                throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, MSG_VIDEO_FRAMES_INVALID);
            }
            uniqueIds.add(id);
            frames.add(requireUploadedFile(id, waterId, account.getId(), FileBizTypeConstant.WATER_VIDEO_FRAME));
        }
        ReviewResult review = reviewFrames(frames);
        WaterVideoDO video = WaterVideoDO.builder()
                .stationId(stationId)
                .waterId(waterId)
                .fileId(command.getFileId())
                .durationMs(Integer.valueOf(durationMs))
                .frameFileIds(command.getFrameFileIds())
                .checkStatus(review.status)
                .checkReason(review.reason)
                // 画面来自客户端抽取；媒体实际时长已在服务端重新核验。
                .clientReported(Integer.valueOf(1))
                .createdBy(account.getId())
                .build();
        waterVideoMapper.insert(video);
        log.info("water video submitted stationId={} waterId={} videoId={} status={}",
                stationId, waterId, video.getId(), review.status);
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

    private FileObjectDO requireUploadedFile(Long id, Long waterId, Long creatorId, String bizType) {
        FileObjectDO file = id == null ? null : fileObjectMapper.selectById(id);
        if (file == null || !bizType.equals(file.getBizType())
                || !waterId.equals(file.getBizId()) || !creatorId.equals(file.getCreatedBy())
                || file.getSizeBytes() == null
                || !file.getSizeBytes().equals(cosService.getObjectSize(file.getCosKey()))) {
            throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, MSG_VIDEO_FILE_INVALID);
        }
        return file;
    }

    private Long parseFileId(String raw) {
        try {
            long parsed = Long.parseLong(raw.trim());
            if (parsed > 0L) {
                return Long.valueOf(parsed);
            }
        } catch (NumberFormatException ex) {
            log.warn("invalid water video frame id");
        }
        throw new BizException(ErrorCodeConstant.WATER_VIDEO_INVALID, MSG_VIDEO_FRAMES_INVALID);
    }

    private ReviewResult reviewFrames(List<FileObjectDO> frames) {
        if (!modelGateway.available()) {
            return new ReviewResult(WaterRuleConstant.VIDEO_FAILED, "视觉识别服务未配置");
        }
        boolean uncertain = false;
        for (int i = 0; i < frames.size(); i++) {
            FileObjectDO frame = frames.get(i);
            byte[] bytes = cosService.getObjectContent(frame.getCosKey());
            if (bytes == null || bytes.length == 0) {
                return new ReviewResult(WaterRuleConstant.VIDEO_FAILED, "无法读取视频关键帧");
            }
            try {
                String reply = modelGateway.completeVision(
                        "你是消防水源视频辅助判读器，只判断当前画面是否清楚可见水源出水口向外持续出水。"
                                + "仅回复 PASS（清楚可见）、REJECT（明确无水流）或 UNKNOWN（无法确认）。"
                                + "不得根据图外信息猜测。",
                        "判断这张出水视频关键帧。请只输出 PASS、REJECT 或 UNKNOWN。",
                        Base64.getEncoder().encodeToString(bytes), frame.getContentType());
                String answer = reply.trim().toUpperCase(Locale.ROOT);
                if ("REJECT".equals(answer)) {
                    return new ReviewResult(WaterRuleConstant.VIDEO_REJECTED,
                            "第" + (i + 1) + "帧未见明显出水（AI 辅助判断）");
                }
                if (!"PASS".equals(answer)) {
                    uncertain = true;
                }
            } catch (IllegalStateException ex) {
                log.warn("water video vision unavailable frameIndex={}", i, ex);
                return new ReviewResult(WaterRuleConstant.VIDEO_FAILED, "视觉识别服务暂不可用");
            }
        }
        return uncertain
                ? new ReviewResult(WaterRuleConstant.VIDEO_FAILED, "关键帧中存在无法确认的画面")
                : new ReviewResult(WaterRuleConstant.VIDEO_PASSED, null);
    }

    private static final class ReviewResult {
        private final String status;
        private final String reason;

        private ReviewResult(String status, String reason) {
            this.status = status;
            this.reason = reason;
        }
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
