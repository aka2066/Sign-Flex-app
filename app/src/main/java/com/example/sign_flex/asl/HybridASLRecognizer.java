package com.example.sign_flex.asl;

import android.graphics.Bitmap;
import android.util.Log;

import com.example.sign_flex.MainActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * A hybrid ASL recognizer that combines camera-based recognition with flex sensor data
 * when available from the ESP32 device. This provides more accurate sign language
 * recognition by fusing both visual and physical sensor inputs.
 */
public class HybridASLRecognizer {
    private static final String TAG = "HybridASLRecognizer";
    
    // Camera-based ASL classifier
    private final ASLClassifier cameraClassifier;
    
    // Weight for camera vs sensor decision (0.0-1.0)
    // Higher means more camera influence, lower means more sensor influence
    private float cameraWeight = 0.6f;
    
    // Flex sensor thresholds for different letters
    private final Map<String, int[]> letterFlexPatterns;
    
    // Latest flex sensor values
    private int[] currentFlexValues = new int[5];
    private boolean flexSensorConnected = false;
    
    // Confidence threshold for camera recognition
    private static final float CAMERA_CONFIDENCE_THRESHOLD = 0.7f;
    
    public HybridASLRecognizer(ASLClassifier cameraClassifier) {
        this.cameraClassifier = cameraClassifier;
        this.letterFlexPatterns = initializeFlexPatterns();
        
        // Try to get a reference to MainActivity for sensor data
        try {
            MainActivity mainActivity = MainActivity.getInstance();
            if (mainActivity != null) {
                mainActivity.setSerialDataListener(this::onFlexSensorDataReceived);
                flexSensorConnected = true;
                Log.d(TAG, "Successfully connected to flex sensor data stream");
            } else {
                Log.d(TAG, "Unable to get MainActivity instance, running in camera-only mode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to flex sensors: " + e.getMessage());
            Log.d(TAG, "Running in camera-only mode");
        }
    }
    
    /**
     * Initialize the flex sensor patterns for various ASL letters
     * These are approximate patterns based on typical finger positions
     * Format: [thumb, index, middle, ring, pinky]
     * Where higher values = more bent/flexed
     */
    private Map<String, int[]> initializeFlexPatterns() {
        Map<String, int[]> patterns = new HashMap<>();
        
        // Add patterns for common ASL letters
        // These are simplified examples - in a real app, these would be calibrated
        patterns.put("A", new int[]{40, 90, 90, 90, 90});  // Fist with thumb out slightly
        patterns.put("B", new int[]{90, 10, 10, 10, 10});  // Flat hand, thumb tucked
        patterns.put("C", new int[]{50, 50, 50, 50, 50});  // Curved hand like a C
        patterns.put("D", new int[]{70, 10, 90, 90, 90});  // Index up, others curled
        patterns.put("E", new int[]{90, 70, 70, 70, 70});  // Curled fingers, thumb against palm
        patterns.put("F", new int[]{30, 80, 10, 10, 10});  // Thumb and index touch, others extended
        
        return patterns;
    }
    
    /**
     * Process flex sensor data received from the ESP32
     * @param rawData Raw sensor string from ESP32
     */
    public void onFlexSensorDataReceived(String rawData) {
        try {
            if (rawData == null || rawData.isEmpty()) {
                return;
            }
            
            // Example expected format: "F1:42,F2:18,F3:95,F4:23,F5:56"
            String[] parts = rawData.split(",");
            if (parts.length >= 5) {
                for (int i = 0; i < 5 && i < parts.length; i++) {
                    String[] valuePart = parts[i].split(":");
                    if (valuePart.length == 2) {
                        currentFlexValues[i] = Integer.parseInt(valuePart[1].trim());
                    }
                }
                Log.d(TAG, "Updated flex values: " + 
                      currentFlexValues[0] + "," + 
                      currentFlexValues[1] + "," + 
                      currentFlexValues[2] + "," + 
                      currentFlexValues[3] + "," + 
                      currentFlexValues[4]);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing flex sensor data: " + e.getMessage());
        }
    }
    
    /**
     * Recognize an ASL letter from camera image and flex sensor data
     * @param bitmap Camera image
     * @return Predicted ASL letter
     */
    public RecognitionResult recognizeSign(Bitmap bitmap) {
        // Get camera-based prediction
        String cameraPrediction = cameraClassifier.classifyImage(bitmap);
        
        // If sensors aren't connected, just use camera
        if (!flexSensorConnected || !isValidFlexData()) {
            return new RecognitionResult(cameraPrediction, "camera", 1.0f);
        }
        
        // Get sensor-based prediction
        String sensorPrediction = predictFromFlexSensors();
        
        // If camera and sensor agree, high confidence!
        if (cameraPrediction.equals(sensorPrediction)) {
            return new RecognitionResult(cameraPrediction, "hybrid", 0.95f);
        }
        
        // If they disagree, use weighted decision
        if (cameraWeight >= 0.5f) {
            return new RecognitionResult(cameraPrediction, "hybrid_camera", cameraWeight);
        } else {
            return new RecognitionResult(sensorPrediction, "hybrid_sensor", 1.0f - cameraWeight);
        }
    }
    
    /**
     * Check if we have valid flex sensor data
     */
    private boolean isValidFlexData() {
        int sum = 0;
        for (int val : currentFlexValues) {
            sum += val;
        }
        // If all values are 0, likely invalid data
        return sum > 0;
    }
    
    /**
     * Predict ASL letter from flex sensor data by comparing
     * to known patterns and finding closest match
     */
    private String predictFromFlexSensors() {
        String bestMatch = "Unknown";
        double bestDistance = Double.MAX_VALUE;
        
        for (Map.Entry<String, int[]> entry : letterFlexPatterns.entrySet()) {
            double distance = calculateDistance(currentFlexValues, entry.getValue());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = entry.getKey();
            }
        }
        
        Log.d(TAG, "Flex sensor prediction: " + bestMatch + " (distance: " + bestDistance + ")");
        return bestDistance < 100 ? bestMatch : "Unknown"; // Threshold for valid match
    }
    
    /**
     * Calculate Euclidean distance between two sensor patterns
     */
    private double calculateDistance(int[] a, int[] b) {
        double sum = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Set the weight for camera vs sensor fusion
     * 1.0 = 100% camera, 0.0 = 100% sensors
     */
    public void setCameraWeight(float weight) {
        this.cameraWeight = Math.max(0.0f, Math.min(1.0f, weight));
    }
    
    /**
     * Get the connection status of flex sensors
     */
    public boolean isFlexSensorConnected() {
        return flexSensorConnected;
    }
    
    /**
     * Result class for ASL recognition
     */
    public static class RecognitionResult {
        private final String letter;
        private final String source;
        private final float confidence;
        
        public RecognitionResult(String letter, String source, float confidence) {
            this.letter = letter;
            this.source = source;
            this.confidence = confidence;
        }
        
        public String getLetter() {
            return letter;
        }
        
        public String getSource() {
            return source;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s, %.1f%%)", letter, source, confidence * 100);
        }
    }
}
