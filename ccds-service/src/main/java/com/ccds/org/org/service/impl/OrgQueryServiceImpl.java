package com.ccds.org.org.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.vo.BrigadeVO;
import com.ccds.iam.identity.vo.MeVO;
import com.ccds.iam.identity.vo.OrgTreeVO;
import com.ccds.iam.identity.vo.StationVO;
import com.ccds.org.org.entity.BrigadeDO;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.BrigadeMapper;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 站级只见本站，大队见下属站，支队/开发者见全部。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
public class OrgQueryServiceImpl implements OrgQueryService {

    private static final String HOME_ATTACK = "#/attack";

    private static final String HOME_HQ = "#/hq";

    private static final String DIRECT_BRIGADE_CODE = "direct";

    private static final String DIRECT_BRIGADE_NAME = "支队直属";

    private final StationMapper stationMapper;

    private final BrigadeMapper brigadeMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public MeVO buildMe(AccountDO account) {
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        List<StationVO> visible = listVisibleStations(account);
        StationVO ownStation = findStation(account.getStationId());
        BrigadeDO ownBrigade = account.getBrigadeId() == null
                ? null
                : brigadeMapper.selectById(account.getBrigadeId());
        boolean command = role != null && role.isCommandRole();
        return MeVO.builder()
                .accountId(account.getId())
                .username(account.getUsername())
                .role(account.getRole())
                .stationId(account.getStationId())
                .stationName(ownStation == null ? null : ownStation.getName())
                .brigadeId(account.getBrigadeId())
                .brigadeName(ownBrigade == null ? null : ownBrigade.getName())
                .mustChangePassword(Boolean.TRUE.equals(account.getMustChangePassword()))
                .homeHash(command ? HOME_HQ : HOME_ATTACK)
                .visibleStations(visible)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrgTreeVO buildOrgTree(AccountDO account) {
        List<StationVO> visible = listVisibleStations(account);
        Map<Long, BrigadeVO> brigadeMap = new LinkedHashMap<>();
        List<StationVO> directStations = new ArrayList<StationVO>();
        Map<Long, BrigadeDO> allBrigades = loadBrigadeMap();
        for (StationVO station : visible) {
            if (station.getBrigadeId() == null) {
                directStations.add(station);
                continue;
            }
            BrigadeVO node = brigadeMap.get(station.getBrigadeId());
            if (node == null) {
                BrigadeDO brigade = allBrigades.get(station.getBrigadeId());
                node = BrigadeVO.builder()
                        .id(station.getBrigadeId())
                        .code(brigade == null ? DIRECT_BRIGADE_CODE : brigade.getCode())
                        .name(brigade == null ? DIRECT_BRIGADE_NAME : brigade.getName())
                        .stations(new ArrayList<StationVO>())
                        .build();
                brigadeMap.put(station.getBrigadeId(), node);
            }
            node.getStations().add(station);
        }
        return OrgTreeVO.builder()
                .brigades(new ArrayList<BrigadeVO>(brigadeMap.values()))
                .directStations(directStations)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StationVO> listVisibleStations(AccountDO account) {
        return toStationVoList(listVisibleStationEntities(account));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StationDO> listVisibleStationEntities(AccountDO account) {
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role == null) {
            return List.of();
        }
        switch (role) {
            case STATION:
                StationDO own = account.getStationId() == null ? null : stationMapper.selectById(account.getStationId());
                return own == null ? List.of() : List.of(own);
            case BRIGADE:
                if (account.getBrigadeId() == null) {
                    return List.of();
                }
                return stationMapper.selectByBrigadeId(account.getBrigadeId());
            case HQ:
            case DEVELOPER:
                return stationMapper.selectAll();
            default:
                return List.of();
        }
    }

    private StationVO findStation(Long stationId) {
        if (stationId == null) {
            return null;
        }
        StationDO station = stationMapper.selectById(stationId);
        return station == null ? null : toStationVo(station);
    }

    private Map<Long, BrigadeDO> loadBrigadeMap() {
        Map<Long, BrigadeDO> map = new LinkedHashMap<>();
        List<BrigadeDO> brigades = brigadeMapper.selectAll();
        for (BrigadeDO brigade : brigades) {
            map.put(brigade.getId(), brigade);
        }
        return map;
    }

    private List<StationVO> toStationVoList(List<StationDO> stations) {
        List<StationVO> result = new ArrayList<StationVO>();
        if (stations == null) {
            return result;
        }
        for (StationDO station : stations) {
            result.add(toStationVo(station));
        }
        return result;
    }

    private StationVO toStationVo(StationDO station) {
        return StationVO.builder()
                .id(station.getId())
                .name(station.getName())
                .brigadeId(station.getBrigadeId())
                .brigadeName(station.getBrigadeName())
                .category(station.getCategory())
                .sortNo(station.getSortNo())
                .build();
    }
}
