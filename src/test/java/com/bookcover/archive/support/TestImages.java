package com.bookcover.archive.support;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/** 큰 래스터를 메모리에 만들지 않고 유효한 단색 PNG 테스트 파일을 만든다. */
public final class TestImages {
    private TestImages() {}

    public static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) {
            byte[] row = new byte[width * 3 + 1];
            for (int y = 0; y < height; y++) deflater.write(row);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        DataOutputStream header = new DataOutputStream(headerBytes);
        header.writeInt(width); header.writeInt(height);
        header.write(new byte[]{8, 2, 0, 0, 0});
        chunk(out, "IHDR", headerBytes.toByteArray());
        chunk(out, "IDAT", compressed.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return bytes.toByteArray();
    }

    private static void chunk(DataOutputStream out, String name, byte[] data) throws IOException {
        byte[] type = name.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32(); crc.update(type); crc.update(data);
        out.writeInt(data.length); out.write(type); out.write(data); out.writeInt((int) crc.getValue());
    }
}
