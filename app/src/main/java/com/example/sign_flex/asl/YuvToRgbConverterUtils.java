package com.example.sign_flex.asl;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class YuvToRgbConverterUtils {
    private static final String TAG = "YuvToRgbConverterUtils";

    public static ByteBuffer imageToByteBuffer(ImageProxy image) {
        final ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        final ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        final ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        final int ySize = yBuffer.remaining();
        final int uSize = uBuffer.remaining();
        final int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Copy Y
        yBuffer.get(nv21, 0, ySize);

        // Copy VU (NV21 format expects VU, not UV)
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return ByteBuffer.wrap(nv21);
    }

    public static byte[] YUV_420_888toNV21(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;

        byte[] nv21 = new byte[ySize + uvSize * 2];

        // Get the Y plane
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        int yRowStride = image.getPlanes()[0].getRowStride();
        int yPixelStride = image.getPlanes()[0].getPixelStride();

        // Get U and V planes
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        int uRowStride = image.getPlanes()[1].getRowStride();
        int uPixelStride = image.getPlanes()[1].getPixelStride();
        int vRowStride = image.getPlanes()[2].getRowStride();
        int vPixelStride = image.getPlanes()[2].getPixelStride();

        // Copy Y plane
        if (yPixelStride == 1) {
            // Fast path for contiguous Y plane
            for (int row = 0; row < height; row++) {
                yBuffer.position(row * yRowStride);
                yBuffer.get(nv21, row * width, width);
            }
        } else {
            // Slow path for non-contiguous Y plane
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    nv21[row * width + col] = yBuffer.get(row * yRowStride + col * yPixelStride);
                }
            }
        }

        // Copy U and V planes
        int uvPos = ySize;
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                nv21[uvPos++] = vBuffer.get(row * vRowStride + col * vPixelStride);
                nv21[uvPos++] = uBuffer.get(row * uRowStride + col * uPixelStride);
            }
        }

        return nv21;
    }
}
