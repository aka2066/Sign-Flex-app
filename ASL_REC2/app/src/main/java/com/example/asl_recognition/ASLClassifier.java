package com.example.asl_recognition;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ASLClassifier {
    private static final String TAG = "ASLClassifier";
    private static final String MODEL_PATH = "asl_model_mobilenetv2.tflite"; // ✅ Make sure it's in /assets/
    private static final int IMAGE_SIZE = 224;
    private static final float CONFIDENCE_THRESHOLD = 0.1f;

    private Interpreter tflite;

    // ✅ 24 valid ASL letters (no J, no Z)
    private final String[] labels = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S",
            "T", "U", "V", "W", "X", "Y"
    };

    public ASLClassifier(AssetManager assetManager) {
        try {
            Log.d(TAG, "🔍 Loading model: " + MODEL_PATH);
            tflite = new Interpreter(loadModelFile(assetManager));
            Log.d(TAG, "✅ Model successfully loaded!");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error loading model: " + e.getMessage());
        }
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public String classifyImage(Bitmap bitmap) {
        Log.d(TAG, "📸 Running image through model...");

        ByteBuffer inputBuffer = preprocessImage(bitmap);
        float[][] output = new float[1][labels.length]; // 24 output classes

        tflite.run(inputBuffer, output);

        int predictedIndex = getMaxIndex(output[0]);
        float confidence = output[0][predictedIndex];

        // ✅ Log all confidence scores for transparency
        Log.d(TAG, "📊 Confidence Scores:");
        for (int i = 0; i < output[0].length; i++) {
            Log.d(TAG, String.format("%s → %.2f%%", labels[i], output[0][i] * 100));
        }

        // ✅ (Optional) Check if probabilities sum close to 1.0
        float sum = 0f;
        for (float val : output[0]) sum += val;
        Log.d(TAG, String.format("🔎 Confidence sum check: %.2f", sum));

        if (confidence < CONFIDENCE_THRESHOLD) {
            Log.w(TAG, "⚠️ Low confidence (" + confidence + ") - using fallback prediction.");
            return getFallbackPrediction(output[0]);
        }

        Log.i(TAG, "✅ Predicted Letter: " + labels[predictedIndex] + " (Confidence: " + (confidence * 100) + "%)");
        return labels[predictedIndex];
    }

    private ByteBuffer preprocessImage(Bitmap bitmap) {
        Log.d(TAG, "🧪 Preprocessing image...");
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3); // 3 channels (RGB)
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];
        resizedBitmap.getPixels(intValues, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int pixelValue : intValues) {
            float red = ((pixelValue >> 16) & 0xFF) / 255.0f;
            float green = ((pixelValue >> 8) & 0xFF) / 255.0f;
            float blue = (pixelValue & 0xFF) / 255.0f;

            byteBuffer.putFloat(red);
            byteBuffer.putFloat(green);
            byteBuffer.putFloat(blue);
        }
        // Log the first 10 pixel values (for debugging purposes)
        Log.d(TAG, "Normalized pixel values (first 10 pixels):");
        for (int i = 0; i < 10; i++) {
            Log.d(TAG, "Pixel " + i + ": " + byteBuffer.getFloat(i * 4));  // Log the first 10 pixel values
        }


        Log.d(TAG, "✅ Image ready for inference.");
        return byteBuffer;
    }

    private int getMaxIndex(float[] probabilities) {
        int maxIndex = 0;
        float maxConfidence = probabilities[0];

        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxConfidence) {
                maxConfidence = probabilities[i];
                maxIndex = i;
            }
        }

        Log.d(TAG, "🏁 Max confidence index: " + maxIndex + " → " + labels[maxIndex]);
        return maxIndex;
    }

    private String getFallbackPrediction(float[] probabilities) {
        int fallbackIndex = getMaxIndex(probabilities);
        float fallbackConfidence = probabilities[fallbackIndex];

        return fallbackConfidence > 0.3f ? labels[fallbackIndex] : "Unknown";
    }
}

