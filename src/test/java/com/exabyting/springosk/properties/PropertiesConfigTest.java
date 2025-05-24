package com.exabyting.springosk.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.exabyting.springosk.config.PropertiesConfig;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PropertiesConfig Tests")
class PropertiesConfigTest {

    @Test
    @DisplayName("Should be annotated with @Configuration")
    void shouldBeAnnotatedWithConfiguration() {
        // When
        Configuration configAnnotation = PropertiesConfig.class.getAnnotation(Configuration.class);

        // Then
        assertNotNull(configAnnotation, "PropertiesConfig should be annotated with @Configuration");
    }

    @Test
    @DisplayName("Should be annotated with @EnableConfigurationProperties and enable OskProperties")
    void shouldBeAnnotatedWithEnableConfigurationPropertiesAndEnableOskProperties() {
        // When
        EnableConfigurationProperties enableAnnotation = PropertiesConfig.class.getAnnotation(EnableConfigurationProperties.class);

        // Then
        assertNotNull(enableAnnotation, "PropertiesConfig should be annotated with @EnableConfigurationProperties");
        
        Class<?>[] enabledClasses = enableAnnotation.value();
        assertEquals(1, enabledClasses.length, "Should enable exactly one properties class");
        assertEquals(OskProperties.class, enabledClasses[0], "Should enable OskProperties class");
    }

    @Test
    @DisplayName("Should be in correct package")
    void shouldBeInCorrectPackage() {
        // When
        String packageName = PropertiesConfig.class.getPackage().getName();

        // Then
        assertEquals("com.exabyting.springosk.config", packageName,
                "PropertiesConfig should be in com.exabyting.springosk.config package");
    }

    @Test
    @DisplayName("Should be instantiable")
    void shouldBeInstantiable() {
        // When & Then
        assertDoesNotThrow(() -> {
            PropertiesConfig config = new PropertiesConfig();
            assertNotNull(config, "Should be able to create instance of PropertiesConfig");
        }, "Should be able to instantiate PropertiesConfig");
    }

    @Test
    @DisplayName("Should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
        // When
        var constructor = PropertiesConfig.class.getDeclaredConstructor();

        // Then
        assertNotNull(constructor, "Should have default constructor");
        assertEquals(0, constructor.getParameterCount(), "Default constructor should have no parameters");
    }

    @Test
    @DisplayName("Should be a configuration class")
    void shouldBeAConfigurationClass() {
        // When & Then
        assertFalse(PropertiesConfig.class.isInterface(), "Should not be an interface");
        assertFalse(PropertiesConfig.class.isEnum(), "Should not be an enum");
        assertTrue(PropertiesConfig.class.isAnnotationPresent(Configuration.class), 
                "Should be annotated with @Configuration");
    }
}
