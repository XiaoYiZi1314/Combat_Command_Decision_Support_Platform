-- P7 审查修复：补 gmt_modified；电话改为密文字段
ALTER TABLE `ccds_file_object`
    ADD COLUMN `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间' AFTER `gmt_create`;

ALTER TABLE `ccds_share_token`
    ADD COLUMN `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间' AFTER `gmt_create`;

ALTER TABLE `ccds_key_unit`
    CHANGE COLUMN `phone` `phone_cipher` VARCHAR(512) NULL COMMENT '联系电话密文';
