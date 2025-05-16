package com.example.asl_recognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

@OptIn(markerClass = ExperimentalGetImage.class) // ✅ Required to access imageProxy.getImage()
public class YuvToRgbConverter {

    public YuvToRgbConverter(Context context) {
        // Context passed for compatibility or future use.
    }

    public Bitmap convert(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            Log.e("YuvToRgbConverter", "❌ Image from ImageProxy is null");
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        YuvToRgbConverterUtils.yuv420ToBitmap(imageProxy, bitmap);

        return bitmap;
    }
}
