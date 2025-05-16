package com.example.sign_flex;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * WebAppInterface for JavaScript-to-native communication with BLE functionality
 */
public class WebAppInterface {
    private static final String TAG = "WebAppInterface";
    private final Context mContext;
    private MainActivity mainActivity;

    /**
     * Constructor for WebAppInterface that takes a MainActivity reference
     * @param activity MainActivity for BLE callbacks
     */
    public WebAppInterface(MainActivity activity) {
        this.mContext = activity;
        this.mainActivity = activity;
    }
    
    /**
     * Constructor for WebAppInterface
     * @param context Application context
     */
    public WebAppInterface(Context context) {
        this.mContext = context;
        if (context instanceof MainActivity) {
            this.mainActivity = (MainActivity) context;
        } else {
            Log.e(TAG, "Context is not MainActivity - BLE functions will not work");
        }
    }

    /**
     * Show a toast message from JavaScript
     * @param message Message to display
     */
    @JavascriptInterface
    public void showToast(String message) {
        try {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + e.getMessage());
        }
    }

    /**
     * Check if app is in demo mode
     * @return true if in demo mode, false otherwise
     */
    @JavascriptInterface
    public boolean isDemoMode() {
        if (mainActivity != null) {
            return mainActivity.isDemoMode();
        }
        return true;
    }

    /**
     * Log a message to the Android logcat
     * @param message Message to log
     */
    @JavascriptInterface
    public void log(String message) {
        try {
            Log.d(TAG, "JS: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Error logging: " + e.getMessage());
        }
    }
    
    /**
     * Start scanning for BLE devices
     */
    @JavascriptInterface
    public void scanForDevices() {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                mainActivity.scanForDevices();
            });
        } else {
            Log.e(TAG, "MainActivity not available for scanForDevices()");
        }
    }
    
    /**
     * Connect to a specific BLE device
     * @param deviceAddress MAC address of the device
     */
    @JavascriptInterface
    public void connectToDevice(String deviceAddress) {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                mainActivity.connectToDevice(deviceAddress);
            });
        } else {
            Log.e(TAG, "MainActivity not available for connectToDevice()");
        }
    }
    
    /**
     * Disconnect from the current device
     */
    @JavascriptInterface
    public void disconnect() {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                mainActivity.disconnectFromDevice();
            });
        } else {
            Log.e(TAG, "MainActivity not available for disconnect()");
        }
    }
    
    /**
     * Check if Bluetooth is enabled
     * @return true if enabled
     */
    @JavascriptInterface
    public boolean isBluetoothEnabled() {
        if (mainActivity != null) {
            return mainActivity.isBluetoothEnabled();
        }
        return false;
    }
    
    /**
     * Check if device has BLE support
     * @return true if supported
     */
    @JavascriptInterface
    public boolean hasBleSupport() {
        if (mainActivity != null) {
            return mainActivity.hasBleSupport();
        }
        return false;
    }
    
    /**
     * Get the name of the connected device
     * @return device name or "None" if not connected
     */
    @JavascriptInterface
    public String getConnectedDeviceName() {
        if (mainActivity != null) {
            return mainActivity.getConnectedDeviceName();
        }
        return "None";
    }
    
    /**
     * Calibrate the flex sensors
     */
    @JavascriptInterface
    public void calibrateFlexSensors() {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                mainActivity.calibrateFlexSensors();
            });
        } else {
            Log.e(TAG, "MainActivity not available for calibrateFlexSensors()");
        }
    }
    
    /**
     * Process gesture data
     */
    @JavascriptInterface
    public void processGesture() {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                mainActivity.processGesture();
            });
        } else {
            Log.e(TAG, "MainActivity not available for processGesture()");
        }
    }
    
    /**
     * Set demo mode
     * @param demoMode true to enable demo mode, false to disable
     */
    @JavascriptInterface
    public void setDemoMode(boolean demoMode) {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                mainActivity.setDemoMode(demoMode);
            });
        } else {
            Log.e(TAG, "MainActivity not available for setDemoMode()");
        }
    }
    
    /**
     * Launch the ASL Recognition activity
     */
    @JavascriptInterface
    public void launchASLRecognition() {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                mainActivity.launchASLRecognitionActivity();
            });
        } else {
            Log.e(TAG, "MainActivity not available for launchASLRecognition()");
        }
    }
    
    /**
     * Receive data from JavaScript
     * @param jsonData JSON-formatted data string
     */
    @JavascriptInterface
    public void receiveData(String jsonData) {
        if (mainActivity == null) {
            Log.e(TAG, "MainActivity not available for receiveData()");
            return;
        }
        
        try {
            JSONObject json = new JSONObject(jsonData);
            String action = json.optString("action", "");
            
            switch (action) {
                case "scan":
                    mainActivity.runOnUiThread(() -> mainActivity.scanForDevices());
                    break;
                case "connect":
                    String deviceAddress = json.optString("deviceAddress", "");
                    mainActivity.runOnUiThread(() -> mainActivity.connectToDevice(deviceAddress));
                    break;
                case "disconnect":
                    mainActivity.runOnUiThread(() -> mainActivity.disconnectFromDevice());
                    break;
                case "calibrate":
                    mainActivity.runOnUiThread(() -> mainActivity.calibrateFlexSensors());
                    break;
                case "setDemoMode":
                    boolean demoMode = json.optBoolean("value", true);
                    mainActivity.runOnUiThread(() -> mainActivity.setDemoMode(demoMode));
                    break;
                case "launchASLRecognition":
                    mainActivity.runOnUiThread(() -> mainActivity.launchASLRecognitionActivity());
                    break;
                default:
                    Log.w(TAG, "Unknown action received: " + action);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
        }
    }
}
