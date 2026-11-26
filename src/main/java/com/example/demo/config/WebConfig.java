package com.example.demo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration 
@RequiredArgsConstructor 
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/admin/**", "/user/**", "/login", "/signup");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membuat folder upload", e);
        }

        String path = uploadPath.toUri().toString();
        if (!path.endsWith("/")) path += "/";

        // Request ke seluruh gambar harus berasal dari logged in user/admin
        registry.addResourceHandler("/user/uploads/**")
            .addResourceLocations(path);
    }
}
