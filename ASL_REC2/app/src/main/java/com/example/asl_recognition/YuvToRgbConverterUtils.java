package com.example.asl_recognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

@ExperimentalGetImage  // ✅ Required to use imageProxy.getImage()
public class YuvToRgbConverterUtils {

    public static void yuv420ToBitmap(ImageProxy imageProxy, Bitmap output) {
        Image image = imageProxy.getImage();
        if (image == null) {
            Log.e("YuvToRgbUtils", "❌ imageProxy.getImage() returned null");
            return;
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        try {
            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    image.getWidth(),
                    image.getHeight(),
                    null
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(
                    new Rect(0, 0, image.getWidth(), image.getHeight()),
                    100,
                    out
            );

            byte[] jpegBytes = out.toByteArray();
            Bitmap tempBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);

            Matrix matrix = new Matrix();
            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());

            Bitmap rotatedBitmap = Bitmap.createBitmap(tempBitmap, 0, 0,
                    tempBitmap.getWidth(), tempBitmap.getHeight(), matrix, true);

            output.eraseColor(0); // Clear
            new Canvas(output).drawBitmap(rotatedBitmap, 0, 0, null);

        } catch (Exception e) {
            Log.e("YuvToRgbUtils", "❌ Failed to convert YUV to RGB", e);
        }
    }
}
