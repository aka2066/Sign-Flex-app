package com.example.sign_flex.asl;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sign_flex.R;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ASLRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "ASLRecognitionActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int FRAME_SKIP = 1;
    private static final int STABILITY_THRESHOLD = 5;

    private PreviewView previewView;
    private TextView predictionText;
    private TextView responseTimeText;
    private Button backButton;

    private ASLClassifier aslClassifier;
    private YuvToRgbConverter yuvToRgbConverter;
    private ExecutorService cameraExecutor;

    private int frameCount = 0;
    private int stableCounter = 0;
    private String lastPrediction = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "Starting ASLRecognitionActivity onCreate");
            setContentView(R.layout.activity_asl_recognition);

            previewView = findViewById(R.id.previewView);
            predictionText = findViewById(R.id.predictionText);
            responseTimeText = findViewById(R.id.responseTimeText);
            backButton = findViewById(R.id.backButton);

            backButton.setOnClickListener(v -> finish());

            // Initialize OpenCV
            boolean openCVLoaded = false;
            try {
                openCVLoaded = OpenCVLoader.initDebug();
                Log.d(TAG, "OpenCV initialization result: " + openCVLoaded);
            } catch (Exception e) {
                Log.e(TAG, "❌ OpenCV initialization exception: " + e.getMessage(), e);
            }
            
            if (!openCVLoaded) {
                Log.e(TAG, "❌ OpenCV initialization failed!");
                Toast.makeText(this, "OpenCV initialization failed! Trying alternative approach...", Toast.LENGTH_LONG).show();
                
                // Try alternative initialization without relying on OpenCV
                initializeWithoutOpenCV();
            } else {
                Log.d(TAG, "✅ OpenCV initialized successfully!");
                initializeAfterOpenCV();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Fatal error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Fatal error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void initializeWithoutOpenCV() {
        try {
            // Initialize without OpenCV
            Log.d(TAG, "Initializing without OpenCV...");
            yuvToRgbConverter = new YuvToRgbConverter(this);
            
            try {
                aslClassifier = new ASLClassifier(getAssets());
                Log.d(TAG, "ASLClassifier initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing ASLClassifier: " + e.getMessage(), e);
                Toast.makeText(this, "Error initializing classifier: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Check camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                startCamera();
            }

            cameraExecutor = Executors.newFixedThreadPool(4);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in initializeWithoutOpenCV: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeAfterOpenCV() {
        try {
            yuvToRgbConverter = new YuvToRgbConverter(this);
            aslClassifier = new ASLClassifier(getAssets());

            // Check for camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                startCamera();
            }

            cameraExecutor = Executors.newFixedThreadPool(4);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in initialization: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void startCamera() {
        try {
            Log.d(TAG, "Starting camera...");
            
            final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                    ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    // Camera provider is now guaranteed to be available
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    // Set up the preview
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    // Choose the back camera
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();

                    // Set up the image analysis
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    imageAnalysis.setAnalyzer(cameraExecutor, this::processImage);

                    // Unbind any bound use cases before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                    
                    Log.d(TAG, "Camera setup complete");

                } catch (Exception e) {
                    Log.e(TAG, "❌ Camera use case binding failed: " + e.getMessage(), e);
                    Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting camera: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to start camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processImage(ImageProxy image) {
        frameCount++;
        if (frameCount % FRAME_SKIP != 0) return;

        Bitmap bitmap = yuvToRgbConverter.convert(image);
        if (bitmap == null) {
            Log.e(TAG, "❌ Null bitmap received, skipping frame.");
            return;
        }

        Bitmap processedBitmap = preprocessImage(bitmap);
        if (processedBitmap == null) {
            Log.e(TAG, "❌ Null processed bitmap received, skipping frame.");
            return;
        }

        long startTime = System.nanoTime();
        String currentPrediction = aslClassifier.classifyImage(processedBitmap);
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        if (currentPrediction.equals(lastPrediction)) {
            stableCounter++;
        } else {
            stableCounter = 0;
        }

        final String predictionToShow = currentPrediction;
        final long finalDurationMs = durationMs;

        if (stableCounter >= STABILITY_THRESHOLD) {
            runOnUiThread(() -> {
                predictionText.setText(String.format("Predicted: %s", predictionToShow));
                responseTimeText.setText(String.format("Response Time: %d ms", finalDurationMs));
            });
        }

        lastPrediction = currentPrediction;
    }

    private Bitmap preprocessImage(Bitmap bitmap) {
        try {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

            Mat mat = new Mat();
            Utils.bitmapToMat(resizedBitmap, mat);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);

            Bitmap processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, processedBitmap);
            mat.release();
            
            return processedBitmap;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error preprocessing image: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.e(TAG, "❌ Camera permission denied");
                Toast.makeText(this, "Camera permission is required for ASL recognition", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
