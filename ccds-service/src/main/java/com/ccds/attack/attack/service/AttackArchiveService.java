package com.ccds.attack.attack.service;

import java.util.List;

import com.ccds.attack.attack.dto.AttackArchiveCommand;
import com.ccds.attack.attack.vo.AttackArchiveVO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 内攻历史归档服务。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AttackArchiveService {

    /**
     * 站级一键归档：要求全部人员已撤出，归档后清空本站人员行。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param command   事件信息
     * @return 归档记录
     */
    AttackArchiveVO archive(AuthPrincipal principal, Long stationId, AttackArchiveCommand command);

    /**
     * 本站历史列表。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param eventKind 类型过滤，可空
     * @return 列表，不会为 null
     */
    List<AttackArchiveVO> list(AuthPrincipal principal, Long stationId, String eventKind);

    /**
     * 指挥端全部可见站历史；stationId 非空时仅返回该站，为空时返回全部可见站。
     *
     * @param principal 当前身份
     * @param stationId 过滤站主键，可空
     * @param eventKind 类型过滤，可空
     * @return 列表，不会为 null
     */
    List<AttackArchiveVO> listAll(AuthPrincipal principal, Long stationId, String eventKind);

    /**
     * 更新归档基础信息（事件名称/地点）。站级本站、支队、开发可改。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param archiveId 归档主键
     * @param command   事件信息
     * @return 更新后的归档
     */
    AttackArchiveVO updateInfo(AuthPrincipal principal, Long stationId, Long archiveId,
                                AttackArchiveCommand command);

    /**
     * 删除归档。站级本站、支队、开发可删。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param archiveId 归档主键
     */
    void delete(AuthPrincipal principal, Long stationId, Long archiveId);

    /**
     * 导出明细 Excel。
     *
     * @param principal 当前身份
     * @param stationId 站主键，可空（指挥端全部）
     * @param eventKind 类型过滤，可空
     * @return xlsx 字节
     */
    byte[] export(AuthPrincipal principal, Long stationId, String eventKind);

    /**
     * 导出统计 Excel（按站/按日期汇总）。
     *
     * @param principal 当前身份
     * @param stationId 站主键，可空（指挥端全部）
     * @param eventKind 类型过滤，可空
     * @return xlsx 字节
     */
    byte[] exportStats(AuthPrincipal principal, Long stationId, String eventKind);
}
