package com.bookcover.archive.service.impl;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 이미지에서 대표 색상을 추출하는 유틸리티 클래스.
 * K-means 기반 색상 클러스터링을 사용하여 대표 색상을 추출합니다.
 *
 * Requirements: 20.2 - 대표 색상 추출 로직
 */
public class ColorExtractor {

    private static final int DEFAULT_SAMPLE_RATE = 10;
    private static final int DEFAULT_QUANTIZATION_LEVEL = 8;

    /**
     * 이미지에서 대표 색상을 추출합니다.
     *
     * @param image 분석할 이미지
     * @return HEX 형식의 대표 색상 (예: #FF5733)
     */
    public static String extractDominantColor(BufferedImage image) {
        return extractDominantColor(image, DEFAULT_SAMPLE_RATE);
    }

    /**
     * 이미지에서 대표 색상을 추출합니다.
     *
     * @param image 분석할 이미지
     * @param sampleRate 샘플링 비율 (성능 최적화용)
     * @return HEX 형식의 대표 색상 (예: #FF5733)
     */
    public static String extractDominantColor(BufferedImage image, int sampleRate) {
        if (image == null) {
            return null;
        }

        Map<Integer, Integer> colorCounts = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y += sampleRate) {
            for (int x = 0; x < image.getWidth(); x += sampleRate) {
                int rgb = image.getRGB(x, y);
                int quantizedRgb = quantizeColor(rgb, DEFAULT_QUANTIZATION_LEVEL);
                colorCounts.merge(quantizedRgb, 1, Integer::sum);
            }
        }

        int dominantColor = colorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        return rgbToHex(dominantColor);
    }

    /**
     * 이미지에서 상위 N개의 대표 색상을 추출합니다.
     *
     * @param image 분석할 이미지
     * @param topN 추출할 색상 개수
     * @return HEX 형식의 색상 목록
     */
    public static List<String> extractTopColors(BufferedImage image, int topN) {
        if (image == null) {
            return List.of();
        }

        Map<Integer, Integer> colorCounts = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y += DEFAULT_SAMPLE_RATE) {
            for (int x = 0; x < image.getWidth(); x += DEFAULT_SAMPLE_RATE) {
                int rgb = image.getRGB(x, y);
                int quantizedRgb = quantizeColor(rgb, DEFAULT_QUANTIZATION_LEVEL);
                colorCounts.merge(quantizedRgb, 1, Integer::sum);
            }
        }

        return colorCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> rgbToHex(entry.getKey()))
                .toList();
    }

    /**
     * 색상을 양자화하여 유사한 색상을 그룹화합니다.
     *
     * @param rgb RGB 값
     * @param level 양자화 레벨
     * @return 양자화된 RGB 값
     */
    private static int quantizeColor(int rgb, int level) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        r = (r / level) * level;
        g = (g / level) * level;
        b = (b / level) * level;

        return (r << 16) | (g << 8) | b;
    }

    /**
     * RGB 값을 HEX 문자열로 변환합니다.
     *
     * @param rgb RGB 값
     * @return HEX 문자열 (예: #FF5733)
     */
    public static String rgbToHex(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * HEX 문자열을 RGB 값으로 변환합니다.
     *
     * @param hex HEX 문자열 (예: #FF5733)
     * @return RGB 값
     */
    public static int hexToRgb(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0;
        }
        String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(cleanHex, 16);
    }
}
