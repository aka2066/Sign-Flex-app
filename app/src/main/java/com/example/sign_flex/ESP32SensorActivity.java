package com.example.sign_flex;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ESP32SensorActivity extends AppCompatActivity implements MainActivity.FlexDataListener, MainActivity.SerialDataListener {
    private static final String TAG = "ESP32SensorActivity";
    private static final int MAX_SERIAL_LINES = 100; // Maximum number of lines to keep in the serial monitor
    private static final int MAX_SERIAL_BUFFER = 10000; // Maximum number of characters in the serial buffer
    
    private TextView deviceNameText;
    private TextView connectionStatus;
    private TextView batteryStatus;
    private TextView serialMonitorText;
    private Button backButton;
    private Button calibrateButton;
    
    // List to keep track of sensor UI elements
    private List<SensorDisplay> sensorDisplays = new ArrayList<>();
    
    // Last received sensor values
    private int[] lastSensorValues = new int[5]; // Assuming 5 flex sensors
    
    // StringBuilder to store serial monitor data
    private StringBuilder serialBuffer = new StringBuilder();
    
    // Container for sensor displays
    private LinearLayout sensorContainer;
    
    // Safe reference to MainActivity 
    private MainActivity mainActivity;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp32_sensor);
        
        // Initialize UI components
        deviceNameText = findViewById(R.id.deviceNameText);
        connectionStatus = findViewById(R.id.connectionStatusText);
        batteryStatus = findViewById(R.id.batteryStatusText);
        serialMonitorText = findViewById(R.id.serialMonitorText);
        backButton = findViewById(R.id.backButton);
        calibrateButton = findViewById(R.id.calibrateButton);
        sensorContainer = findViewById(R.id.sensorDataContainer);
        
        setupUI();
        
        // Get MainActivity reference safely
        try {
            mainActivity = MainActivity.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get MainActivity instance: " + e.getMessage());
            addSerialData("ERROR: Failed to connect to main activity");
        }
        
        // Initialize sensor displays
        initializeSensorDisplays();
        
        // Update connection status and device info
        updateConnectionInfo();
        
        // Initialize serial monitor
        addSerialData("ESP32 Serial Monitor initialized");
        
        try {
            if (mainActivity != null && mainActivity.isDeviceConnected()) {
                addSerialData("Connected to " + mainActivity.getConnectedDeviceName());
            } else {
                addSerialData("No device connected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking connection: " + e.getMessage());
            addSerialData("ERROR: Failed to check connection status");
        }
    }
    
    private void setupUI() {
        // Set up the serial monitor text view
        serialMonitorText.setMovementMethod(new ScrollingMovementMethod());
        
        // Set up back button
        backButton.setOnClickListener(v -> finish());
        
        // Set up calibrate button
        calibrateButton.setOnClickListener(v -> {
            try {
                if (mainActivity != null && mainActivity.isDeviceConnected()) {
                    mainActivity.calibrateFlexSensors();
                    Toast.makeText(this, "Calibrating flex sensors...", Toast.LENGTH_SHORT).show();
                    addSerialData("Calibration command sent to ESP32");
                } else {
                    Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show();
                    addSerialData("ERROR: Cannot calibrate - no device connected");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during calibration: " + e.getMessage());
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                addSerialData("ERROR: Calibration failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Get fresh MainActivity reference
        try {
            mainActivity = MainActivity.getInstance();
            
            // Register for sensor data updates if MainActivity is available
            if (mainActivity != null) {
                mainActivity.setFlexDataListener(this);
                mainActivity.setSerialDataListener(this);
                
                // Update connection info in case it changed
                updateConnectionInfo();
            } else {
                Log.w(TAG, "MainActivity instance is null in onResume");
                addSerialData("WARNING: Cannot connect to main activity");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage());
            addSerialData("ERROR: " + e.getMessage());
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister for sensor data updates but don't disconnect
        try {
            if (mainActivity != null) {
                mainActivity.setFlexDataListener(null);
                mainActivity.setSerialDataListener(null);
                // Don't close the BLE connection, just remove listeners
                addSerialData("Pausing activity but maintaining BLE connection");
                Log.d(TAG, "Pausing activity but maintaining BLE connection");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause: " + e.getMessage());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called - maintaining BLE connection for MainActivity");
        
        // We intentionally DO NOT close the BLE connection here
        // The connection is managed by MainActivity
    }
    
    /**
     * Initialize UI elements for each sensor
     */
    private void initializeSensorDisplays() {
        sensorDisplays.clear();
        
        // Create displays for each sensor (assuming 5 flex sensors)
        String[] sensorNames = {"Thumb", "Index", "Middle", "Ring", "Pinky"};
        int[] colors = {
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#9C27B0")  // Purple
        };
        
        for (int i = 0; i < 5; i++) {
            SensorDisplay display = new SensorDisplay(sensorNames[i], colors[i]);
            sensorDisplays.add(display);
        }
    }
    
    /**
     * Update connection status and device information
     */
    private void updateConnectionInfo() {
        try {
            if (mainActivity != null) {
                boolean connected = mainActivity.isDeviceConnected();
                String deviceName = connected ? mainActivity.getConnectedDeviceName() : null;
                int batteryLevel = mainActivity.getBatteryLevel();
                
                if (connected && deviceName != null) {
                    deviceNameText.setText(deviceName);
                    connectionStatus.setText("Connected to " + deviceName);
                    connectionStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                    
                    if (batteryLevel >= 0) {
                        batteryStatus.setText("Battery: " + batteryLevel + "%");
                    } else {
                        batteryStatus.setText("Battery: Unknown");
                    }
                    
                    calibrateButton.setEnabled(true);
                } else {
                    deviceNameText.setText("No Device");
                    connectionStatus.setText("Disconnected");
                    connectionStatus.setTextColor(Color.parseColor("#F44336")); // Red
                    batteryStatus.setText("Battery: N/A");
                    calibrateButton.setEnabled(false);
                }
            } else {
                deviceNameText.setText("No Device");
                connectionStatus.setText("Disconnected");
                connectionStatus.setTextColor(Color.parseColor("#F44336")); // Red
                batteryStatus.setText("Battery: N/A");
                calibrateButton.setEnabled(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating connection info: " + e.getMessage());
            deviceNameText.setText("Error");
            connectionStatus.setText("Error: " + e.getMessage());
            connectionStatus.setTextColor(Color.parseColor("#F44336")); // Red
            batteryStatus.setText("Battery: N/A");
            calibrateButton.setEnabled(false);
        }
    }
    
    /**
     * Update sensor data displays with new values
     * @param sensorValues Array of int values from flex sensors
     */
    @SuppressLint("SetTextString")
    @Override
    public void updateSensorData(int[] sensorValues) {
        if (sensorValues == null || sensorValues.length != 5) {
            return;
        }
        
        // Store the values
        lastSensorValues = sensorValues.clone();
        
        // Update the sensor display UI elements
        runOnUiThread(() -> {
            // Update each sensor display with its corresponding value
            for (int i = 0; i < Math.min(sensorDisplays.size(), sensorValues.length); i++) {
                sensorDisplays.get(i).updateValue(sensorValues[i]);
            }
        });
        
        // Get accelerometer data from MainActivity
        float[] accelValues = {0, 0};
        float accelX = 0, accelY = 0;
        
        if (mainActivity != null) {
            try {
                accelValues = mainActivity.getAccelerometerValues();
                accelX = accelValues[0];
                accelY = accelValues[1];
            } catch (Exception e) {
                Log.e(TAG, "Error getting accelerometer values: " + e.getMessage());
            }
        }
        
        // Format exactly like Arduino output for serial monitor
        // Get the predicted letter from MainActivity
        String predictedLetter = "-";
        if (mainActivity != null) {
            predictedLetter = mainActivity.getLastPredictedLetter();
        }
        
        // Add accelerometer data (exactly like Arduino format)
        // Even if values are 0.0, still display them for debugging
        final float finalAccelX = accelX;
        final float finalAccelY = accelY;
        
        // Add to serial monitor with current values
        addSerialData(String.format("Accel X: %.2f, Y: %.2f", finalAccelX, finalAccelY));
        
        // Format the output exactly like the Arduino serial monitor
        
        // Add detailed finger data to serial monitor (exactly like Arduino format)
        String[] fingerNames = {"Thumb", "Index", "Middle", "Ring", "Pinky"};
        for (int i = 0; i < Math.min(fingerNames.length, sensorValues.length); i++) {
            float angle = mapSensorToAngle(i, sensorValues[i]);
            addSerialData(String.format("%s: Raw=%d, Angle=%.2f°", fingerNames[i], sensorValues[i], angle));
        }
        
        // Add angle debug details (exactly like Arduino format)
        addSerialData("Angle debug:");
        for (int i = 0; i < Math.min(5, sensorValues.length); i++) {
            float angle = mapSensorToAngle(i, sensorValues[i]);
            addSerialData(String.format("Finger %d: %.2f", i, angle));
        }
        
        // If there's a detected letter, repeat it after the data (as seen in Arduino output)
        if (!predictedLetter.isEmpty() && !predictedLetter.equals("-")) {
            addSerialData(" Detected Letter: " + predictedLetter);
            // Find the description again
            String description = "Unknown gesture";
            switch (predictedLetter) {
                case "A":
                    description = "All fingers fully bent (custom)";
                    break;
                case "B":
                    description = "All fingers moderately bent";
                    break;
                // Add more letter descriptions as needed
            }
            addSerialData(" Description: " + description);
            addSerialData(" Sent flex sensor data");
            addSerialData(" Sent letter as hex: " + String.format("%02X", (int)predictedLetter.charAt(0)));
        }
    }
    
    /**
     * Map a raw sensor value to an angle in degrees
     */
    private float mapSensorToAngle(int sensorIndex, int sensorValue) {
        // Use the exact same calibration values as in ESP32 code
        int[] flexMin = {847, 2510, 1206, 2625, 949};   // Straight
        int[] flexMax = {3056, 4095, 2412, 3611, 2304}; // Bent
        
        // Ensure valid sensor index
        if (sensorIndex < 0 || sensorIndex >= flexMin.length) {
            return 0.0f;
        }
        
        // Constrain to valid range (equivalent to Arduino's constrain function)
        sensorValue = Math.max(flexMin[sensorIndex], Math.min(flexMax[sensorIndex], sensorValue));
        
        // Arduino map() uses integer division and casting
        // map(value, fromLow, fromHigh, toLow, toHigh)
        long result = (long)(sensorValue - flexMin[sensorIndex]) * 90L / (flexMax[sensorIndex] - flexMin[sensorIndex]);
        return (float)result;
    }
    
    /**
     * Map a raw sensor value to a percentage (0-100)
     */
    private int mapSensorToPercentage(int sensorIndex, int sensorValue) {
        // Default mapping values covering full ADC range
        int minVal = 0;
        int maxVal = 4095;
        
        // Use these default ranges that support the full sensor range
        // to avoid hardcoded calibration that might not match actual sensors
        
        // Constrain to valid range
        sensorValue = Math.max(minVal, Math.min(maxVal, sensorValue));
        
        // Map to 0-100 percent
        return Math.round(100.0f * (sensorValue - minVal) / (float)(maxVal - minVal));
    }
    
    /**
     * Add data to the serial monitor
     */
    public void addSerialData(String data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        
        // Format timestamp
        String timestamp = new SimpleDateFormat("[HH:mm:ss.SSS] ", Locale.US).format(new Date());
        final String formattedData = timestamp + data;
        
        runOnUiThread(() -> addNewSerialLine(formattedData));
    }
    
    private void addNewSerialLine(final String line) {
        runOnUiThread(() -> {
            // Append the new data
            serialBuffer.append(line).append("\n");
            
            // Trim the buffer if it gets too long
            if (serialBuffer.length() > MAX_SERIAL_BUFFER) {
                serialBuffer.delete(0, serialBuffer.length() - MAX_SERIAL_BUFFER);
            }
            
            serialMonitorText.setText(serialBuffer.toString());
            
            // Auto-scroll to the bottom
            final ScrollView scrollView = (ScrollView) serialMonitorText.getParent();
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }
    
    private void scrollToBottom() {
        final ScrollView scrollView = (ScrollView) serialMonitorText.getParent();
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    /**
     * Inner class to handle the display of a single sensor
     */
    private class SensorDisplay {
        View containerView;
        TextView nameText;
        TextView valueText;
        ProgressBar progressBar;
        int color;
        
        SensorDisplay(String name, int color) {
            this.color = color;
            
            // Inflate sensor layout
            containerView = getLayoutInflater().inflate(R.layout.item_flex_sensor, null);
            
            // Get references to views
            nameText = containerView.findViewById(R.id.sensorNameText);
            valueText = containerView.findViewById(R.id.sensorValueText);
            progressBar = containerView.findViewById(R.id.sensorProgressBar);
            
            // Set up views
            nameText.setText(name);
            valueText.setText("0");
            progressBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            
            // Add layout parameters
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 32); // Add bottom margin
            containerView.setLayoutParams(params);
            
            // Add to container
            sensorContainer.addView(containerView);
        }
        
        void updateValue(int value) {
            try {
                // Assuming values range from 0 to 4095 (12-bit ADC)
                // Scale to 0-100 for the progress bar
                int percentage = (int) ((value / 4095.0) * 100);
                valueText.setText(String.format("%d", value));
                progressBar.setProgress(percentage);
            } catch (Exception e) {
                Log.e(TAG, "Error updating sensor value: " + e.getMessage());
            }
        }
    }
}
