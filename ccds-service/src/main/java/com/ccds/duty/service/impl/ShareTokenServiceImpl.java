package com.ccds.duty.service.impl;

import com.ccds.attack.attack.entity.AttackPersonDO;
import com.ccds.attack.attack.mapper.AttackPersonMapper;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.dto.CreateShareTokenRequestDTO;
import com.ccds.duty.dto.CreateShareTokenResponseDTO;
import com.ccds.duty.entity.ShareTokenDO;
import com.ccds.duty.mapper.ShareTokenMapper;
import com.ccds.duty.service.ShareTokenService;
import com.ccds.duty.vo.ShareAttackVO;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 共享令牌服务实现
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareTokenServiceImpl implements ShareTokenService {

    private final ShareTokenMapper shareTokenMapper;
    private final AttackPersonMapper attackPersonMapper;
    private final StationMapper stationMapper;

    @Value("${ccds.share.base-url:http://localhost:8080}")
    private String shareBaseUrl;

    private static final int DEFAULT_EXPIRE_HOURS = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateShareTokenResponseDTO createShareToken(CreateShareTokenRequestDTO request, Long accountId) {
        // 生成随机令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(token);

        // 计算过期时间
        int expireHours = request.getExpireHours() != null ? request.getExpireHours() : DEFAULT_EXPIRE_HOURS;
        LocalDateTime expireAt = LocalDateTime.now().plusHours(expireHours);

        // 创建令牌记录
        ShareTokenDO shareToken = ShareTokenDO.builder()
                .stationId(request.getStationId())
                .tokenHash(tokenHash)
                .expireAt(expireAt)
                .createdBy(accountId)
                .build();

        shareTokenMapper.insert(shareToken);

        // 构建完整URL
        String shareUrl = String.format("%s/s/%s", shareBaseUrl, token);

        log.info("创建共享令牌：stationId={}, tokenId={}, expireAt={}", 
                request.getStationId(), shareToken.getId(), expireAt);

        return CreateShareTokenResponseDTO.builder()
                .tokenId(shareToken.getId())
                .token(token)
                .shareUrl(shareUrl)
                .expireAt(expireAt)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeToken(Long tokenId) {
        ShareTokenDO token = shareTokenMapper.selectById(tokenId);
        if (token == null) {
            throw new BizException("SHARE_TOKEN_NOT_FOUND", "令牌不存在");
        }

        if (token.getRevokedAt() != null) {
            return;
        }

        ShareTokenDO update = new ShareTokenDO();
        update.setId(tokenId);
        update.setRevokedAt(LocalDateTime.now());
        shareTokenMapper.updateById(update);

        log.info("作废共享令牌：tokenId={}", tokenId);
    }

    @Override
    public ShareAttackVO getSharedAttack(String token) {
        // 验证令牌
        String tokenHash = hashToken(token);
        ShareTokenDO shareToken = shareTokenMapper.selectByTokenHash(tokenHash);

        if (shareToken == null) {
            throw new BizException("SHARE_TOKEN_INVALID", "共享链接无效");
        }

        if (shareToken.getRevokedAt() != null) {
            throw new BizException("SHARE_TOKEN_REVOKED", "共享链接已失效");
        }

        if (shareToken.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException("SHARE_TOKEN_EXPIRED", "共享链接已过期");
        }

        // 查询消防站信息
        StationDO station = stationMapper.selectById(shareToken.getStationId());
        if (station == null) {
            throw new BizException("STATION_NOT_FOUND", "消防站不存在");
        }

        // 查询内攻人员（仅未撤出）
        List<AttackPersonDO> persons = attackPersonMapper.selectByStationId(shareToken.getStationId()).stream()
                .filter(p -> p.getWithdrawnAt() == null)
                .collect(Collectors.toList());

        // 构建脱敏VO
        ShareAttackVO vo = new ShareAttackVO();
        vo.setStationName(station.getName());
        vo.setLastUpdateTime(persons.isEmpty() ? null : 
                persons.stream().map(AttackPersonDO::getGmtModified).max(LocalDateTime::compareTo).orElse(null));

        // 按编组汇总
        Map<String, List<AttackPersonDO>> groupMap = persons.stream()
                .collect(Collectors.groupingBy(p -> p.getGroupName() != null ? p.getGroupName() : "未分组"));

        List<ShareAttackVO.GroupSummary> groups = groupMap.entrySet().stream()
                .map(entry -> {
                    String groupName = entry.getKey();
                    List<AttackPersonDO> groupPersons = entry.getValue();
                    
                    String worstStatus = groupPersons.stream()
                            .map(AttackPersonDO::getStatus)
                            .max(Comparator.comparing(this::statusPriority))
                            .orElse("in");

                    return ShareAttackVO.GroupSummary.builder()
                            .groupName(groupName)
                            .count(groupPersons.size())
                            .worstStatus(worstStatus)
                            .build();
                })
                .collect(Collectors.toList());

        vo.setGroups(groups);

        // 人员卡片（脱敏：不含电话、NFC号）
        List<ShareAttackVO.PersonCard> personCards = persons.stream()
                .map(p -> ShareAttackVO.PersonCard.builder()
                        .name(p.getDisplayName())
                        .groupName(p.getGroupName())
                        .cylType(p.getCylType())
                        .currentPressure(p.getCurrentPressure() != null ? p.getCurrentPressure().doubleValue() : null)
                        .remainSec(p.getRemainSec())
                        .status(p.getStatus())
                        .build())
                .collect(Collectors.toList());

        vo.setPersons(personCards);

        return vo;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String tokenHash = hashToken(token);
            ShareTokenDO shareToken = shareTokenMapper.selectByTokenHash(tokenHash);

            return shareToken != null 
                    && shareToken.getRevokedAt() == null
                    && shareToken.getExpireAt().isAfter(LocalDateTime.now());
        } catch (Exception e) {
            log.error("验证令牌失败", e);
            return false;
        }
    }

    /**
     * 令牌SHA256哈希
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("令牌哈希失败", e);
        }
    }

    /**
     * 状态优先级（用于取最严重状态）
     */
    private int statusPriority(String status) {
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
