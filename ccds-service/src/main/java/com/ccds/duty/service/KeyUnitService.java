package com.ccds.duty.service;

import com.ccds.duty.dto.KeyUnitDTO;

import java.util.List;

/**
 * 重点单位预案服务
 *
 * @author system
 * @since 2024
 */
public interface KeyUnitService {

    /**
     * 根据ID查询重点单位
     *
     * @param id 重点单位ID
     * @return 重点单位
     */
    KeyUnitDTO getById(Long id);

    /**
     * 根据消防站查询重点单位列表
     *
     * @param stationId 消防站ID
     * @return 重点单位列表
     */
    List<KeyUnitDTO> listByStation(Long stationId);

    /**
     * 根据类别查询重点单位列表
     *
     * @param stationId 消防站ID
     * @param category  类别
     * @return 重点单位列表
     */
    List<KeyUnitDTO> listByCategory(Long stationId, String category);

    /**
     * 根据关键词搜索重点单位
     *
     * @param stationId 消防站ID
     * @param keyword   关键词
     * @return 重点单位列表
     */
    List<KeyUnitDTO> search(Long stationId, String keyword);

    /**
     * 创建重点单位
     *
     * @param dto 重点单位
     * @return 创建后的重点单位
     */
    KeyUnitDTO create(KeyUnitDTO dto);

    /**
     * 更新重点单位
     *
     * @param id  重点单位ID
     * @param dto 重点单位
     * @return 更新后的重点单位
     */
    KeyUnitDTO update(Long id, KeyUnitDTO dto);

    /**
     * 删除重点单位（软删除）
     *
     * @param id 重点单位ID
     */
    void delete(Long id);
}
