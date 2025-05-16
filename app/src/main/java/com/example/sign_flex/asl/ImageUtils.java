package com.example.sign_flex.asl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Utility class for image processing and drawing
 */
public class ImageUtils {

    /**
     * Draw a detection box on an image
     * @param bitmap Bitmap to draw on
     * @param rect Rectangle area to draw
     * @param label Label text to display
     * @return Modified bitmap with box and label
     */
    public static Bitmap drawDetectionBox(Bitmap bitmap, Rect rect, String label) {
        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);
        
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5);
        
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);
        
        // Draw bounding box
        canvas.drawRect(rect, boxPaint);
        
        // Draw label
        canvas.drawText(label, rect.left, rect.top - 10, textPaint);
        
        return outputBitmap;
    }
    
    /**
     * Add text overlay to an image
     * @param bitmap Bitmap to add text to
     * @param text Text to add
     * @param position Position of text (1-4: top-left, top-right, bottom-left, bottom-right)
     * @return Modified bitmap with text overlay
     */
    public static Bitmap addTextOverlay(Bitmap bitmap, String text, int position) {
        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);
        
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);
        
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        
        int x, y;
        
        switch (position) {
            case 1: // Top-left
                x = 20;
                y = bounds.height() + 20;
                break;
            case 2: // Top-right
                x = bitmap.getWidth() - bounds.width() - 20;
                y = bounds.height() + 20;
                break;
            case 3: // Bottom-left
                x = 20;
                y = bitmap.getHeight() - 20;
                break;
            case 4: // Bottom-right
            default:
                x = bitmap.getWidth() - bounds.width() - 20;
                y = bitmap.getHeight() - 20;
                break;
        }
        
        canvas.drawText(text, x, y, textPaint);
        
        return outputBitmap;
    }
}
