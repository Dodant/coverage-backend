package com.bookcover.archive.service.impl;

import org.junit.jupiter.api.Test;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.*;

class ImageFingerprintTest {
    private static BufferedImage image(int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);
        g.setColor(Color.BLUE); g.fillRect(w / 8, h / 8, w / 2, h / 4);
        g.setColor(Color.BLACK); g.fillOval(w / 3, h / 2, w / 2, h / 3); g.dispose(); return image;
    }
    private static byte[] bytes(BufferedImage image, String type) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(image, type, out); return out.toByteArray();
    }
    @Test void identicalBytesMatchButRecompressionIsOnlyASimilarCandidate() throws Exception {
        byte[] png = bytes(image(800, 1200), "png");
        var original = ImageFingerprint.of(png); var same = ImageFingerprint.of(png);
        var jpeg = ImageFingerprint.of(bytes(image(800, 1200), "jpg"));
        var smaller = ImageFingerprint.of(bytes(image(400, 600), "png"));
        assertThat(original.fileSha256()).hasSize(64).isEqualTo(same.fileSha256()).isNotEqualTo(jpeg.fileSha256());
        assertThat(ImageFingerprint.distance(original.perceptualHash(), jpeg.perceptualHash())).isLessThanOrEqualTo(6);
        assertThat(ImageFingerprint.distance(original.perceptualHash(), smaller.perceptualHash())).isLessThanOrEqualTo(6);
    }
    @Test void malformedImageFailsAndHashDistanceCountsBits() {
        assertThatThrownBy(() -> ImageFingerprint.of(new byte[]{1, 2, 3})).isInstanceOf(java.io.IOException.class);
        assertThat(ImageFingerprint.distance("ffffffffffffffff", "0000000000000000")).isEqualTo(64);
        assertThat(ImageFingerprint.distance("8000000000000000", "0000000000000000")).isEqualTo(1);
    }
}
