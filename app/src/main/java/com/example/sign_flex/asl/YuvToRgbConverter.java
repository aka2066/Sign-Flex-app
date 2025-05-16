package com.example.sign_flex.asl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class YuvToRgbConverter {
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType;
    private Type.Builder rgbaType;

    public YuvToRgbConverter(Context context) {
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public Bitmap convert(ImageProxy image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            Log.e("YuvConverter", "❌ Unsupported image format: " + image.getFormat());
            return null;
        }

        try {
            // Get the YUV data
            ByteBuffer yuvBuffer = YuvToRgbConverterUtils.imageToByteBuffer(image);
            
            // Create output bitmap
            Bitmap outputBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            
            // Ensure our Types are initialized
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs))
                        .setX(yuvBuffer.capacity());
            }
            
            if (rgbaType == null) {
                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
                        .setX(image.getWidth())
                        .setY(image.getHeight());
            }
            
            // Prepare the conversion
            Allocation inputAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            Allocation outputAllocation = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            
            // Copy data to allocation
            inputAllocation.copyFrom(yuvBuffer.array());
            
            // Convert YUV to RGB
            yuvToRgbIntrinsic.setInput(inputAllocation);
            yuvToRgbIntrinsic.forEach(outputAllocation);
            
            // Copy the output to the bitmap
            outputAllocation.copyTo(outputBitmap);
            
            // Clean up
            inputAllocation.destroy();
            outputAllocation.destroy();
            
            return outputBitmap;
        } catch (Exception e) {
            Log.e("YuvConverter", "❌ Error converting YUV to RGB: " + e.getMessage());
            return null;
        }
    }
}
