package com.bookcover.archive.service.impl;

import com.bookcover.archive.controller.CoverController;
import com.bookcover.archive.exception.GlobalExceptionHandler;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.ImageAssetRepository;
import com.bookcover.archive.service.ImageService;
import com.bookcover.archive.support.TestImages;
import com.bookcover.archive.validation.ImageValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 허용 픽셀 상한은 업로드·해시가 공유하고, 초과 파일은 저장소 변경 전에 400으로 거절한다. */
class ImageUploadLimitsTest {
    @Test void oversizedButSmallCompressedPngIsRejectedBeforeStorage() throws Exception {
        byte[] bytes = TestImages.png(6400, 6400);
        assertThat(bytes.length).isLessThan((int) ImageValidator.MAX_FILE_SIZE);
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", bytes);
        ImageValidator validator = new ImageValidator();
        assertThat(validator.validate(file).valid()).isFalse();
        assertThatThrownBy(() -> ImageFingerprint.of(bytes)).isInstanceOf(java.io.IOException.class);
        S3Client storage = mock(S3Client.class);
        ImageAssetRepository assets = mock(ImageAssetRepository.class);
        ImageService service = new ImageServiceImpl(storage, validator, assets);
        var mvc = MockMvcBuilders.standaloneSetup(new CoverController(service, mock(BookCoverRepository.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(multipart("/covers").file(file)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThatThrownBy(() -> service.uploadImage(file, ImageService.ImageType.COVER)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(storage, assets);
    }

    @Test void pixelBoundaryIsInclusiveAndUsesLongArithmetic() {
        assertThat(ImageValidator.isWithinPixelLimit(8000, 5000)).isTrue();
        assertThat(ImageValidator.isWithinPixelLimit(8000, 5001)).isFalse();
        assertThat(ImageValidator.isWithinPixelLimit(Integer.MAX_VALUE, 2)).isFalse();
    }

    @Test void ordinaryCoverPassesBothUploadValidationAndFingerprinting() throws Exception {
        byte[] bytes = TestImages.png(800, 1200);
        var file = new MockMultipartFile("file", "cover.png", "image/png", bytes);
        assertThat(new ImageValidator().validate(file).valid()).isTrue();
        assertThat(ImageFingerprint.of(bytes).fileSha256()).hasSize(64);
    }
}
