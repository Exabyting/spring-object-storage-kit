package com.exabyting.springosk.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.exabyting.springosk.annotation.ValidEnum;
import com.exabyting.springosk.properties.StorageType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("ValidEnumValidator Tests")
class ValidEnumValidatorTest {

    private ValidEnumValidator validator;

    @Mock
    private ValidEnum validEnum;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new ValidEnumValidator();
        
        // Mock the annotation to return StorageType enum class
        when(validEnum.enumClass()).thenAnswer(invocation -> StorageType.class);
        validator.initialize(validEnum);
    }

    @Test
    @DisplayName("Should return true for valid enum value")
    void shouldReturnTrueForValidEnumValue() {
        // Given
        String validValue = "s3";

        // When
        boolean result = validator.isValid(validValue, context);

        // Then
        assertTrue(result, "Should return true for valid enum value 's3'");
    }

    @Test
    @DisplayName("Should return true for another valid enum value")
    void shouldReturnTrueForAnotherValidEnumValue() {
        // Given
        String validValue = "minio";

        // When
        boolean result = validator.isValid(validValue, context);

        // Then
        assertTrue(result, "Should return true for valid enum value 'minio'");
    }

    @Test
    @DisplayName("Should return false for invalid enum value")
    void shouldReturnFalseForInvalidEnumValue() {
        // Given
        String invalidValue = "invalid";

        // When
        boolean result = validator.isValid(invalidValue, context);

        // Then
        assertFalse(result, "Should return false for invalid enum value");
    }

    @Test
    @DisplayName("Should return false for empty string")
    void shouldReturnFalseForEmptyString() {
        // Given
        String emptyValue = "";

        // When
        boolean result = validator.isValid(emptyValue, context);

        // Then
        assertFalse(result, "Should return false for empty string");
    }

    @Test
    @DisplayName("Should return true for null value")
    void shouldReturnTrueForNullValue() {
        // Given
        String nullValue = null;

        // When
        boolean result = validator.isValid(nullValue, context);

        // Then
        assertTrue(result, "Should return true for null value (allowing null values)");
    }

    @Test
    @DisplayName("Should be case sensitive")
    void shouldBeCaseSensitive() {
        // Given
        String upperCaseValue = "S3";
        String mixedCaseValue = "Minio";

        // When
        boolean upperCaseResult = validator.isValid(upperCaseValue, context);
        boolean mixedCaseResult = validator.isValid(mixedCaseValue, context);

        // Then
        assertFalse(upperCaseResult, "Should be case sensitive - 'S3' should be invalid");
        assertFalse(mixedCaseResult, "Should be case sensitive - 'Minio' should be invalid");
    }

    @Test
    @DisplayName("Should handle whitespace values")
    void shouldHandleWhitespaceValues() {
        // Given
        String whitespaceValue = "   ";
        String tabValue = "\t";

        // When
        boolean whitespaceResult = validator.isValid(whitespaceValue, context);
        boolean tabResult = validator.isValid(tabValue, context);

        // Then
        assertFalse(whitespaceResult, "Should return false for whitespace-only string");
        assertFalse(tabResult, "Should return false for tab character");
    }
}
