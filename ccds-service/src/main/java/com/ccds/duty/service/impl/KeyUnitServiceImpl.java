package com.ccds.duty.service.impl;

import com.ccds.common.api.exception.BizException;
import com.ccds.duty.dto.FileObjectDTO;
import com.ccds.duty.dto.KeyUnitDTO;
import com.ccds.duty.entity.KeyUnitDO;
import com.ccds.duty.mapper.KeyUnitMapper;
import com.ccds.duty.service.FileObjectService;
import com.ccds.duty.service.KeyUnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 重点单位预案服务实现
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyUnitServiceImpl implements KeyUnitService {

    private final KeyUnitMapper keyUnitMapper;
    private final FileObjectService fileObjectService;

    @Override
    public KeyUnitDTO getById(Long id) {
        KeyUnitDO keyUnit = keyUnitMapper.selectById(id);
        if (keyUnit == null || keyUnit.getDeletedAt() != null) {
            return null;
        }
        return convertToDTO(keyUnit);
    }

    @Override
    public List<KeyUnitDTO> listByStation(Long stationId) {
        List<KeyUnitDO> keyUnits = keyUnitMapper.selectByStationId(stationId);
        return keyUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KeyUnitDTO> listByCategory(Long stationId, String category) {
        List<KeyUnitDO> keyUnits = keyUnitMapper.selectByStationAndCategory(stationId, category);
        return keyUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KeyUnitDTO> search(Long stationId, String keyword) {
        List<KeyUnitDO> keyUnits = keyUnitMapper.searchByKeyword(stationId, keyword);
        return keyUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeyUnitDTO create(KeyUnitDTO dto) {
        KeyUnitDO keyUnit = KeyUnitDO.builder()
                .stationId(dto.getStationId())
                .name(dto.getName())
                .address(dto.getAddress())
                .category(dto.getCategory())
                .contact(dto.getContact())
                .phone(dto.getPhone())
                .notes(dto.getNotes())
                .planText(dto.getPlanText())
                .build();

        keyUnitMapper.insert(keyUnit);
        log.info("创建重点单位：stationId={}, name={}, id={}", dto.getStationId(), dto.getName(), keyUnit.getId());

        return convertToDTO(keyUnit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KeyUnitDTO update(Long id, KeyUnitDTO dto) {
        KeyUnitDO existing = keyUnitMapper.selectById(id);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BizException("KEY_UNIT_NOT_FOUND", "重点单位不存在");
        }

        KeyUnitDO keyUnit = KeyUnitDO.builder()
                .id(id)
                .stationId(dto.getStationId())
                .name(dto.getName())
                .address(dto.getAddress())
                .category(dto.getCategory())
                .contact(dto.getContact())
                .phone(dto.getPhone())
                .notes(dto.getNotes())
                .planText(dto.getPlanText())
                .build();

        keyUnitMapper.updateById(keyUnit);
        log.info("更新重点单位：id={}, name={}", id, dto.getName());

        return convertToDTO(keyUnit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KeyUnitDO existing = keyUnitMapper.selectById(id);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }

        // 软删除
        KeyUnitDO keyUnit = new KeyUnitDO();
        keyUnit.setId(id);
        keyUnit.setDeletedAt(LocalDateTime.now());
        keyUnitMapper.updateById(keyUnit);

        log.info("删除重点单位：id={}", id);
    }

    /**
     * 转换为DTO
     */
    private KeyUnitDTO convertToDTO(KeyUnitDO keyUnit) {
        KeyUnitDTO dto = KeyUnitDTO.builder()
                .id(keyUnit.getId())
                .stationId(keyUnit.getStationId())
                .name(keyUnit.getName())
                .address(keyUnit.getAddress())
                .category(keyUnit.getCategory())
                .contact(keyUnit.getContact())
                .phone(keyUnit.getPhone())
                .notes(keyUnit.getNotes())
                .planText(keyUnit.getPlanText())
                .build();

        // 加载关联文件
        List<FileObjectDTO> planFiles = fileObjectService.listByBiz("keyunit_plan", keyUnit.getId());
        List<FileObjectDTO> floorPlans = fileObjectService.listByBiz("keyunit_floor", keyUnit.getId());
        dto.setPlanFiles(planFiles);
        dto.setFloorPlans(floorPlans);

        return dto;
    }
}
