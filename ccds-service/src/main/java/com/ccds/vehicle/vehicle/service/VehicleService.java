package com.ccds.vehicle.vehicle.service;

import java.util.List;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.vehicle.vehicle.dto.VehicleDTO;
import com.ccds.vehicle.vehicle.dto.VehicleRequestDTO;
import com.ccds.vehicle.vehicle.dto.VehicleRequestReviewDTO;

/**
 * 车辆档案服务。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface VehicleService {

    /**
     * 本站列表。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param type      类型码，可空
     * @param status    状态，可空
     * @param keyword   号牌/型号/厂家关键字，可空
     * @return 列表，不会为 null
     */
    List<VehicleDTO> list(AuthPrincipal principal, Long stationId, String type, String status, String keyword);

    /**
     * 指挥端可见的全部车辆，stationId 为空时返回全部可见站。
     *
     * @param principal 当前身份
     * @param stationId 站主键，可空
     * @param type      类型码，可空
     * @param status    状态，可空
     * @param keyword   号牌/型号/厂家关键字，可空
     * @return 列表，不会为 null
     */
    List<VehicleDTO> listAll(AuthPrincipal principal, Long stationId, String type, String status, String keyword);

    /**
     * 单车详情。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param vehicleId 车辆主键
     * @return 车辆
     */
    VehicleDTO getById(AuthPrincipal principal, Long stationId, Long vehicleId);

    /**
     * 支队/开发直接落档：新增或修改。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param dto       车辆内容
     * @return 保存结果
     */
    VehicleDTO save(AuthPrincipal principal, Long stationId, VehicleDTO dto);

    /**
     * 支队/开发直接删除。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param vehicleId 车辆主键
     */
    void delete(AuthPrincipal principal, Long stationId, Long vehicleId);

    /**
     * 站级提交变更申请。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param dto       申请内容
     * @return 申请
     */
    VehicleRequestDTO submitRequest(AuthPrincipal principal, Long stationId, VehicleRequestDTO dto);

    /**
     * 查询变更申请：站级查本站，支队/开发查全部可见站。
     *
     * @param principal 当前身份
     * @param stationId 站主键，路径指定时限定该站，可空
     * @return 列表，不会为 null
     */
    List<VehicleRequestDTO> listRequests(AuthPrincipal principal, Long stationId);

    /**
     * 支队/开发审批。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param requestId 申请主键
     * @param review    审批结果
     * @return 更新后的申请
     */
    VehicleRequestDTO reviewRequest(AuthPrincipal principal, Long stationId, Long requestId,
                                     VehicleRequestReviewDTO review);

    /**
     * 删除申请记录：站级删本站，支队删任意可见站。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param requestId 申请主键
     */
    void deleteRequest(AuthPrincipal principal, Long stationId, Long requestId);
}
