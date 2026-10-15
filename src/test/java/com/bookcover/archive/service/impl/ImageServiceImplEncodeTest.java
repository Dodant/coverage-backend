package com.bookcover.archive.service.impl;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 투명 PNG가 .jpg 이름으로 올라오면 0바이트 파일이 저장되던 문제 (2026-10 로컬에서 발견) */
class ImageServiceImplEncodeTest {

    @Test
    void transparentImageIsFlattenedWhenSavedAsJpeg() throws IOException {
        BufferedImage argb = new BufferedImage(40, 60, BufferedImage.TYPE_INT_ARGB);
        argb.setRGB(5, 5, 0x80FF0000);

        byte[] bytes = ImageServiceImpl.imageToBytes(argb, "jpg");

        assertThat(bytes).isNotEmpty();
        BufferedImage back = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(back.getWidth()).isEqualTo(40);
        assertThat(back.getHeight()).isEqualTo(60);
        assertThat(back.getRGB(0, 0) & 0xFFFFFF).isEqualTo(0xFFFFFF); // 투명 부분은 흰 배경
    }

    @Test
    void pngKeepsAlpha() throws IOException {
        BufferedImage argb = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        assertThat(ImageIO.read(new ByteArrayInputStream(ImageServiceImpl.imageToBytes(argb, "png"))).getColorModel().hasAlpha()).isTrue();
    }

    @Test
    void unknownFormatFailsInsteadOfStoringEmptyFile() {
        BufferedImage rgb = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        assertThrows(IOException.class, () -> ImageServiceImpl.imageToBytes(rgb, "heic"));
    }
}
