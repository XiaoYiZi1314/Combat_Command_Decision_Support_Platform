package com.ccds.infra.cloud.cos;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ccds.infra.cloud.TencentCloudEnvConstant;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;

import lombok.extern.slf4j.Slf4j;

/**
 * 腾讯云 COS 配置。密钥只从环境变量读取。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Configuration
public class COSConfig {

    /**
     * COS 客户端持有者。缺配置时 client 为空，业务侧转业务异常。
     *
     * @return 持有者，不会为 null
     */
    @Bean
    public CosClientHolder cosClientHolder() {
        String region = System.getenv(TencentCloudEnvConstant.COS_REGION);
        String secretId = System.getenv(TencentCloudEnvConstant.COS_SECRET_ID);
        String secretKey = System.getenv(TencentCloudEnvConstant.COS_SECRET_KEY);
        String bucket = System.getenv(TencentCloudEnvConstant.COS_BUCKET);
        if (region == null || region.isBlank()
                || secretId == null || secretId.isBlank()
                || secretKey == null || secretKey.isBlank()
                || bucket == null || bucket.isBlank()) {
            log.warn("COS配置缺失，COS功能将不可用");
            return new CosClientHolder(null, bucket);
        }
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        log.info("初始化COS客户端：region={}", region);
        return new CosClientHolder(new COSClient(cred, clientConfig), bucket);
    }
}
