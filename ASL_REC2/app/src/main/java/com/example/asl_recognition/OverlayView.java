package com.example.asl_recognition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class OverlayView extends View {
    private Paint paint;

    public OverlayView(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw a centered guide box
        int boxSize = Math.min(width, height) / 2;
        int left = (width - boxSize) / 2;
        int top = (height - boxSize) / 2;

        canvas.drawRect(left, top, left + boxSize, top + boxSize, paint);

        // Optional crosshair
        canvas.drawLine(width / 2, 0, width / 2, height, paint);
        canvas.drawLine(0, height / 2, width, height / 2, paint);
    }
}
