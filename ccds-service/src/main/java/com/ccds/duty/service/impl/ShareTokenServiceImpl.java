package com.ccds.duty.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.attack.attack.entity.AttackPersonDO;
import com.ccds.attack.attack.mapper.AttackPersonMapper;
import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.dto.CreateShareTokenRequestDTO;
import com.ccds.duty.dto.CreateShareTokenResponseDTO;
import com.ccds.duty.entity.ShareTokenDO;
import com.ccds.duty.mapper.ShareTokenMapper;
import com.ccds.duty.service.DutyAccessService;
import com.ccds.duty.service.ShareTokenService;
import com.ccds.duty.vo.ShareAttackVO;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 共享令牌：哈希入库，匿名只读投影。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareTokenServiceImpl implements ShareTokenService {

    private static final int DEFAULT_EXPIRE_HOURS = 2;

    private static final String HASH_ALG = "SHA-256";

    private static final String UNGROUPED = "未分组";

    private final ShareTokenMapper shareTokenMapper;

    private final AttackPersonMapper attackPersonMapper;

    private final StationMapper stationMapper;

    private final DutyAccessService dutyAccessService;

    @Value("${ccds.share.base-url:http://localhost:8080}")
    private String shareBaseUrl;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateShareTokenResponseDTO createShareToken(AuthPrincipal principal, CreateShareTokenRequestDTO request) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireWritableStation(account, request.getStationId());
        String token = UUID.randomUUID().toString().replace("-", "");
        int expireHours = request.getExpireHours() != null ? request.getExpireHours() : DEFAULT_EXPIRE_HOURS;
        LocalDateTime expireAt = LocalDateTime.now().plusHours(expireHours);
        ShareTokenDO shareToken = ShareTokenDO.builder()
                .stationId(request.getStationId())
                .tokenHash(hashToken(token))
                .expireAt(expireAt)
                .createdBy(account.getId())
                .build();
        shareTokenMapper.insert(shareToken);
        log.info("创建共享令牌：stationId={}, tokenId={}, expireAt={}",
                request.getStationId(), shareToken.getId(), expireAt);
        return CreateShareTokenResponseDTO.builder()
                .tokenId(shareToken.getId())
                .token(token)
                .shareUrl(buildShareUrl(token))
                .expireAt(expireAt)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeToken(AuthPrincipal principal, Long tokenId) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        ShareTokenDO token = shareTokenMapper.selectById(tokenId);
        if (token == null) {
            throw new BizException(ErrorCodeConstant.SHARE_TOKEN_NOT_FOUND, "令牌不存在");
        }
        dutyAccessService.requireWritableStation(account, token.getStationId());
        if (token.getRevokedAt() != null) {
            return;
        }
        ShareTokenDO update = new ShareTokenDO();
        update.setId(tokenId);
        update.setRevokedAt(LocalDateTime.now());
        shareTokenMapper.updateById(update);
        log.info("作废共享令牌：tokenId={}", tokenId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ShareAttackVO getSharedAttack(String token) {
        ShareTokenDO shareToken = requireValidToken(token);
        StationDO station = stationMapper.selectById(shareToken.getStationId());
        if (station == null) {
            throw new BizException(ErrorCodeConstant.DUTY_STATION_FORBIDDEN, "消防站不存在");
        }
        List<AttackPersonDO> persons = attackPersonMapper.selectByStationId(shareToken.getStationId()).stream()
                .filter(person -> person.getWithdrawnAt() == null)
                .collect(Collectors.toList());
        ShareAttackVO vo = new ShareAttackVO();
        vo.setStationName(station.getName());
        vo.setLastUpdateTime(latestModified(persons));
        vo.setGroups(buildGroups(persons));
        vo.setPersons(buildPersonCards(persons));
        return vo;
    }

    private ShareTokenDO requireValidToken(String token) {
        ShareTokenDO shareToken = shareTokenMapper.selectByTokenHash(hashToken(token));
        if (shareToken == null) {
            throw new BizException(ErrorCodeConstant.SHARE_TOKEN_INVALID, "共享链接无效");
        }
        if (shareToken.getRevokedAt() != null) {
            throw new BizException(ErrorCodeConstant.SHARE_TOKEN_REVOKED, "共享链接已失效");
        }
        if (shareToken.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCodeConstant.SHARE_TOKEN_EXPIRED, "共享链接已过期");
        }
        return shareToken;
    }

    private String buildShareUrl(String token) {
        String base = shareBaseUrl == null ? "" : shareBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + ApiPathConstant.API_V1 + "/s/" + token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALG);
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new BizException(ErrorCodeConstant.SYSTEM_ERROR, "令牌哈希失败");
        }
    }

    private LocalDateTime latestModified(List<AttackPersonDO> persons) {
        return persons.stream()
                .map(AttackPersonDO::getGmtModified)
                .filter(item -> item != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private List<ShareAttackVO.GroupSummary> buildGroups(List<AttackPersonDO> persons) {
        Map<String, List<AttackPersonDO>> groupMap = persons.stream()
                .collect(Collectors.groupingBy(person ->
                        person.getGroupName() != null ? person.getGroupName() : UNGROUPED));
        return groupMap.entrySet().stream()
                .map(entry -> ShareAttackVO.GroupSummary.builder()
                        .groupName(entry.getKey())
                        .count(entry.getValue().size())
                        .worstStatus(worstStatus(entry.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ShareAttackVO.PersonCard> buildPersonCards(List<AttackPersonDO> persons) {
        return persons.stream()
                .map(person -> ShareAttackVO.PersonCard.builder()
                        .name(person.getDisplayName())
                        .groupName(person.getGroupName())
                        .cylType(person.getCylType())
                        .currentPressure(person.getCurrentPressure() != null
                                ? person.getCurrentPressure().doubleValue() : null)
                        .remainSec(person.getRemainSec())
                        .status(person.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    private String worstStatus(List<AttackPersonDO> groupPersons) {
        return groupPersons.stream()
                .map(AttackPersonDO::getStatus)
                .max(Comparator.comparing(this::statusPriority))
                .orElse("in");
    }

    private int statusPriority(String status) {
        if (status == null) {
            return -1;
        }
        switch (status) {
            case "danger":
                return 3;
            case "warn":
                return 2;
            case "in":
                return 1;
            case "out":
                return 0;
            default:
                return -1;
        }
    }
}
