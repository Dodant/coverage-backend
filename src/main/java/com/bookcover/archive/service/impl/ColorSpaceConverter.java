package com.bookcover.archive.service.impl;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

/**
 * 이미지 색공간 변환 유틸리티 클래스.
 * 다양한 색공간의 이미지를 표준 sRGB로 변환합니다.
 *
 * Requirements: 14.7 - 업로드된 이미지를 표준 색공간(sRGB)으로 변환하여 저장
 */
public class ColorSpaceConverter {

    private static final ColorSpace SRGB_COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_sRGB);

    /**
     * 이미지를 sRGB 색공간으로 변환합니다.
     * 이미 sRGB인 경우 원본을 반환합니다.
     *
     * @param image 변환할 이미지
     * @return sRGB 색공간의 이미지
     */
    public static BufferedImage convertToSRGB(BufferedImage image) {
        if (image == null) {
            return null;
        }

        // Check if already sRGB
        if (isSRGB(image)) {
            return image;
        }

        // Convert to sRGB
        ColorConvertOp colorConvertOp = new ColorConvertOp(SRGB_COLOR_SPACE, null);
        BufferedImage srgbImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        colorConvertOp.filter(image, srgbImage);
        return srgbImage;
    }

    /**
     * 이미지가 sRGB 색공간인지 확인합니다.
     *
     * @param image 확인할 이미지
     * @return sRGB인 경우 true
     */
    public static boolean isSRGB(BufferedImage image) {
        if (image == null) {
            return false;
        }
        ColorSpace colorSpace = image.getColorModel().getColorSpace();
        return colorSpace.isCS_sRGB();
    }

    /**
     * 이미지의 색공간 이름을 반환합니다.
     *
     * @param image 확인할 이미지
     * @return 색공간 이름
     */
    public static String getColorSpaceName(BufferedImage image) {
        if (image == null) {
            return "Unknown";
        }
        ColorSpace colorSpace = image.getColorModel().getColorSpace();
        int type = colorSpace.getType();
        return switch (type) {
            case ColorSpace.TYPE_RGB -> "RGB";
            case ColorSpace.TYPE_GRAY -> "Grayscale";
            case ColorSpace.TYPE_CMYK -> "CMYK";
            case ColorSpace.TYPE_XYZ -> "XYZ";
            case ColorSpace.TYPE_Lab -> "Lab";
            default -> "Unknown (" + type + ")";
        };
    }

    /**
     * CMYK 이미지를 sRGB로 변환합니다.
     * CMYK 이미지는 인쇄용으로 사용되며, 웹 표시를 위해 RGB 변환이 필요합니다.
     *
     * @param image CMYK 이미지
     * @return sRGB 이미지
     */
    public static BufferedImage convertCMYKToSRGB(BufferedImage image) {
        if (image == null) {
            return null;
        }

        ColorSpace cmykColorSpace = image.getColorModel().getColorSpace();
        if (cmykColorSpace.getType() != ColorSpace.TYPE_CMYK) {
            // Not CMYK, use standard conversion
            return convertToSRGB(image);
        }

        // Convert CMYK to sRGB
        ColorConvertOp colorConvertOp = new ColorConvertOp(
                cmykColorSpace,
                SRGB_COLOR_SPACE,
                null
        );
        BufferedImage srgbImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        colorConvertOp.filter(image, srgbImage);
        return srgbImage;
    }
}
