package com.ccds.duty.controller;

import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.duty.dto.KeyUnitDTO;
import com.ccds.duty.service.KeyUnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 重点单位预案Controller
 *
 * @author system
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stations/{stationId}/key-units")
@RequiredArgsConstructor
public class KeyUnitController {

    private final KeyUnitService keyUnitService;

    /**
     * 查询消防站的重点单位列表
     *
     * @param stationId 消防站ID
     * @param category  类别（可选）
     * @param keyword   关键词（可选）
     * @return 重点单位列表
     */
    @GetMapping
    public ApiResultVO<List<KeyUnitDTO>> list(
            @PathVariable Long stationId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        List<KeyUnitDTO> keyUnits;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            keyUnits = keyUnitService.search(stationId, keyword.trim());
        } else if (category != null && !category.trim().isEmpty()) {
            keyUnits = keyUnitService.listByCategory(stationId, category);
        } else {
            keyUnits = keyUnitService.listByStation(stationId);
        }
        
        return ApiResultVO.ok(keyUnits);
    }

    /**
     * 根据ID查询重点单位
     *
     * @param stationId 消防站ID
     * @param id        重点单位ID
     * @return 重点单位
     */
    @GetMapping("/{id}")
    public ApiResultVO<KeyUnitDTO> getById(
            @PathVariable Long stationId,
            @PathVariable Long id) {
        KeyUnitDTO keyUnit = keyUnitService.getById(id);
        return ApiResultVO.ok(keyUnit);
    }

    /**
     * 创建重点单位
     *
     * @param stationId 消防站ID
     * @param dto       重点单位
     * @return 创建后的重点单位
     */
    @PostMapping
    public ApiResultVO<KeyUnitDTO> create(
            @PathVariable Long stationId,
            @Valid @RequestBody KeyUnitDTO dto) {
        dto.setStationId(stationId);
        KeyUnitDTO created = keyUnitService.create(dto);
        return ApiResultVO.ok(created);
    }

    /**
     * 更新重点单位
     *
     * @param stationId 消防站ID
     * @param id        重点单位ID
     * @param dto       重点单位
     * @return 更新后的重点单位
     */
    @PutMapping("/{id}")
    public ApiResultVO<KeyUnitDTO> update(
            @PathVariable Long stationId,
            @PathVariable Long id,
            @Valid @RequestBody KeyUnitDTO dto) {
        dto.setStationId(stationId);
        KeyUnitDTO updated = keyUnitService.update(id, dto);
        return ApiResultVO.ok(updated);
    }

    /**
     * 删除重点单位
     *
     * @param stationId 消防站ID
     * @param id        重点单位ID
     * @return 成功标识
     */
    @DeleteMapping("/{id}")
    public ApiResultVO<Void> delete(
            @PathVariable Long stationId,
            @PathVariable Long id) {
        keyUnitService.delete(id);
        return ApiResultVO.ok(null);
    }
}
