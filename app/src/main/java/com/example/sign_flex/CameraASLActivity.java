package com.example.sign_flex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraASLActivity extends AppCompatActivity {
    private static final String TAG = "CameraASLActivity";
    private PreviewView previewView;
    private ASLClassifier aslClassifier;
    private TextView predictionText;
    private LinearLayout historyContainer;
    private Button backButton;
    private Button switchModeButton;
    private ExecutorService cameraExecutor;
    
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int FRAME_SKIP = 10;
    private int frameCount = 0;
    
    // For stable predictions
    private String lastPrediction = "";
    private int stableCounter = 0;
    private static final int STABILITY_THRESHOLD = 3;
    
    // For history display
    private Queue<String> recognitionHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_asl);
        
        previewView = findViewById(R.id.previewView);
        predictionText = findViewById(R.id.predictionText);
        historyContainer = findViewById(R.id.historyContainer);
        backButton = findViewById(R.id.backButton);
        switchModeButton = findViewById(R.id.switchModeButton);
        
        // Initialize ASL classifier
        aslClassifier = new ASLClassifier(getAssets());
        
        // Set up button listeners
        backButton.setOnClickListener(v -> finish());
        switchModeButton.setOnClickListener(v -> {
            finish();
            // Start the sensor-based ASL activity if it exists
            if (MainActivity.getInstance() != null) {
                MainActivity.getInstance().launchASLRecognitionActivity();
            }
        });
        
        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                
                imageAnalysis.setAnalyzer(cameraExecutor, this::processImage);
                
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing camera", e);
                Toast.makeText(this, "Error initializing camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void processImage(ImageProxy image) {
        frameCount++;
        if (frameCount % FRAME_SKIP != 0) {
            image.close();
            return;
        }
        
        try {
            Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
            if (bitmap == null) {
                Log.e(TAG, "Null bitmap received, skipping frame.");
                image.close();
                return;
            }
            
            // Process the image with native Android processing
            Bitmap processedBitmap = preprocessImage(bitmap);
            if (processedBitmap == null) {
                Log.e(TAG, "Null processed bitmap received, skipping frame.");
                image.close();
                return;
            }
            
            // Classify the image
            String currentPrediction = aslClassifier.classifyImage(processedBitmap);
            
            // Implement prediction stability
            if (currentPrediction.equals(lastPrediction)) {
                stableCounter++;
            } else {
                stableCounter = 0;
            }
            
            if (stableCounter >= STABILITY_THRESHOLD) {
                final String stablePrediction = currentPrediction;
                runOnUiThread(() -> {
                    predictionText.setText("Predicted: " + stablePrediction);
                    
                    // Add to history if it's a new prediction
                    if (!recognitionHistory.contains(stablePrediction) && !stablePrediction.equals("Unknown")) {
                        // Add to history queue
                        recognitionHistory.add(stablePrediction);
                        if (recognitionHistory.size() > MAX_HISTORY_SIZE) {
                            recognitionHistory.poll(); // Remove oldest
                        }
                        
                        // Update the history display
                        updateHistoryDisplay();
                    }
                });
            }
            
            lastPrediction = currentPrediction;
        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage(), e);
        } finally {
            image.close();
        }
    }
    
    private Bitmap preprocessImage(Bitmap bitmap) {
        try {
            // Create a mutable bitmap to work with
            Bitmap processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            // Step 1: Convert to grayscale
            Canvas canvas = new Canvas(processedBitmap);
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0); // Remove color saturation (make grayscale)
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            canvas.drawBitmap(processedBitmap, 0, 0, paint);
            
            // Step 2: Increase contrast (simple algorithm)
            int width = processedBitmap.getWidth();
            int height = processedBitmap.getHeight();
            int[] pixels = new int[width * height];
            processedBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            // Enhance contrast
            double contrast = 1.5; // Contrast factor (1.0 = no change)
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                
                // Apply contrast formula
                r = (int)(((r / 255.0 - 0.5) * contrast + 0.5) * 255.0);
                g = (int)(((g / 255.0 - 0.5) * contrast + 0.5) * 255.0);
                b = (int)(((b / 255.0 - 0.5) * contrast + 0.5) * 255.0);
                
                // Clamp values
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                
                pixels[i] = Color.rgb(r, g, b);
            }
            
            processedBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            
            return processedBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing image: " + e.getMessage(), e);
            return null;
        }
    }
    
    private void updateHistoryDisplay() {
        historyContainer.removeAllViews();
        for (String sign : recognitionHistory) {
            TextView textView = new TextView(this);
            textView.setText(sign);
            textView.setTextSize(16);
            textView.setPadding(0, 8, 0, 8);
            historyContainer.addView(textView);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for ASL recognition", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
