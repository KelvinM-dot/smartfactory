package com.jqhc.dataplatform;

import com.jqhc.dataplatform.config.JqhcProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JqhcProperties.class)
public class DataplatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataplatformApplication.class, args);
    }
}
