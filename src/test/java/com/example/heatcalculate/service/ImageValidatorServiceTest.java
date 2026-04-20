package com.example.heatcalculate.service;

import com.example.heatcalculate.exception.ImageValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图片校验服务测试
 */
class ImageValidatorServiceTest {

    private ImageValidatorService imageValidatorService;

    @BeforeEach
    void setUp() {
        imageValidatorService = new ImageValidatorService();
    }

    @Test
    @DisplayName("校验通过 - 合法的JPG图片")
    void validate_ValidJpgImage_ShouldPass() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.jpg",
            "image/jpeg",
            new byte[1024] // 1KB
        );

        // Then
        assertDoesNotThrow(() -> imageValidatorService.validate(file));
    }

    @Test
    @DisplayName("校验通过 - 合法的PNG图片")
    void validate_ValidPngImage_ShouldPass() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.png",
            "image/png",
            new byte[1024]
        );

        // Then
        assertDoesNotThrow(() -> imageValidatorService.validate(file));
    }

    @Test
    @DisplayName("校验通过 - 合法的WEBP图片")
    void validate_ValidWebpImage_ShouldPass() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.webp",
            "image/webp",
            new byte[1024]
        );

        // Then
        assertDoesNotThrow(() -> imageValidatorService.validate(file));
    }

    @Test
    @DisplayName("校验失败 - 不支持的格式(GIF)")
    void validate_InvalidFormatGif_ShouldThrowException() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.gif",
            "image/gif",
            new byte[1024]
        );

        // Then
        ImageValidationException exception = assertThrows(
            ImageValidationException.class,
            () -> imageValidatorService.validate(file)
        );
        assertEquals("仅支持 JPG、PNG、WEBP 格式的图片", exception.getMessage());
    }

    @Test
    @DisplayName("校验失败 - 不支持的格式(BMP)")
    void validate_InvalidFormatBmp_ShouldThrowException() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.bmp",
            "image/bmp",
            new byte[1024]
        );

        // Then
        ImageValidationException exception = assertThrows(
            ImageValidationException.class,
            () -> imageValidatorService.validate(file)
        );
        assertEquals("仅支持 JPG、PNG、WEBP 格式的图片", exception.getMessage());
    }

    @Test
    @DisplayName("校验失败 - 文件过大(超过10MB)")
    void validate_FileTooLarge_ShouldThrowException() {
        // Given - 11MB
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
            "image",
            "large.jpg",
            "image/jpeg",
            largeContent
        );

        // Then
        ImageValidationException exception = assertThrows(
            ImageValidationException.class,
            () -> imageValidatorService.validate(file)
        );
        assertEquals("图片大小不能超过10MB", exception.getMessage());
    }

    @Test
    @DisplayName("校验通过 - 刚好10MB的文件")
    void validate_FileExactly10MB_ShouldPass() {
        // Given - 10MB exactly
        byte[] content = new byte[10 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
            "image",
            "exactly10mb.jpg",
            "image/jpeg",
            content
        );

        // Then
        assertDoesNotThrow(() -> imageValidatorService.validate(file));
    }

    @Test
    @DisplayName("校验失败 - 空文件")
    void validate_EmptyFile_ShouldThrowException() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        // Then
        ImageValidationException exception = assertThrows(
            ImageValidationException.class,
            () -> imageValidatorService.validate(file)
        );
        assertEquals("图片不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("校验失败 - null文件")
    void validate_NullFile_ShouldThrowException() {
        // Then
        ImageValidationException exception = assertThrows(
            ImageValidationException.class,
            () -> imageValidatorService.validate(null)
        );
        assertEquals("图片不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("校验失败 - 内容类型为null")
    void validate_NullContentType_ShouldThrowException() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.jpg",
            null,
            new byte[1024]
        );

        // Then
        ImageValidationException exception = assertThrows(
            ImageValidationException.class,
            () -> imageValidatorService.validate(file)
        );
        assertEquals("仅支持 JPG、PNG、WEBP 格式的图片", exception.getMessage());
    }

    @Test
    @DisplayName("校验通过 - contentType大小写不敏感")
    void validate_CaseInsensitiveContentType_ShouldPass() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.jpg",
            "IMAGE/JPEG", // 大写
            new byte[1024]
        );

        // Then
        assertDoesNotThrow(() -> imageValidatorService.validate(file));
    }
}
