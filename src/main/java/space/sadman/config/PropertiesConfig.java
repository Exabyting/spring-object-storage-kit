package space.sadman.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import space.sadman.properties.OskProperties;

@Configuration
@EnableConfigurationProperties({
        OskProperties.class
})
public class PropertiesConfig {
}
