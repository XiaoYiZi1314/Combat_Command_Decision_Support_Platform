package com.ccds.duty.service;

import java.util.List;

import com.ccds.duty.dto.KeyUnitDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 重点单位预案。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface KeyUnitService {

    /**
     * 详情，含预案与平面图短时 URL。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param id        重点单位主键
     * @return 重点单位
     */
    KeyUnitDTO getById(AuthPrincipal principal, Long stationId, Long id);

    /**
     * 本站列表。不附带文件，避免 N+1。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param category  类别，可空
     * @param keyword   名称前缀，可空
     * @return 列表，不会为 null
     */
    List<KeyUnitDTO> list(AuthPrincipal principal, Long stationId, String category, String keyword);

    /**
     * 创建。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param dto       入参
     * @return 创建结果
     */
    KeyUnitDTO create(AuthPrincipal principal, Long stationId, KeyUnitDTO dto);

    /**
     * 更新。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param id        重点单位主键
     * @param dto       入参
     * @return 更新结果
     */
    KeyUnitDTO update(AuthPrincipal principal, Long stationId, Long id, KeyUnitDTO dto);

    /**
     * 软删除，并删除关联文件。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param id        重点单位主键
     */
    void delete(AuthPrincipal principal, Long stationId, Long id);
}
