package com.defen.yunyun.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 *
 * @author defen
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                // 允许发送 Cookie
                .allowCredentials(true)
//                 .allowedOriginPatterns("*")
                .allowedOrigins("http://localhost:5173/","http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600)
                .exposedHeaders("*");
    }
}
