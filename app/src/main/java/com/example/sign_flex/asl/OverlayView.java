package com.example.sign_flex.asl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class OverlayView extends View {
    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();
    private final RectF regionOfInterest = new RectF();
    private String prediction = "";

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60);
        textPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
        invalidate();
    }

    public void setRegionOfInterest(int left, int top, int right, int bottom) {
        regionOfInterest.set(left, top, right, bottom);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw region of interest box
        if (regionOfInterest.width() > 0 && regionOfInterest.height() > 0) {
            canvas.drawRect(regionOfInterest, paint);
        }

        // Draw prediction text
        if (!prediction.isEmpty()) {
            canvas.drawText(prediction, 50, 100, textPaint);
        }
    }
}
