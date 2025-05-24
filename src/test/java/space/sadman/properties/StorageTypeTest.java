package space.sadman.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StorageType Enum Tests")
class StorageTypeTest {

    @Test
    @DisplayName("Should have exactly two enum values")
    void shouldHaveExactlyTwoEnumValues() {
        // When
        StorageType[] values = StorageType.values();

        // Then
        assertEquals(2, values.length, "StorageType should have exactly 2 enum values");
    }

    @Test
    @DisplayName("Should contain S3 storage type")
    void shouldContainS3StorageType() {
        // When
        StorageType s3 = StorageType.valueOf("s3");

        // Then
        assertEquals(StorageType.s3, s3, "Should contain s3 storage type");
        assertEquals("s3", s3.name(), "S3 enum name should be 's3'");
    }

    @Test
    @DisplayName("Should contain Minio storage type")
    void shouldContainMinioStorageType() {
        // When
        StorageType minio = StorageType.valueOf("minio");

        // Then
        assertEquals(StorageType.minio, minio, "Should contain minio storage type");
        assertEquals("minio", minio.name(), "Minio enum name should be 'minio'");
    }

    @Test
    @DisplayName("Should throw exception for invalid enum value")
    void shouldThrowExceptionForInvalidEnumValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            StorageType.valueOf("invalid");
        }, "Should throw IllegalArgumentException for invalid enum value");
    }

    @Test
    @DisplayName("Should handle ordinal values correctly")
    void shouldHandleOrdinalValuesCorrectly() {
        // When & Then
        assertEquals(0, StorageType.s3.ordinal(), "S3 should have ordinal 0");
        assertEquals(1, StorageType.minio.ordinal(), "Minio should have ordinal 1");
    }

    @Test
    @DisplayName("Should handle toString method")
    void shouldHandleToStringMethod() {
        // When & Then
        assertEquals("s3", StorageType.s3.toString(), "S3 toString should return 's3'");
        assertEquals("minio", StorageType.minio.toString(), "Minio toString should return 'minio'");
    }

    @Test
    @DisplayName("Should verify all enum values are present")
    void shouldVerifyAllEnumValuesArePresent() {
        // Given
        StorageType[] expectedValues = {StorageType.s3, StorageType.minio};

        // When
        StorageType[] actualValues = StorageType.values();

        // Then
        assertArrayEquals(expectedValues, actualValues, "Should have s3 and minio as enum values");
    }
}
