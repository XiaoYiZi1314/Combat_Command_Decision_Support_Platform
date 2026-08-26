package com.ccds.infra.cloud.cos;

import org.springframework.beans.factory.DisposableBean;

import com.qcloud.cos.COSClient;

/**
 * COS 客户端与桶名。配置缺失时 client 为空。
 *
 * @author ccds
 * @since 0.1.0
 */
public class CosClientHolder implements DisposableBean {

    private final COSClient client;

    private final String bucket;

    /**
     * 构造持有者。
     *
     * @param client COS 客户端，未配置时为 null
     * @param bucket 桶名，未配置时为 null
     */
    public CosClientHolder(COSClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    /**
     * COS 客户端。
     *
     * @return 客户端，未配置为 null
     */
    public COSClient getClient() {
        return client;
    }

    /**
     * 桶名。
     *
     * @return 桶名，未配置为 null
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * 是否已配置且可用。
     *
     * @return 可用为 true
     */
    public boolean ready() {
        return client != null && bucket != null && !bucket.isBlank();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        if (client != null) {
            client.shutdown();
        }
    }
}
