package com.ccds.roster.roster.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.dto.ProfileTransferCommand;
import com.ccds.roster.roster.entity.ProfileDO;
import com.ccds.roster.roster.entity.ProfileTombstoneDO;
import com.ccds.roster.roster.entity.ProfileTransferDO;
import com.ccds.roster.roster.mapper.ProfileMapper;
import com.ccds.roster.roster.mapper.ProfileTombstoneMapper;
import com.ccds.roster.roster.mapper.ProfileTransferMapper;
import com.ccds.roster.roster.service.ProfileTransferService;
import com.ccds.roster.roster.service.RosterAccessService;
import com.ccds.roster.roster.vo.ProfileTransferVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 人员跨站调动实现。
 *
 * <p>落档三条硬约束（改法明细 4.1）：</p>
 * <ol>
 *   <li>调入站一律新建档案号，绝不把调出站的档案号原样写进调入站；</li>
 *   <li>只有调入站已有同名卡才并进那张卡；同号但不同名禁止合并、禁止覆盖；</li>
 *   <li>调出站按原档案号加人名删除并写删除记录，同步时不得回到调出站。</li>
 * </ol>
 *
 * @author ccds
 * @since 0.1.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileTransferServiceImpl implements ProfileTransferService {

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_TRANSFER_MISSING = "调动申请不存在";

    private static final String MSG_TRANSFER_PROCESSED = "该调动申请已处理";

    private static final String MSG_PROFILE_MISSING = "人员档案不存在";

    private static final String MSG_SAME_STATION = "调出站与调入站不能相同";

    private static final String MSG_NAME_CONFLICT = "调入站已有不同姓名的同号人员，禁止合并";

    private static final String MSG_TRANSFER_FORBIDDEN = "仅本站或支队账号可提交调动";

    private static final String STATUS_PENDING = "pending";

    private static final String STATUS_APPROVED = "approved";

    private static final String STATUS_REJECTED = "rejected";

    private final AccountMapper accountMapper;

    private final ProfileMapper profileMapper;

    private final ProfileTombstoneMapper profileTombstoneMapper;

    private final ProfileTransferMapper profileTransferMapper;

    private final RosterAccessService rosterAccessService;

    private final StationMapper stationMapper;

    private final OrgQueryService orgQueryService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileTransferVO submit(AuthPrincipal principal, ProfileTransferCommand command) {
        AccountDO account = requireAccount(principal);
        if (command.getFromStationId().equals(command.getToStationId())) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_SAME_STATION);
        }
        rosterAccessService.requireVisibleStation(account, command.getFromStationId());
        if (!rosterAccessService.canSubmitTransfer(account, command.getFromStationId())) {
            throw new BizException(ErrorCodeConstant.ROSTER_WRITE_FORBIDDEN, MSG_TRANSFER_FORBIDDEN);
        }
        rosterAccessService.requireVisibleStation(account, command.getToStationId());
        ProfileDO profile = profileMapper.selectAliveById(command.getProfileId());
        if (profile == null || !command.getFromStationId().equals(profile.getStationId())) {
            throw new BizException(ErrorCodeConstant.ROSTER_PROFILE_NOT_FOUND, MSG_PROFILE_MISSING);
        }
        if (command.getClientRequestId() != null && !command.getClientRequestId().isBlank()) {
            ProfileTransferDO exist = profileTransferMapper.selectByClientRequestId(
                    command.getFromStationId(), command.getClientRequestId().trim());
            if (exist != null) {
                return toVo(exist);
            }
        }
        ProfileTransferDO transfer = ProfileTransferDO.builder()
                .fromStationId(command.getFromStationId())
                .toStationId(command.getToStationId())
                .profileId(command.getProfileId())
                .sourceArchiveNo(profile.getArchiveNo())
                .targetProfileId(command.getTargetProfileId())
                .reason(command.getReason().trim())
                .status(STATUS_PENDING)
                .createdBy(account.getId())
                .clientRequestId(trimToNull(command.getClientRequestId()))
                .build();
        profileTransferMapper.insert(transfer);
        log.info("profile transfer submitted transferId={} from={} to={}",
                transfer.getId(), command.getFromStationId(), command.getToStationId());
        return toVo(transfer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProfileTransferVO> list(AuthPrincipal principal, Long stationId, String status) {
        AccountDO account = requireAccount(principal);
        List<Long> stationIds = new ArrayList<Long>();
        if (stationId != null) {
            rosterAccessService.requireVisibleStation(account, stationId);
            stationIds.add(stationId);
        } else {
            for (StationDO station : orgQueryService.listVisibleStationEntities(account)) {
                stationIds.add(station.getId());
            }
        }
        if (stationIds.isEmpty()) {
            return new ArrayList<ProfileTransferVO>();
        }
        List<ProfileTransferVO> out = new ArrayList<ProfileTransferVO>();
        for (ProfileTransferDO transfer : profileTransferMapper.selectByStations(
                stationIds, trimToNull(status))) {
            out.add(toVo(transfer));
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileTransferVO review(AuthPrincipal principal, Long transferId, Boolean approve,
                                    String reviewReason) {
        AccountDO account = requireAccount(principal);
        rosterAccessService.requireReviewer(account);
        ProfileTransferDO transfer = profileTransferMapper.selectById(transferId);
        if (transfer == null) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_TRANSFER_MISSING);
        }
        if (!STATUS_PENDING.equals(transfer.getStatus())) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_TRANSFER_PROCESSED);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean pass = Boolean.TRUE.equals(approve);
        int affected = profileTransferMapper.reviewCas(transferId, STATUS_PENDING,
                pass ? STATUS_APPROVED : STATUS_REJECTED, account.getId(), account.getUsername(),
                trimToNull(reviewReason), now);
        if (affected == 0) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_TRANSFER_PROCESSED);
        }
        if (pass) {
            applyApproved(transfer, account.getId(), now);
        }
        transfer.setStatus(pass ? STATUS_APPROVED : STATUS_REJECTED);
        transfer.setReviewerName(account.getUsername());
        transfer.setReviewedAt(now);
        log.info("profile transfer reviewed transferId={} approve={}",
                transferId, Boolean.valueOf(pass));
        return toVo(transfer);
    }

    /**
     * 审批通过落档：调入站新建行并分配新档案号；调出站软删并写删除记录。
     *
     * @param transfer   申请
     * @param operatorId 审批人账号主键
     * @param now        当前时间
     */
    private void applyApproved(ProfileTransferDO transfer, Long operatorId, LocalDateTime now) {
        ProfileDO source = profileMapper.selectAliveById(transfer.getProfileId());
        if (source == null) {
            throw new BizException(ErrorCodeConstant.ROSTER_PROFILE_NOT_FOUND, MSG_PROFILE_MISSING);
        }
        Long toStationId = transfer.getToStationId();
        ProfileDO sameName = profileMapper.selectAliveByStationAndName(toStationId, source.getName());
        Long targetProfileId = null;
        if (sameName != null) {
            // 只有同名才允许并卡；同号不同名禁止合并
            if (transfer.getTargetProfileId() != null
                    && !transfer.getTargetProfileId().equals(sameName.getId())) {
                throw new BizException(ErrorCodeConstant.ROSTER_NAME_DUPLICATE, MSG_NAME_CONFLICT);
            }
            targetProfileId = sameName.getId();
            mergeIntoExisting(sameName, source, now);
        } else {
            ProfileDO created = new ProfileDO();
            created.setStationId(toStationId);
            created.setName(source.getName());
            created.setTitle(source.getTitle());
            created.setRankName(source.getRankName());
            // 电话密文直接复制，不解密、不落明文
            created.setPhoneCipher(source.getPhoneCipher());
            created.setCylType(source.getCylType());
            created.setHeightCm(source.getHeightCm());
            created.setWeightKg(source.getWeightKg());
            created.setAge(source.getAge());
            created.setMorningPressure(source.getMorningPressure());
            created.setGmtCreate(now);
            created.setGmtModified(now);
            profileMapper.insert(created);
            // 调入站一律新建档案号
            String newArchiveNo = RosterRuleConstant.ARCHIVE_NO_PREFIX
                    + String.format("%0" + RosterRuleConstant.ARCHIVE_NO_PAD + "d", created.getId());
            created.setArchiveNo(newArchiveNo);
            profileMapper.update(created);
            targetProfileId = created.getId();
            transfer.setNewArchiveNo(newArchiveNo);
        }
        profileTransferMapper.updateApplied(transfer.getId(), transfer.getNewArchiveNo(), targetProfileId);
        // 调出站按原档案号加人名删除，并写删除记录
        profileTombstoneMapper.insertIgnore(ProfileTombstoneDO.builder()
                .stationId(source.getStationId())
                .profileId(source.getId())
                .archiveNo(source.getArchiveNo())
                .legacyProfileId(source.getArchiveNo())
                .name(source.getName())
                .nameNorm(normalizeName(source.getName()))
                .nfcTag(source.getNfcTag())
                .nfcNorm(normalizeNfc(source.getNfcTag()))
                .source(RosterRuleConstant.TOMBSTONE_SOURCE_TRANSFER)
                .createdBy(operatorId)
                .build());
        profileMapper.softDelete(source.getId(), now, now);
    }

    private void mergeIntoExisting(ProfileDO target, ProfileDO source, LocalDateTime now) {
        // 只填空字段，保留目标原档案号与已有内容
        if (target.getTitle() == null && source.getTitle() != null) {
            target.setTitle(source.getTitle());
        }
        if (target.getRankName() == null && source.getRankName() != null) {
            target.setRankName(source.getRankName());
        }
        if (target.getPhoneCipher() == null && source.getPhoneCipher() != null) {
            target.setPhoneCipher(source.getPhoneCipher());
        }
        if (target.getHeightCm() == null && source.getHeightCm() != null) {
            target.setHeightCm(source.getHeightCm());
        }
        if (target.getWeightKg() == null && source.getWeightKg() != null) {
            target.setWeightKg(source.getWeightKg());
        }
        if (target.getAge() == null && source.getAge() != null) {
            target.setAge(source.getAge());
        }
        target.setGmtModified(now);
        profileMapper.update(target);
    }

    private ProfileTransferVO toVo(ProfileTransferDO transfer) {
        Map<Long, String> names = stationNameMap();
        String profileName = null;
        ProfileDO source = profileMapper.selectAliveById(transfer.getProfileId());
        if (source != null) {
            profileName = source.getName();
        }
        return ProfileTransferVO.builder()
                .id(transfer.getId())
                .fromStationId(transfer.getFromStationId())
                .fromStationName(names.get(transfer.getFromStationId()))
                .toStationId(transfer.getToStationId())
                .toStationName(names.get(transfer.getToStationId()))
                .profileName(profileName)
                .sourceArchiveNo(transfer.getSourceArchiveNo())
                .newArchiveNo(transfer.getNewArchiveNo())
                .targetProfileId(transfer.getTargetProfileId())
                .reason(transfer.getReason())
                .status(transfer.getStatus())
                .reviewReason(transfer.getReviewReason())
                .reviewerName(transfer.getReviewerName())
                .reviewedAt(transfer.getReviewedAt())
                .gmtCreate(transfer.getGmtCreate())
                .build();
    }

    private Map<Long, String> stationNameMap() {
        Map<Long, String> names = new LinkedHashMap<Long, String>();
        for (StationDO station : stationMapper.selectAll()) {
            names.put(station.getId(), station.getName());
        }
        return names;
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

    private String normalizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replaceAll("[\\s\\n\\r　]", "");
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeNfc(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().replaceAll("[\\s:：\\-_]", "").toUpperCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }
}
