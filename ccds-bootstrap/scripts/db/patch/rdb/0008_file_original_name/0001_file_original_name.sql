-- 文件对象补原始文件名，供 APP 展示与下载，不改已发布建表语句。
ALTER TABLE `ccds_file_object`
    ADD COLUMN `original_name` VARCHAR(255) NULL COMMENT '原始文件名' AFTER `cos_key`;
