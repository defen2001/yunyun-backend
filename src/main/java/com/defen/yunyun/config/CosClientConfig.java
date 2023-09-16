//package com.defen.yunyun.config;
//
//import com.qcloud.cos.COSClient;
//import com.qcloud.cos.ClientConfig;
//import com.qcloud.cos.auth.BasicCOSCredentials;
//import com.qcloud.cos.auth.COSCredentials;
//import com.qcloud.cos.http.HttpProtocol;
//import com.qcloud.cos.region.Region;
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 腾讯云对象存储客户端
// */
//@Configuration
//@ConfigurationProperties(prefix = "cos")
//@Data
//public class CosClientConfig {
//
//    /**
//     * secretId
//     */
//    private String secretId;
//
//    /**
//     * secretKey
//     */
//    private String secretKey;
//
//    /**
//     * 区域
//     */
//    private String region;
//
//    /**
//     * 桶名
//     */
//    private String bucketName;
//
//    @Bean
//    public COSClient getCosClient() {
//        // 1 初始化用户身份信息(secretId, secretKey)
//        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
//        // 2.1设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
//        ClientConfig clientConfig = new ClientConfig(new Region(region));
//        // 2.2 使用https协议传输
//        clientConfig.setHttpProtocol(HttpProtocol.https);
//        // 3 生成cos客户端
//        return new COSClient(cred, clientConfig);
//    }
//
//}
