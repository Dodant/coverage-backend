package com.bookcover.archive.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ImageValidator.
 * Validates: Requirements 14.1, 14.4, 14.5
 */
class ImageValidatorTest {

    private ImageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImageValidator();
    }

    @Nested
    @DisplayName("File Format Validation")
    class FormatTests {

        @Test
        @DisplayName("JPEG content type is valid")
        void jpegContentTypeValid() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", new byte[100]);
            assertTrue(validator.isValidFormat(file));
        }

        @Test
        @DisplayName("PNG content type is valid")
        void pngContentTypeValid() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.png", "image/png", new byte[100]);
            assertTrue(validator.isValidFormat(file));
        }

        @Test
        @DisplayName("WebP content type is valid")
        void webpContentTypeValid() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.webp", "image/webp", new byte[100]);
            assertTrue(validator.isValidFormat(file));
        }

        @Test
        @DisplayName("GIF content type is invalid")
        void gifContentTypeInvalid() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.gif", "image/gif", new byte[100]);
            assertFalse(validator.isValidFormat(file));
        }

        @Test
        @DisplayName("Valid extension without content type")
        void validExtensionWithoutContentType() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.png", null, new byte[100]);
            assertTrue(validator.isValidFormat(file));
        }

        @Test
        @DisplayName("Null file is invalid")
        void nullFileInvalid() {
            assertFalse(validator.isValidFormat(null));
        }
    }

    @Nested
    @DisplayName("File Size Validation")
    class SizeTests {

        @Test
        @DisplayName("File under 10MB is valid")
        void fileUnderLimitValid() {
            byte[] content = new byte[5 * 1024 * 1024]; // 5MB
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", content);
            assertTrue(validator.isValidFileSize(file));
        }

        @Test
        @DisplayName("File exactly 10MB is valid")
        void fileAtLimitValid() {
            byte[] content = new byte[10 * 1024 * 1024]; // 10MB
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", content);
            assertTrue(validator.isValidFileSize(file));
        }

        @Test
        @DisplayName("File over 10MB is invalid")
        void fileOverLimitInvalid() {
            byte[] content = new byte[11 * 1024 * 1024]; // 11MB
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", content);
            assertFalse(validator.isValidFileSize(file));
        }

        @Test
        @DisplayName("Null file is invalid")
        void nullFileInvalid() {
            assertFalse(validator.isValidFileSize(null));
        }
    }

    @Nested
    @DisplayName("Resolution Validation")
    class ResolutionTests {

        @Test
        @DisplayName("Resolution at minimum is valid")
        void resolutionAtMinimumValid() {
            assertTrue(validator.isValidResolution(800, 1200));
        }

        @Test
        @DisplayName("Resolution above minimum is valid")
        void resolutionAboveMinimumValid() {
            assertTrue(validator.isValidResolution(1600, 2400));
        }

        @Test
        @DisplayName("Width below minimum is invalid")
        void widthBelowMinimumInvalid() {
            assertFalse(validator.isValidResolution(799, 1200));
        }

        @Test
        @DisplayName("Height below minimum is invalid")
        void heightBelowMinimumInvalid() {
            assertFalse(validator.isValidResolution(800, 1199));
        }

        @Test
        @DisplayName("Both dimensions below minimum is invalid")
        void bothBelowMinimumInvalid() {
            assertFalse(validator.isValidResolution(400, 600));
        }
    }

    @Nested
    @DisplayName("Full Validation")
    class FullValidationTests {

        @Test
        @DisplayName("Valid image passes all checks")
        void validImagePasses() throws IOException {
            MockMultipartFile file = createValidImageFile(1000, 1500);
            ImageValidator.ImageValidationResult result = validator.validate(file);

            assertTrue(result.valid());
            assertTrue(result.errors().isEmpty());
            assertEquals(1000, result.width());
            assertEquals(1500, result.height());
        }

        @Test
        @DisplayName("Null file fails validation")
        void nullFileFails() {
            ImageValidator.ImageValidationResult result = validator.validate(null);

            assertFalse(result.valid());
            assertFalse(result.errors().isEmpty());
        }

        @Test
        @DisplayName("Empty file fails validation")
        void emptyFileFails() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", new byte[0]);
            ImageValidator.ImageValidationResult result = validator.validate(file);

            assertFalse(result.valid());
        }

        @Test
        @DisplayName("Image with low resolution fails")
        void lowResolutionFails() throws IOException {
            MockMultipartFile file = createValidImageFile(400, 600);
            ImageValidator.ImageValidationResult result = validator.validate(file);

            assertFalse(result.valid());
            assertTrue(result.errors().stream()
                    .anyMatch(e -> e.contains("resolution")));
        }
    }

    /**
     * Helper method to create a valid image file for testing.
     */
    private MockMultipartFile createValidImageFile(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        return new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                baos.toByteArray()
        );
    }
}
