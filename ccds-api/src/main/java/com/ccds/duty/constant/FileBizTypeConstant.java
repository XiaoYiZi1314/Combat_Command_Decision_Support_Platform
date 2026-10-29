package com.ccds.duty.constant;

/**
 * 文件业务类型常量
 *
 * @author system
 * @since 2024
 */
public final class FileBizTypeConstant {

    /**
     * 水源照片
     */
    public static final String WATER_PHOTO = "water_photo";

    /**
     * 水源档案材料
     */
    public static final String WATER_DOC = "water_doc";

    /**
     * 水源出水视频（限 10 秒）
     */
    public static final String WATER_VIDEO = "water_video";

    /**
     * 出水视频三帧（不混入水源普通照片列表）。
     */
    public static final String WATER_VIDEO_FRAME = "water_video_frame";

    /**
     * 重点单位预案文件
     */
    public static final String KEYUNIT_PLAN = "keyunit_plan";

    /**
     * 重点单位平面图
     */
    public static final String KEYUNIT_FLOOR = "keyunit_floor";

    /**
     * 危化品识别图片
     */
    public static final String HAZMAT_IMAGE = "hazmat_image";

    private FileBizTypeConstant() {
    }
}
