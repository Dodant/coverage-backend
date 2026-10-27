package com.bookcover.archive.service.impl;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 파일 SHA-256은 동일 파일, 64비트 dHash는 사람이 확인할 유사 후보를 찾는다. */
public record ImageFingerprint(String fileSha256, String perceptualHash) {
    public static ImageFingerprint of(byte[] bytes) throws IOException {
        BufferedImage image;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("지원하지 않는 이미지입니다.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000L) throw new IOException("이미지 크기가 너무 큽니다.");
                image = reader.read(0);
            } finally { reader.dispose(); }
        }
        return new ImageFingerprint(sha256(bytes), differenceHash(image));
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static String differenceHash(BufferedImage image) {
        BufferedImage small = new BufferedImage(9, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, 9, 8);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, 9, 8, null); g.dispose();
        long hash = 0;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            hash <<= 1;
            if (luminance(small.getRGB(x, y)) > luminance(small.getRGB(x + 1, y))) hash |= 1;
        }
        return String.format("%016x", hash);
    }
    private static int luminance(int rgb) { return ((rgb >> 16) & 255) * 299 + ((rgb >> 8) & 255) * 587 + (rgb & 255) * 114; }
    public static int distance(String left, String right) {
        return Long.bitCount(Long.parseUnsignedLong(left, 16) ^ Long.parseUnsignedLong(right, 16));
    }
}
