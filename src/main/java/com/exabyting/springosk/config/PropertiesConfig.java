package com.exabyting.springosk.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.exabyting.springosk.properties.OskProperties;

@Configuration
@EnableConfigurationProperties({
        OskProperties.class
})
public class PropertiesConfig {
}
