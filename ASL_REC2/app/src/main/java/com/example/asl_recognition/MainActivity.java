package com.example.asl_recognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int FRAME_SKIP = 1;
    private static final int STABILITY_THRESHOLD = 5;

    private PreviewView previewView;
    private TextView predictionText;
    private TextView responseTimeText;

    private ASLClassifier aslClassifier;
    private YuvToRgbConverter yuvToRgbConverter;
    private ExecutorService cameraExecutor;

    private int frameCount = 0;
    private int stableCounter = 0;
    private String lastPrediction = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        predictionText = findViewById(R.id.predictionText);
        responseTimeText = findViewById(R.id.responseTimeText);

        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "❌ OpenCV initialization failed!");
        } else {
            Log.d("OpenCV", "✅ OpenCV initialized successfully!");
        }

        yuvToRgbConverter = new YuvToRgbConverter(this);
        aslClassifier = new ASLClassifier(getAssets());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }

        cameraExecutor = Executors.newFixedThreadPool(4);
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

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    processImage(image);
                    image.close();
                });

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "❌ Error initializing camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy image) {
        frameCount++;
        if (frameCount % FRAME_SKIP != 0) return;

        Bitmap bitmap = yuvToRgbConverter.convert(image);
        if (bitmap == null) {
            Log.e("Processing", "❌ Null bitmap received, skipping frame.");
            return;
        }

        saveBitmapToInternalStorage(bitmap);

        Bitmap processedBitmap = preprocessImage(bitmap);
        if (processedBitmap == null) {
            Log.e("Processing", "❌ Null processed bitmap received, skipping frame.");
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

        if (stableCounter >= STABILITY_THRESHOLD) {
            runOnUiThread(() -> {
                predictionText.setText(String.format("Predicted: %s", currentPrediction));
                responseTimeText.setText(String.format("Response Time: %d ms", durationMs));
            });
        }

        lastPrediction = currentPrediction;
    }

    private Bitmap preprocessImage(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        Mat mat = new Mat();
        Utils.bitmapToMat(resizedBitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);

        Bitmap processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, processedBitmap);
        mat.release();
        return processedBitmap;
    }

    private void saveBitmapToInternalStorage(Bitmap bitmap) {
        try {
            File debugDir = new File(getFilesDir(), "asl_debug");
            if (!debugDir.exists() && !debugDir.mkdirs()) {
                Log.e("FrameSave", "❌ Failed to create debug directory");
                return;
            }

            File file = new File(debugDir, "frame_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Log.d("FrameSave", "✅ Saved frame to: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e("FrameSave", "❌ Failed to save frame", e);
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
                Log.e("Camera", "❌ Camera permission denied");
            }
        }
    }
}
