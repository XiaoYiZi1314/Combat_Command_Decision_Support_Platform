package com.ccds.duty.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.constant.FileBizTypeConstant;
import com.ccds.duty.constant.KeyUnitCategoryConstant;
import com.ccds.duty.dto.KeyUnitDTO;
import com.ccds.duty.entity.KeyUnitDO;
import com.ccds.duty.mapper.KeyUnitMapper;
import com.ccds.duty.service.DutyAccessService;
import com.ccds.duty.service.FileObjectService;
import com.ccds.duty.service.KeyUnitService;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.crypto.FieldCipherUtil;
import com.ccds.infra.crypto.SensitiveMaskUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 重点单位预案读写。电话密文入库。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyUnitServiceImpl implements KeyUnitService {

    private static final String CIPHER_PREFIX = "v1:";

    private static final String MSG_NOT_FOUND = "重点单位不存在";

    private static final String MSG_CATEGORY_INVALID = "重点单位类别不合法";

    private final KeyUnitMapper keyUnitMapper;

    private final FileObjectService fileObjectService;

    private final DutyAccessService dutyAccessService;

    private final FieldCipherUtil fieldCipherUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyUnitDTO getById(AuthPrincipal principal, Long stationId, Long id) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireVisibleStation(account, stationId);
        boolean writable = dutyAccessService.isWritable(account, stationId);
        KeyUnitDO keyUnit = requireInStation(stationId, id);
        return convertToDTO(keyUnit, writable, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<KeyUnitDTO> list(AuthPrincipal principal, Long stationId, String category, String keyword) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireVisibleStation(account, stationId);
        boolean writable = dutyAccessService.isWritable(account, stationId);
        List<KeyUnitDO> keyUnits = loadList(stationId, category, keyword);
        return keyUnits.stream()
                .map(item -> convertToDTO(item, writable, false))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeyUnitDTO create(AuthPrincipal principal, Long stationId, KeyUnitDTO dto) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireWritableStation(account, stationId);
        validateCategory(dto.getCategory());
        KeyUnitDO keyUnit = KeyUnitDO.builder()
                .stationId(stationId)
                .name(dto.getName())
                .address(dto.getAddress())
                .category(dto.getCategory())
                .contact(dto.getContact())
                .phoneCipher(encryptPhone(dto.getPhone()))
                .notes(dto.getNotes())
                .planText(dto.getPlanText())
                .build();
        keyUnitMapper.insert(keyUnit);
        log.info("创建重点单位：stationId={}, id={}", stationId, keyUnit.getId());
        return convertToDTO(keyUnit, true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeyUnitDTO update(AuthPrincipal principal, Long stationId, Long id, KeyUnitDTO dto) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireWritableStation(account, stationId);
        requireInStation(stationId, id);
        validateCategory(dto.getCategory());
        KeyUnitDO keyUnit = KeyUnitDO.builder()
                .id(id)
                .stationId(stationId)
                .name(dto.getName())
                .address(dto.getAddress())
                .category(dto.getCategory())
                .contact(dto.getContact())
                .phoneCipher(encryptPhone(dto.getPhone()))
                .notes(dto.getNotes())
                .planText(dto.getPlanText())
                .build();
        keyUnitMapper.updateById(keyUnit);
        log.info("更新重点单位：id={}", id);
        return convertToDTO(requireInStation(stationId, id), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(AuthPrincipal principal, Long stationId, Long id) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireWritableStation(account, stationId);
        requireInStation(stationId, id);
        fileObjectService.deleteByBiz(FileBizTypeConstant.KEYUNIT_PLAN, id);
        fileObjectService.deleteByBiz(FileBizTypeConstant.KEYUNIT_FLOOR, id);
        KeyUnitDO patch = new KeyUnitDO();
        patch.setId(id);
        patch.setDeletedAt(LocalDateTime.now());
        keyUnitMapper.softDeleteById(patch);
        log.info("删除重点单位：id={}", id);
    }

    private List<KeyUnitDO> loadList(Long stationId, String category, String keyword) {
        String normalizedCategory = normalizeFilter(category);
        String normalizedKeyword = normalizeFilter(keyword);
        if (normalizedCategory == null && normalizedKeyword == null) {
            return keyUnitMapper.selectByStationId(stationId);
        }
        String escapedKeyword = normalizedKeyword == null ? null : escapeLikePrefix(normalizedKeyword);
        return keyUnitMapper.selectByFilters(stationId, normalizedCategory, escapedKeyword);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private KeyUnitDO requireInStation(Long stationId, Long id) {
        KeyUnitDO keyUnit = keyUnitMapper.selectById(id);
        if (keyUnit == null || keyUnit.getDeletedAt() != null || !stationId.equals(keyUnit.getStationId())) {
            throw new BizException(ErrorCodeConstant.KEY_UNIT_NOT_FOUND, MSG_NOT_FOUND);
        }
        return keyUnit;
    }

    private void validateCategory(String category) {
        if (!KeyUnitCategoryConstant.isAllowed(category)) {
            throw new BizException(ErrorCodeConstant.KEY_UNIT_CATEGORY_INVALID, MSG_CATEGORY_INVALID);
        }
    }

    private KeyUnitDTO convertToDTO(KeyUnitDO keyUnit, boolean exposePhone, boolean withFiles) {
        String phone = decryptPhone(keyUnit.getPhoneCipher());
        KeyUnitDTO dto = KeyUnitDTO.builder()
                .id(keyUnit.getId())
                .stationId(keyUnit.getStationId())
                .name(keyUnit.getName())
                .address(keyUnit.getAddress())
                .category(keyUnit.getCategory())
                .contact(keyUnit.getContact())
                .phone(exposePhone ? phone : null)
                .phoneMasked(SensitiveMaskUtil.maskPhone(phone))
                .notes(keyUnit.getNotes())
                .planText(keyUnit.getPlanText())
                .build();
        if (withFiles) {
            dto.setPlanFiles(fileObjectService.listByBizWithPreview(
                    FileBizTypeConstant.KEYUNIT_PLAN, keyUnit.getId()));
            dto.setFloorPlans(fileObjectService.listByBizWithPreview(
                    FileBizTypeConstant.KEYUNIT_FLOOR, keyUnit.getId()));
        }
        return dto;
    }

    private String encryptPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return fieldCipherUtil.encrypt(phone.trim());
    }

    private String decryptPhone(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (stored.startsWith(CIPHER_PREFIX)) {
            return fieldCipherUtil.decrypt(stored);
        }
        return stored;
    }

    private String escapeLikePrefix(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
