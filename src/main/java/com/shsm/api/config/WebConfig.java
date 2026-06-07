package com.shsm.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.profile-pictures-dir:profile_pictures}")
    private String profilePicturesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(profilePicturesDir).toAbsolutePath();
        registry.addResourceHandler("/profile_pictures/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}