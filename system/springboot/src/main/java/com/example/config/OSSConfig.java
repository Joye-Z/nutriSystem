package com.example.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 阿里云OSS配置类
 * 使用环境变量配置敏感信息，避免硬编码
 */
@Configuration
public class OSSConfig {

    private static final Logger logger = LoggerFactory.getLogger(OSSConfig.class);
    
    // 从环境变量读取访问凭证
    private final String accessKeyId;
    private final String accessKeySecret;
    
    // 其他配置仍从配置文件读取
    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.bucketName}")
    private String bucketName;

    @Value("${oss.fileUrl}")
    private String fileUrl;
    
    // 构造函数初始化，从环境变量读取凭证
    public OSSConfig() {
        // 尝试从环境变量读取访问凭证
        this.accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        this.accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        
        // 验证环境变量是否存在
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            logger.warn("警告: OSS访问凭证环境变量未设置，请设置OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET环境变量");
        } else {
            logger.info("已从环境变量加载OSS访问凭证");
        }
    }

    /**
     * 创建OSS客户端Bean
     */
    @Bean
    public OSS ossClient() {
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new RuntimeException("OSS访问凭证未配置，请设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET");
        }
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}