package com.app;

import com.app.config.EnvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CulturalResourcesApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CulturalResourcesApplication.class);
        app.addInitializers(new EnvInitializer());
        app.run(args);
    }
}

