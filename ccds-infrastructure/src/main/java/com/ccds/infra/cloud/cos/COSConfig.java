package com.ccds.infra.cloud.cos;

import com.ccds.infra.cloud.TencentCloudEnvConstant;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云COS配置
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Configuration
public class COSConfig {

    @Bean
    public COSClient cosClient() {
        String region = System.getenv(TencentCloudEnvConstant.COS_REGION);
        String secretId = System.getenv(TencentCloudEnvConstant.COS_SECRET_ID);
        String secretKey = System.getenv(TencentCloudEnvConstant.COS_SECRET_KEY);

        if (region == null || secretId == null || secretKey == null) {
            log.warn("COS配置缺失，COS功能将不可用");
            return null;
        }

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        
        log.info("初始化COS客户端：region={}", region);
        return new COSClient(cred, clientConfig);
    }

    @Bean
    public String cosBucket() {
        return System.getenv(TencentCloudEnvConstant.COS_BUCKET);
    }

    @Bean
    public String cosRegion() {
        return System.getenv(TencentCloudEnvConstant.COS_REGION);
    }
}
