package com.example.sign_flex;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * ASLGestureRecognizer - Processes flex sensor and accelerometer data to recognize ASL gestures
 * This class implements a simple threshold-based gesture recognition algorithm for the ASL alphabet
 */
public class ASLGestureRecognizer {
    private static final String TAG = "ASLGestureRecognizer";
    
    // Constants for gesture recognition
    private static final float FLEX_THRESHOLD_STRAIGHT = 0.3f;
    private static final float FLEX_THRESHOLD_PARTIAL = 0.6f;
    private static final float FLEX_THRESHOLD_BENT = 0.8f;
    
    // Maps for storing gesture templates
    private final Map<String, GestureTemplate> gestureTemplates;
    
    // Last recognized gesture
    private String currentGesture = "";
    private long lastGestureTime = 0;
    private static final long DEBOUNCE_TIME = 500; // ms
    
    public ASLGestureRecognizer() {
        gestureTemplates = new HashMap<>();
        initializeGestureTemplates();
    }
    
    /**
     * Initialize predefined gesture templates for ASL letters
     */
    private void initializeGestureTemplates() {
        // A - Fist with thumb out to the side
        gestureTemplates.put("A", new GestureTemplate(
                new float[]{0.2f, 0.9f, 0.9f, 0.9f, 0.9f}, // thumb straight, others bent
                new float[]{-0.5f, 0.5f, 0.0f}, // palm facing left
                "A"
        ));
        
        // B - Fingers straight up, thumb across palm
        gestureTemplates.put("B", new GestureTemplate(
                new float[]{0.8f, 0.2f, 0.2f, 0.2f, 0.2f}, // thumb bent, others straight
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "B"
        ));
        
        // C - Curved hand
        gestureTemplates.put("C", new GestureTemplate(
                new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f}, // all fingers partially bent
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "C"
        ));
        
        // D - Index pointing up, others curled
        gestureTemplates.put("D", new GestureTemplate(
                new float[]{0.8f, 0.2f, 0.8f, 0.8f, 0.8f}, // index straight, others bent
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "D"
        ));
        
        // E - Fingers curled, thumb across fingers
        gestureTemplates.put("E", new GestureTemplate(
                new float[]{0.7f, 0.7f, 0.7f, 0.7f, 0.7f}, // all fingers partially bent
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "E"
        ));
        
        // Add more gesture templates for other letters...
        // F, G, H, I, etc.
        
        // I - Pinky extended, others closed
        gestureTemplates.put("I", new GestureTemplate(
                new float[]{0.8f, 0.8f, 0.8f, 0.8f, 0.2f}, // pinky straight, others bent
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "I"
        ));
        
        // L - L shape with thumb and index
        gestureTemplates.put("L", new GestureTemplate(
                new float[]{0.2f, 0.2f, 0.8f, 0.8f, 0.8f}, // thumb and index straight, others bent
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "L"
        ));
        
        // O - O shape with fingers
        gestureTemplates.put("O", new GestureTemplate(
                new float[]{0.6f, 0.6f, 0.6f, 0.6f, 0.6f}, // all fingers moderately bent
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "O"
        ));
        
        // Y - Thumb and pinky extended
        gestureTemplates.put("Y", new GestureTemplate(
                new float[]{0.2f, 0.8f, 0.8f, 0.8f, 0.2f}, // thumb and pinky straight, others bent
                new float[]{0.0f, 0.8f, 0.0f}, // palm facing forward
                "Y"
        ));
    }
    
    /**
     * Recognize gesture from sensor data
     * @param flexValues Array of 5 flex sensor values (0.0-1.0, normalized)
     * @param accelValues Array of 3 accelerometer values (x,y,z)
     * @return The recognized ASL letter, or empty string if no match
     */
    public String recognizeGesture(float[] flexValues, float[] accelValues) {
        if (flexValues == null || accelValues == null || 
            flexValues.length != 5 || accelValues.length != 3) {
            return "";
        }
        
        // Debounce to prevent rapid gesture changes
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGestureTime < DEBOUNCE_TIME) {
            return currentGesture;
        }
        
        // Find the best matching gesture template
        String bestMatch = "";
        float bestScore = 0.4f; // Minimum threshold to consider a match
        
        for (Map.Entry<String, GestureTemplate> entry : gestureTemplates.entrySet()) {
            GestureTemplate template = entry.getValue();
            float score = template.calculateMatchScore(flexValues, accelValues);
            
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry.getKey();
            }
        }
        
        if (!bestMatch.isEmpty()) {
            currentGesture = bestMatch;
            lastGestureTime = currentTime;
            Log.d(TAG, "Recognized gesture: " + bestMatch + " with score: " + bestScore);
        }
        
        return currentGesture;
    }
    
    /**
     * Inner class to represent a gesture template
     */
    private static class GestureTemplate {
        private final float[] flexTemplate;
        private final float[] accelTemplate;
        private final String name;
        
        public GestureTemplate(float[] flexTemplate, float[] accelTemplate, String name) {
            this.flexTemplate = flexTemplate;
            this.accelTemplate = accelTemplate;
            this.name = name;
        }
        
        /**
         * Calculate how well the input values match this template
         * @param flexValues Flex sensor values
         * @param accelValues Accelerometer values
         * @return Score between 0.0 (no match) and 1.0 (perfect match)
         */
        public float calculateMatchScore(float[] flexValues, float[] accelValues) {
            // Calculate flex score (80% of total)
            float flexScore = 0;
            for (int i = 0; i < 5; i++) {
                float diff = Math.abs(flexTemplate[i] - flexValues[i]);
                flexScore += (1.0f - Math.min(diff, 1.0f)) / 5.0f;
            }
            
            // Calculate accelerometer score (20% of total)
            float accelScore = 0;
            for (int i = 0; i < 3; i++) {
                float diff = Math.abs(accelTemplate[i] - accelValues[i]);
                accelScore += (1.0f - Math.min(diff, 1.0f)) / 3.0f;
            }
            
            // Combined weighted score
            return (flexScore * 0.8f) + (accelScore * 0.2f);
        }
    }
}
