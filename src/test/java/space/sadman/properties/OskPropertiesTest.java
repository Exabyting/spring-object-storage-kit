package space.sadman.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OskProperties Tests")
class OskPropertiesTest {

    private Validator validator;
    private OskProperties oskProperties;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        oskProperties = new OskProperties();
        
        // Set required fields to avoid validation failures
        oskProperties.setEndpoint("http://localhost:9000");
        oskProperties.setRegion("us-east-1");
        oskProperties.setAccessKey("test-access-key");
        oskProperties.setSecretKey("test-secret-key");
        oskProperties.setDefaultBucket("test-bucket");
    }

    @Test
    @DisplayName("Should pass validation with valid S3 storage type")
    void shouldPassValidationWithValidS3StorageType() {
        // Given
        oskProperties.setStorageType(StorageType.s3);

        // When
        Set<ConstraintViolation<OskProperties>> violations = validator.validate(oskProperties);

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation violations for valid S3 storage type");
    }

    @Test
    @DisplayName("Should pass validation with valid Minio storage type")
    void shouldPassValidationWithValidMinioStorageType() {
        // Given
        oskProperties.setStorageType(StorageType.minio);

        // When
        Set<ConstraintViolation<OskProperties>> violations = validator.validate(oskProperties);

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation violations for valid Minio storage type");
    }

    @Test
    @DisplayName("Should fail validation when storage type is null")
    void shouldFailValidationWhenStorageTypeIsNull() {
        // Given
        oskProperties.setStorageType(null);

        // When
        Set<ConstraintViolation<OskProperties>> violations = validator.validate(oskProperties);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation violations when storage type is null");
        assertEquals(1, violations.size(), "Should have exactly one validation violation");
        
        ConstraintViolation<OskProperties> violation = violations.iterator().next();
        assertEquals("storageType", violation.getPropertyPath().toString(), "Violation should be on storageType property");
    }

    @Test
    @DisplayName("Should fail validation when required fields are null")
    void shouldFailValidationWhenRequiredFieldsAreNull() {
        // Given - create a completely empty OskProperties object
        OskProperties emptyProperties = new OskProperties();

        // When
        Set<ConstraintViolation<OskProperties>> violations = validator.validate(emptyProperties);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation violations when required fields are null");
        assertEquals(6, violations.size(), "Should have validation violations for all 6 @NotNull fields");
        
        // Verify that violations are for the expected fields
        var violatedPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .sorted()
                .toList();
        
        assertEquals(List.of("accessKey", "defaultBucket", "endpoint", "region", "secretKey", "storageType"), 
                violatedPaths, "Violations should be for all required fields");
    }

    @Test
    @DisplayName("Should validate successfully with all required fields set")
    void shouldValidateSuccessfullyWithAllRequiredFieldsSet() {
        // Given - oskProperties is already set up in @BeforeEach with all required fields
        oskProperties.setStorageType(StorageType.s3);

        // When
        Set<ConstraintViolation<OskProperties>> violations = validator.validate(oskProperties);

        // Then
        assertTrue(violations.isEmpty(), "Should have no validation violations when all required fields are set");
    }

    @Test
    @DisplayName("Should properly set and get storage type")
    void shouldProperlySetAndGetStorageType() {
        // Given & When
        oskProperties.setStorageType(StorageType.s3);

        // Then
        assertEquals(StorageType.s3, oskProperties.getStorageType(), "Should return the set storage type");
    }

    @Test
    @DisplayName("Should handle toString method")
    void shouldHandleToStringMethod() {
        // Given
        oskProperties.setStorageType(StorageType.minio);

        // When
        String toString = oskProperties.toString();

        // Then
        assertNotNull(toString, "toString should not return null");
        assertTrue(toString.contains("minio"), "toString should contain the storage type value");
    }

    @Test
    @DisplayName("Should handle equals and hashCode methods")
    void shouldHandleEqualsAndHashCodeMethods() {
        // Given
        OskProperties props1 = new OskProperties();
        OskProperties props2 = new OskProperties();
        props1.setStorageType(StorageType.s3);
        props2.setStorageType(StorageType.s3);

        // When & Then
        assertEquals(props1, props2, "Properties with same storage type should be equal");
        assertEquals(props1.hashCode(), props2.hashCode(), "Properties with same storage type should have same hash code");

        // Change one property
        props2.setStorageType(StorageType.minio);
        assertNotEquals(props1, props2, "Properties with different storage types should not be equal");
    }
}
