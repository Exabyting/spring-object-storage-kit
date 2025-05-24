package space.sadman.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import space.sadman.properties.StorageType;
import space.sadman.validator.ValidEnumValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidEnum Annotation Tests")
class ValidEnumTest {

    @Test
    @DisplayName("Should have correct annotation properties")
    void shouldHaveCorrectAnnotationProperties() throws NoSuchFieldException {
        // When
        ValidEnum annotation = TestClassWithValidEnum.class
                .getDeclaredField("storageType")
                .getAnnotation(ValidEnum.class);

        // Then
        assertNotNull(annotation, "ValidEnum annotation should be present");
        assertEquals(StorageType.class, annotation.enumClass(), "Should have correct enum class");
        assertEquals("must be a valid enum value", annotation.message(), "Should have default message");
        assertEquals(0, annotation.groups().length, "Should have empty groups array");
        assertEquals(0, annotation.payload().length, "Should have empty payload array");
    }

    @Test
    @DisplayName("Should have correct retention policy")
    void shouldHaveCorrectRetentionPolicy() {
        // When
        Retention retention = ValidEnum.class.getAnnotation(Retention.class);

        // Then
        assertNotNull(retention, "Should have Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(), "Should have RUNTIME retention policy");
    }

    @Test
    @DisplayName("Should have correct target elements")
    void shouldHaveCorrectTargetElements() {
        // When
        Target target = ValidEnum.class.getAnnotation(Target.class);

        // Then
        assertNotNull(target, "Should have Target annotation");
        ElementType[] expectedTargets = {ElementType.FIELD, ElementType.PARAMETER};
        assertArrayEquals(expectedTargets, target.value(), "Should target FIELD and PARAMETER");
    }

    @Test
    @DisplayName("Should use ValidEnumValidator as constraint validator")
    void shouldUseValidEnumValidatorAsConstraintValidator() {
        // When
        jakarta.validation.Constraint constraint = ValidEnum.class.getAnnotation(jakarta.validation.Constraint.class);

        // Then
        assertNotNull(constraint, "Should have Constraint annotation");
        assertEquals(1, constraint.validatedBy().length, "Should have one validator");
        assertEquals(ValidEnumValidator.class, constraint.validatedBy()[0], "Should use ValidEnumValidator");
    }

    @Test
    @DisplayName("Should allow custom message")
    void shouldAllowCustomMessage() throws NoSuchFieldException {
        // When
        ValidEnum annotation = TestClassWithCustomMessage.class
                .getDeclaredField("type")
                .getAnnotation(ValidEnum.class);

        // Then
        assertNotNull(annotation, "ValidEnum annotation should be present");
        assertEquals("Custom validation message", annotation.message(), "Should have custom message");
    }

    // Test helper classes
    private static class TestClassWithValidEnum {
        @ValidEnum(enumClass = StorageType.class)
        private String storageType;
    }

    private static class TestClassWithCustomMessage {
        @ValidEnum(enumClass = StorageType.class, message = "Custom validation message")
        private String type;
    }
}
