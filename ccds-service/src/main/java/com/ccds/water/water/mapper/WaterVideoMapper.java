package com.ccds.water.water.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.water.water.entity.WaterVideoDO;

/**
 * 水源出水视频访问。
 *
 * @author ccds
 * @since 0.1.2
 */
@Mapper
public interface WaterVideoMapper {

    /**
     * 插入视频记录。
     *
     * @param video 视频
     * @return 影响行数
     */
    int insert(WaterVideoDO video);

    /**
     * 某水源的视频列表。
     *
     * @param waterId 水源主键
     * @return 视频列表，不会为 null
     */
    List<WaterVideoDO> selectByWaterId(@Param("waterId") Long waterId);

    /**
     * 指定水源中「有用」视频的数量（用于「上传出水视频数」统计）。
     *
     * @param stationIds 站主键列表，不得为空
     * @return 按水源分组的计数行，不会为 null
     */
    List<java.util.Map<String, Object>> countVideosByWaterIds(@Param("stationIds") List<Long> stationIds);

    /**
     * 删除某水源的全部视频记录。
     *
     * @param waterId 水源主键
     * @return 影响行数
     */
    int deleteByWaterId(@Param("waterId") Long waterId);
}
