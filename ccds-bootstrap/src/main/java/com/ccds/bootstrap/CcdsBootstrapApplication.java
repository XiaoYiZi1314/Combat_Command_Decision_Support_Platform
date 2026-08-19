package com.ccds.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 作战指挥辅助决策平台启动入口。
 *
 * @author ccds
 * @since 0.1.0
 */
@SpringBootApplication(scanBasePackages = "com.ccds", exclude = {DataSourceAutoConfiguration.class})
public class CcdsBootstrapApplication {

    /**
     * 启动 Spring Boot。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CcdsBootstrapApplication.class, args);
    }
}
