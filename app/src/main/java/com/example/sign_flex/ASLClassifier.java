package com.example.sign_flex;

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
    private static final String MODEL_PATH = "best_asl_model.tflite"; // TFLite model path
    private static final int IMAGE_SIZE = 224;
    private Interpreter tflite;
    private final String[] labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");

    /**
     * Loads the TensorFlow Lite model from the assets folder.
     * Initializes the TFLite Interpreter to run predictions.
     */
    public ASLClassifier(AssetManager assetManager) {
        try {
            Log.d(TAG, "Loading model: " + MODEL_PATH);
            tflite = new Interpreter(loadModelFile(assetManager));
            Log.d(TAG, "Model successfully loaded!");
        } catch (IOException e) {
            Log.e(TAG, "Error loading model: " + e.getMessage());
        }
    }

    /**
     * Loads the TensorFlow Lite model file from assets
     */
    private MappedByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Takes the preprocessed image and runs it through the ASL model.
     * The model returns probabilities for each letter (A-Z).
     * Finds the letter with the highest confidence score.
     */
    public String classifyImage(Bitmap bitmap) {
        Log.d(TAG, "Running image through model...");

        ByteBuffer inputBuffer = preprocessImage(bitmap);
        float[][] output = new float[1][26]; // Matches 26 ASL letters

        tflite.run(inputBuffer, output);

        int predictedIndex = getMaxIndex(output[0]);
        float confidence = output[0][predictedIndex];

        // Log All Confidence Scores
        Log.d(TAG, "Confidence Scores: ");
        for (int i = 0; i < output[0].length; i++) {
            Log.d(TAG, labels[i] + ": " + output[0][i]);
        }

        // Ignore low-confidence predictions (prevents bad outputs)
        if (confidence < 0.4) { // Threshold at 0.4 for more reliable results
            Log.d(TAG, "Low confidence (" + confidence + ") - returning fallback prediction.");
            return getFallbackPrediction(output[0]);
        }

        Log.d(TAG, "Predicted Letter: " + labels[predictedIndex] + " (Confidence: " + confidence + ")");
        return labels[predictedIndex];
    }

    /**
     * Resizes the image to 224x224 pixels (to match model input).
     * Normalizes the pixel values (0 to 1 scale).
     * Converts the image to a ByteBuffer (required for TensorFlow Lite).
     */
    private ByteBuffer preprocessImage(Bitmap bitmap) {
        Log.d(TAG, "Preprocessing image...");

        // Resize to 224x224 (Matches model input)
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);

        // Normalize pixel values (rescale=1./255 from training)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];
        resizedBitmap.getPixels(intValues, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int pixelValue : intValues) {
            // Ensure correct normalization (Convert 0-255 to 0-1)
            float red = ((pixelValue >> 16) & 0xFF) / 255.0f;
            float green = ((pixelValue >> 8) & 0xFF) / 255.0f;
            float blue = (pixelValue & 0xFF) / 255.0f;

            byteBuffer.putFloat(red);
            byteBuffer.putFloat(green);
            byteBuffer.putFloat(blue);
        }

        Log.d(TAG, "Image preprocessing completed!");
        return byteBuffer;
    }

    /**
     * Checks all 26 possible ASL letters.
     * Finds the letter with the highest confidence score.
     * Returns the index of the predicted letter.
     */
    private int getMaxIndex(float[] probabilities) {
        int maxIndex = 0;
        float maxConfidence = probabilities[0];

        for (int i = 1; i < probabilities.length; i++) {
            Log.d(TAG, "Letter " + labels[i] + " Confidence: " + probabilities[i]);
            if (probabilities[i] > maxConfidence) {
                maxConfidence = probabilities[i];
                maxIndex = i;
            }
        }

        Log.d(TAG, "Highest confidence class index: " + maxIndex + " (Confidence: " + maxConfidence + ")");
        return maxIndex;
    }

    /**
     * If all letters have low confidence (<30%), it returns "Unknown".
     * Returns a fallback prediction when confidence is low
     */
    private String getFallbackPrediction(float[] probabilities) {
        int fallbackIndex = getMaxIndex(probabilities); // Get the best available guess
        float fallbackConfidence = probabilities[fallbackIndex];

        if (fallbackConfidence > 0.3) { // If any letter has at least 30% confidence, return it
            return labels[fallbackIndex];
        } else {
            return "Unknown"; // If all confidence scores are too low, return "Unknown"
        }
    }
}
