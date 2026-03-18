package com.eduquiz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map URL /uploads/** → file system upload directory
        // VD: GET /uploads/subjects/abc.png → ./uploads/subjects/abc.png
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        log.info("[WebConfig] Serving static files: /uploads/** → {}", absolutePath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath)
                .setCachePeriod(3600);  // cache 1 giờ
    }
}
