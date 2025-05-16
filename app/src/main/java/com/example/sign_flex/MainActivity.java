package com.example.sign_flex;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sign_flex.asl.ASLRecognitionActivity;
import com.example.sign_flex.auth.SessionManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MainActivity for the SIGN_FLEX app with WebView and BLE functionality
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SignFlexApp";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private static final long SCAN_PERIOD = 10000; // Scan for 10 seconds

    // Static instance for use by other activities
    private static MainActivity instance;

    // UUIDs for the SignFlex glove service and characteristics
    // These are example UUIDs - replace with your ESP32's actual UUIDs
    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID FLEX_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID PREDICTED_LETTER_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9");
    private static final UUID BATTERY_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa");

    // Define these as constants to avoid hard-coding UUIDs in multiple places
    private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Device name (for filtering)
    private static final String ESP32_DEVICE_NAME_PREFIX = "ESP32";
    private static final String[] ADDITIONAL_DEVICE_NAMES = {"ASL", "asl", "glove", "Glove", "GLOVE"};

    // Connection states
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private WebView webView;
    private TextView connectionStatus;
    private TextView batteryStatus;
    private TextView predictedLetterTextView; // TextView for predicted letter
    private TextView translationResult;
    private Button scanButton;
    private Button aslButton;
    private Button sensorDataButton;
    private Button loginButton;
    private Button profileButton;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner bleScanner;
    private boolean scanning = false;
    private boolean isDemoMode = false; // Demo mode disabled by default

    // Map to store discovered devices
    private HashMap<String, BluetoothDevice> discoveredDevices = new HashMap<>();
    private BluetoothDevice connectedDevice = null;

    // GATT connection
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;
    private BluetoothGattCharacteristic flexCharacteristic;
    private BluetoothGattCharacteristic batteryCharacteristic;
    private BluetoothGattCharacteristic predictedLetterCharacteristic; // Characteristic for predicted letter
    private int batteryLevel = -1;

    // ASL Recognition Activity reference
    private ASLRecognitionActivity aslRecognitionActivity;

    // Flex Sensor data callback
    private FlexDataListener flexDataListener;

    // Serial data callback
    private SerialDataListener serialDataListener;

    // Last processed flex sensor values (for new activities to query)
    private int[] lastFlexValues = new int[5]; // Assuming 5 flex sensors
    private float accelX = 0;
    private float accelY = 0;
    private String lastPredictedLetter = "";

    // Finger names for display
    private String[] fingerNames = {"Thumb", "Index", "Middle", "Ring", "Pinky"};

    // Flag to control debug toasts and logs
    private static final boolean SHOW_DEBUG_TOASTS = true;

    // Handler for periodic tasks
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable letterPollingRunnable;

    // Sensor calibration
    private boolean calibrationMode = false;
    
    // Keep connection across activities
    private boolean keepConnectionAlive = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Store instance for singleton access
        instance = this;

        try {
            // Initialize UI elements
            webView = findViewById(R.id.webview);  // Note: using webview (lowercase) as in the layout
            connectionStatus = findViewById(R.id.connectionStatus);
            batteryStatus = findViewById(R.id.batteryStatus);
            translationResult = findViewById(R.id.translationResult);
            predictedLetterTextView = findViewById(R.id.predictedLetterTextView);
            scanButton = findViewById(R.id.scanButton);

            // Initialize SessionManager
            SessionManager sessionManager = new SessionManager(this);

            // Initially set to disconnected state
            connectionStatus.setText("Disconnected");
            batteryStatus.setText("Battery: N/A");

            // By default, start in non-demo mode
            isDemoMode = false;

            // Find and initialize the demo mode label
            TextView demoModeLabel = findViewById(R.id.demoModeLabel);
            if (demoModeLabel != null) {
                demoModeLabel.setVisibility(View.GONE);
            }

            // Set up buttons based on login state
            setupButtons(sessionManager.isLoggedIn());

            // Set up scan button
            scanButton.setOnClickListener(v -> {
                if (isDemoMode) {
                    Toast.makeText(MainActivity.this, "Demo mode active. Showing random gestures.", Toast.LENGTH_SHORT).show();
                } else {
                    if (scanning) {
                        stopScan();
                    } else {
                        scanForDevices();
                    }
                }
            });

            // Initialize Bluetooth
            initializeBluetooth();

            // Set up WebView
            setupWebView();

            // Update UI to show initial state (not in demo mode)
            updateUIForDemoMode(isDemoMode);
            
            // Start a timer to periodically poll for the letter characteristic
            startLetterPollingTimer();

            Log.d(TAG, "MainActivity initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during initialization: " + e.getMessage());
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Set up UI buttons based on login status
     */
    private void setupButtons(boolean isLoggedIn) {
        try {
            // Find the parent layout that contains the scan button
            LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
            if (buttonContainer != null) {
                // Clear existing views to prevent duplication
                buttonContainer.removeAllViews();

                // Create button container for main functions
                createMainButtonRow(buttonContainer);

                // Create secondary button container
                LinearLayout secondaryButtonContainer = createSecondaryButtonContainer();

                // Only show the appropriate authentication button based on login status
                if (isLoggedIn) {
                    // User is logged in, show profile button
                    addProfileButton(secondaryButtonContainer);
                } else {
                    // User is not logged in, show login button
                    addLoginButton(secondaryButtonContainer);
                }

                // Add sensor data button
                addSensorDataButton(secondaryButtonContainer);
            } else {
                Log.e(TAG, "Button container not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up buttons: " + e.getMessage());
        }
    }

    /**
     * Create the main button row with ASL and Scan buttons
     */
    private void createMainButtonRow(LinearLayout buttonContainer) {
        // Add ASL recognition button
        aslButton = new Button(this);
        aslButton.setText("ASL Recognition");
        aslButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));
        aslButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, com.example.sign_flex.asl.ASLRecognitionActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching ASL Activity: " + e.getMessage());
                Toast.makeText(this, "Cannot launch ASL Recognition: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        buttonContainer.addView(aslButton);

        // Add scan button
        scanButton = new Button(this);
        scanButton.setText("Scan for Devices");
        scanButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));
        scanButton.setOnClickListener(v -> {
            if (isDemoMode) {
                Toast.makeText(MainActivity.this, "Demo mode active. Showing random gestures.", Toast.LENGTH_SHORT).show();
            } else {
                if (scanning) {
                    stopScan();
                } else {
                    scanForDevices();
                }
            }
        });
        buttonContainer.addView(scanButton);
    }

    /**
     * Create a secondary button container for profile and sensor data
     */
    private LinearLayout createSecondaryButtonContainer() {
        // Find the parent layout
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
        LinearLayout parentContainer = (LinearLayout) buttonContainer.getParent();

        // Create a new row for additional buttons
        LinearLayout secondaryRow = new LinearLayout(this);
        secondaryRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        secondaryRow.setOrientation(LinearLayout.HORIZONTAL);

        // Add margin to the top
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) secondaryRow.getLayoutParams();
        params.setMargins(0, 16, 0, 0);
        secondaryRow.setLayoutParams(params);

        // Add to parent container
        if (parentContainer != null) {
            int index = parentContainer.indexOfChild(buttonContainer) + 1;
            parentContainer.addView(secondaryRow, index);
        }

        return secondaryRow;
    }

    /**
     * Add profile button to the provided container
     */
    private void addProfileButton(LinearLayout container) {
        profileButton = new Button(this);
        profileButton.setText("Profile");
        profileButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));

        // Set margin
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) profileButton.getLayoutParams();
        params.setMargins(8, 0, 8, 0);
        profileButton.setLayoutParams(params);

        // Add click listener
        profileButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching profile activity: " + e.getMessage(), e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        container.addView(profileButton);
    }

    /**
     * Add login button to the provided container
     */
    private void addLoginButton(LinearLayout container) {
        loginButton = new Button(this);
        loginButton.setText("Login");
        loginButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));

        // Set margin
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) loginButton.getLayoutParams();
        params.setMargins(8, 0, 8, 0);
        loginButton.setLayoutParams(params);

        // Add click listener
        loginButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching login activity: " + e.getMessage(), e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        container.addView(loginButton);
    }

    /**
     * Add sensor data button to the provided container
     */
    private void addSensorDataButton(LinearLayout container) {
        sensorDataButton = new Button(this);
        sensorDataButton.setText("Flex Sensor Data");
        sensorDataButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f));

        // Set margin
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sensorDataButton.getLayoutParams();
        params.setMargins(8, 0, 8, 0);
        sensorDataButton.setLayoutParams(params);

        // Add click listener with direct intent creation
        sensorDataButton.setOnClickListener(v -> {
            try {
                // Create and launch intent directly instead of using helper method
                Intent intent = new Intent();
                intent.setClassName(getPackageName(), getPackageName() + ".ESP32SensorActivity");
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching sensor activity: " + e.getMessage(), e);
                Toast.makeText(this, "Error launching sensor data: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        container.addView(sensorDataButton);
    }

    /**
     * Initialize Bluetooth adapter
     */
    private void initializeBluetooth() {
        try {
            // Get Bluetooth adapter
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
            }

            // Check if device supports Bluetooth
            if (bluetoothAdapter == null || bleScanner == null) {
                Log.w(TAG, "Device doesn't support Bluetooth LE");
                setDemoMode(true);
                Toast.makeText(this, "Bluetooth LE not supported - using demo mode", Toast.LENGTH_LONG).show();
                return;
            }

            // Check if Bluetooth is enabled
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please enable Bluetooth to use with ESP32", Toast.LENGTH_LONG).show();
            }

            // Check and request permissions
            checkAndRequestPermissions();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Bluetooth: " + e.getMessage());
            setDemoMode(true);
        }
    }

    /**
     * Check and request required Bluetooth permissions
     */
    private void checkAndRequestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                String[] permissions = {
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                };

                boolean allPermissionsGranted = true;
                for (String permission : permissions) {
                    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }

                if (!allPermissionsGranted) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
                    // Default to demo mode while waiting for permissions
                    setDemoMode(true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
            setDemoMode(true);
        }
    }

    /**
     * Set up WebView with HTML content and JavaScript interface
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        try {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            webView.setWebChromeClient(new WebChromeClient());

            // Make WebView visible
            webView.setVisibility(View.VISIBLE);

            // Add WebAppInterface for JavaScript communication
            webView.addJavascriptInterface(new WebAppInterface(this), "Android");

            // Use loadDataWithBaseURL instead of loadData
            String html = "<html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; padding: 20px; text-align: center; background-color: #f5f5f5; }" +
                    "h1 { color: #2196F3; margin-bottom: 10px; }" +
                    "h2 { color: #555; font-size: 18px; margin-top: 0; margin-bottom: 20px; }" +
                    ".card { background-color: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); " +
                    "       padding: 20px; margin-bottom: 20px; }" +
                    "button { background-color: #4CAF50; color: white; border: none; padding: 12px 20px; " +
                    "        margin: 10px; border-radius: 4px; cursor: pointer; width: 90%; font-size: 16px; }" +
                    "button:hover { opacity: 0.9; }" +
                    "button.blue { background-color: #2196F3; }" +
                    "button.warning { background-color: #FF9800; }" +
                    ".demo-badge { display: inline-block; background-color: #FF5722; color: white; " +
                    "             border-radius: 12px; padding: 5px 10px; font-size: 12px; margin-bottom: 10px; }" +
                    "</style></head>" +
                    "<body>" +
                    "<div class='card'>" +
                    "<h1>SIGN FLEX</h1>" +
                    "<h2>Sign Language Recognition</h2>" +
                    "<div class='demo-badge' id='demoModeIndicator'>Demo Mode Active</div>" +
                    "</div>" +

                    "<div class='card'>" +
                    "<button class='blue' onclick='Android.launchASLRecognition()'>ASL Recognition</button>" +
                    "<button onclick='Android.scanForDevices()'>Scan for Devices</button>" +
                    "<button onclick='Android.calibrateFlexSensors()'>Calibrate Sensors</button>" +
                    "</div>" +

                    "<div class='card'>" +
                    "<button onclick='testDemoMode()'>Check Demo Mode</button>" +
                    "<button class='warning' onclick='toggleDemoMode()'>Toggle Demo Mode</button>" +
                    "</div>" +

                    "<script>" +
                    "function testDemoMode() {" +
                    "  var isDemo = Android.isDemoMode();" +
                    "  Android.showToast('Demo mode is: ' + isDemo);" +
                    "  updateDemoModeDisplay(isDemo);" +
                    "}" +

                    "function toggleDemoMode() {" +
                    "  var currentMode = Android.isDemoMode();" +
                    "  Android.setDemoMode(!currentMode);" +
                    "  updateDemoModeDisplay(!currentMode);" +
                    "  Android.showToast('Demo mode set to: ' + !currentMode);" +
                    "}" +

                    "function updateDemoModeDisplay(isDemoMode) {" +
                    "  var badge = document.getElementById('demoModeIndicator');" +
                    "  if (isDemoMode) {" +
                    "    badge.style.display = 'inline-block';" +
                    "  } else {" +
                    "    badge.style.display = 'none';" +
                    "  }" +
                    "}" +

                    "// Initialize demo mode indicator" +
                    "updateDemoModeDisplay(Android.isDemoMode());" +
                    "</script>" +
                    "</body></html>";

            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

            // Log success
            Log.d(TAG, "WebView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up WebView: " + e.getMessage());
            Toast.makeText(this, "WebView error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update UI to reflect demo mode status
     */
    private void updateUIForDemoMode(boolean demoMode) {
        if (demoMode) {
            // Only set demo mode UI elements if we're not connected to a real device
            if (connectionState != STATE_CONNECTED) {
                connectionStatus.setText("Demo Mode");
                batteryStatus.setText("Battery: 85%");
                scanButton.setText("DEMO MODE");
            }
            // Create a "Demo Mode Active" info banner
            TextView demoModeLabel = findViewById(R.id.demoModeLabel);
            if (demoModeLabel != null) {
                demoModeLabel.setVisibility(View.VISIBLE);
            }
        } else {
            // Only update UI if we're not connected to a real device
            if (connectionState != STATE_CONNECTED) {
                connectionStatus.setText("Disconnected");
                batteryStatus.setText("Battery: N/A");
                scanButton.setText("SCAN FOR GLOVE");
            }
            // Hide the "Demo Mode Active" banner
            TextView demoModeLabel = findViewById(R.id.demoModeLabel);
            if (demoModeLabel != null) {
                demoModeLabel.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Set demo mode
     */
    public void setDemoMode(boolean demoMode) {
        this.isDemoMode = demoMode;
        runOnUiThread(() -> updateUIForDemoMode(demoMode));
    }

    /**
     * Scan for BLE devices
     */
    public void scanForDevices() {
        if (!hasRequiredBluetoothPermissions()) {
            Log.w(TAG, "Cannot scan without required permissions");
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled");
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // If already scanning, stop the scan first
        if (scanning) {
            stopScan();
            // Wait a brief moment before starting a new scan
            handler.postDelayed(this::startBleScan, 100);
        } else {
            // Start the actual scan
            startBleScan();
        }
    }

    /**
     * Start the BLE scanning process
     */
    @SuppressLint("MissingPermission")
    public void startBleScan() {
        if (bleScanner == null) {
            Log.e(TAG, "BLE Scanner not available");
            return;
        }

        // Check if we're already scanning
        if (scanning) {
            Log.d(TAG, "Scan already in progress, not starting another");
            return;
        }

        try {
            // Clear previous results
            discoveredDevices.clear();

            // Set up scan filters to look for ESP32 devices
            List<ScanFilter> filters = new ArrayList<>();

            // Using a more relaxed approach without specific filters
            // to ensure we can see all nearby devices including ESP32

            // Set up scan settings for low latency to find devices quickly
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            // Start scanning
            scanning = true;
            scanButton.setText("STOP SCAN");
            connectionStatus.setText("Scanning for ESP32...");

            Log.d(TAG, "Starting BLE scan for ESP32 devices");
            bleScanner.startScan(filters, settings, scanCallback);

            // Automatically stop scanning after SCAN_PERIOD
            handler.postDelayed(this::stopScan, SCAN_PERIOD);

        } catch (Exception e) {
            Log.e(TAG, "Error starting BLE scan: " + e.getMessage());
            scanning = false;
            scanButton.setText("SCAN FOR GLOVE");
            connectionStatus.setText("Scan error");
        }
    }

    /**
     * Stop the BLE scanning process
     */
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (bleScanner != null && scanning) {
            try {
                Log.d(TAG, "Stopping BLE scan");
                bleScanner.stopScan(scanCallback);
                scanning = false;
                scanButton.setText(R.string.scan_for_glove);
                connectionStatus.setText(R.string.scan_complete);
                Log.d(TAG, "BLE scan stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping scan: " + e.getMessage());
            }
        }
    }

    /**
     * Callback for BLE scan results
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d(TAG, "Found device: " + device.getName() + " [" + device.getAddress() + "]");

            if (device.getName() != null) {
                boolean isTargetDevice = device.getName().startsWith(ESP32_DEVICE_NAME_PREFIX);

                // Check for additional device names
                if (!isTargetDevice) {
                    for (String deviceName : ADDITIONAL_DEVICE_NAMES) {
                        if (device.getName().contains(deviceName)) {
                            isTargetDevice = true;
                            break;
                        }
                    }
                }

                if (isTargetDevice) {
                    // This is likely our ESP32 device or another supported device
                    Log.i(TAG, "Found supported device: " + device.getName() + " [" + device.getAddress() + "]");
                    stopScan();
                    connectToDevice(device);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan failed with error: " + errorCode);
            runOnUiThread(() -> {
                scanning = false;
                scanButton.setText("SCAN FOR GLOVE");
                connectionStatus.setText("Scan failed");
                Toast.makeText(MainActivity.this, "Scan failed with error " + errorCode, Toast.LENGTH_SHORT).show();
            });
        }
    };

    /**
     * Connect to a device by address
     */
    @SuppressLint("MissingPermission")
    public void connectToDevice(String deviceAddress) {
        if (deviceAddress == null) {
            Log.e(TAG, "Cannot connect to null address");
            return;
        }

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            if (device == null) {
                Log.e(TAG, "Device not found with address: " + deviceAddress);
                return;
            }

            connectToDevice(device);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device by address: " + e.getMessage());
            Toast.makeText(this, "Error connecting to device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Connect to a device
     */
    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot connect to null device");
            return;
        }

        Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();

        // Store the device and update UI
        connectedDevice = device;

        try {
            // Close existing connection if any
            if (bluetoothGatt != null) {
                bluetoothGatt.close();
            }

            connectionState = STATE_CONNECTING;
            // Connect to GATT server on the device
            bluetoothGatt = device.connectGatt(this, false, gattCallback);

            Log.d(TAG, "Connecting to GATT server on " + device.getName());

            // Update UI
            runOnUiThread(() -> {
                connectionStatus.setText("Connecting...");
            });

            // Debug - log the UUIDs we're looking for
            Log.d(TAG, "Will be looking for Service UUID: " + SERVICE_UUID);
            Log.d(TAG, "Will be looking for Flex Char UUID: " + FLEX_CHAR_UUID);
            Log.d(TAG, "Will be looking for Battery Char UUID: " + BATTERY_CHAR_UUID);
            Log.d(TAG, "Will be looking for Predicted Letter Char UUID: " + PREDICTED_LETTER_CHAR_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device: " + e.getMessage());
            connectionState = STATE_DISCONNECTED;
            bluetoothGatt = null;
        }
    }

    /**
     * BluetoothGatt callback for connection and service discovery
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server on " + gatt.getDevice().getName());

                // Update UI
                runOnUiThread(() -> {
                    connectionStatus.setText("Connected to " + gatt.getDevice().getName());

                    // Explicitly disable demo mode when a real device is connected
                    if (isDemoMode) {
                        isDemoMode = false;
                        updateUIForDemoMode(false);
                        Log.d(TAG, "Demo mode disabled due to real device connection");
                    }
                });

                // Discover services
                try {
                    Log.i(TAG, "Attempting to discover services...");
                    boolean discovering = gatt.discoverServices();
                    Log.d(TAG, "Service discovery started: " + discovering);
                } catch (Exception e) {
                    Log.e(TAG, "Error discovering services: " + e.getMessage());
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server");

                // Update UI
                runOnUiThread(() -> {
                    connectionStatus.setText("Disconnected");
                    batteryStatus.setText("Battery: N/A");
                });

                // Close the GATT connection
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered successfully");

                // Debug - log all services and characteristics for troubleshooting
                List<BluetoothGattService> services = gatt.getServices();
                Log.d(TAG, "Found " + services.size() + " services on device");

                for (BluetoothGattService service : services) {
                    Log.d(TAG, "Service: " + service.getUuid());
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic c : characteristics) {
                        Log.d(TAG, "  Characteristic: " + c.getUuid() + ", properties: " + c.getProperties());
                    }
                }

                // Find the SignFlex service
                BluetoothGattService signFlexService = gatt.getService(SERVICE_UUID);
                if (signFlexService != null) {
                    Log.i(TAG, "SignFlex service found");

                    // Get flex characteristic
                    flexCharacteristic = signFlexService.getCharacteristic(FLEX_CHAR_UUID);
                    if (flexCharacteristic != null) {
                        Log.i(TAG, "Flex characteristic found. Properties: " +
                                flexCharacteristic.getProperties());

                        // Enable notifications if supported
                        if ((flexCharacteristic.getProperties() &
                                BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            Log.d(TAG, "Enabling notifications for flex characteristic");

                            // First enable local notification
                            boolean success = gatt.setCharacteristicNotification(flexCharacteristic, true);
                            Log.d(TAG, "Set flex notification success: " + success);

                            // Then set the descriptor to enable remote notifications
                            try {
                                BluetoothGattDescriptor flexDescriptor = flexCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
                                if (flexDescriptor != null) {
                                    flexDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    boolean descriptorWritten = gatt.writeDescriptor(flexDescriptor);
                                    Log.d(TAG, "Flex descriptor write initiated: " + descriptorWritten);

                                    // We'll wait for the descriptor write to complete before enabling the next characteristic
                                    return; // Exit early and continue after descriptor write callback
                                } else {
                                    Log.w(TAG, "Flex notification descriptor not found");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error enabling flex notifications: " + e.getMessage());
                            }
                        } else {
                            Log.w(TAG, "Flex characteristic does not support notifications");
                        }
                    } else {
                        Log.w(TAG, "Flex characteristic not found");
                    }

                    // Get battery characteristic
                    batteryCharacteristic = signFlexService.getCharacteristic(BATTERY_CHAR_UUID);
                    Log.d(TAG, "Battery characteristic found: " + (batteryCharacteristic != null));

                    // Get predicted letter characteristic
                    predictedLetterCharacteristic = signFlexService.getCharacteristic(PREDICTED_LETTER_CHAR_UUID);
                    if (predictedLetterCharacteristic != null) {
                        Log.i(TAG, "Predicted letter characteristic found. Properties: " +
                                predictedLetterCharacteristic.getProperties());

                        // Enable notifications if supported
                        if ((predictedLetterCharacteristic.getProperties() &
                                BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            Log.d(TAG, "Enabling notifications for predicted letter characteristic");

                            // First set local notification
                            boolean success = gatt.setCharacteristicNotification(predictedLetterCharacteristic, true);
                            Log.d(TAG, "Set letter notification success: " + success);

                            // Then write the descriptor to enable remote notifications
                            try {
                                BluetoothGattDescriptor descriptor = predictedLetterCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);

                                if (descriptor != null) {
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    boolean success1 = gatt.writeDescriptor(descriptor);
                                    Log.d(TAG, "Letter descriptor write initiated: " + success1);

                                    // Wait for the descriptor write completion
                                    return;
                                } else {
                                    Log.w(TAG, "Letter notification descriptor not found");
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                        "Letter characteristic descriptor not found!", Toast.LENGTH_LONG).show());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error enabling letter notifications: " + e.getMessage(), e);
                                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                    "Error enabling letter notifications: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        } else {
                            Log.w(TAG, "Predicted letter characteristic does not support notifications");
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                "Letter characteristic doesn't support notifications!", Toast.LENGTH_LONG).show());
                        }
                    } else {
                        Log.w(TAG, "Predicted letter characteristic not found");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Letter characteristic not found!", Toast.LENGTH_LONG).show());
                    }
                } else {
                    Log.w(TAG, "SignFlex service not found");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "SignFlex service not found!", Toast.LENGTH_LONG).show());
                }
            } else {
                Log.w(TAG, "Failed to discover services, status: " + status);
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "Failed to discover BLE services: " + status, Toast.LENGTH_LONG).show());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: " + descriptor.getUuid() + ", status: " + status);

            // Check which characteristic this descriptor belongs to
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

            if (characteristic.getUuid().equals(FLEX_CHAR_UUID)) {
                Log.d(TAG, "Flex sensor notifications enabled");

                // Now enable battery notifications
                if (batteryCharacteristic != null) {
                    enableNotifications(gatt, batteryCharacteristic);
                } else {
                    // Move on to letter characteristic
                    if (predictedLetterCharacteristic != null) {
                        enableNotifications(gatt, predictedLetterCharacteristic);
                    }
                }
            } else if (characteristic.getUuid().equals(BATTERY_CHAR_UUID)) {
                Log.d(TAG, "Battery notifications enabled");

                // Now enable letter notifications
                if (predictedLetterCharacteristic != null) {
                    enableNotifications(gatt, predictedLetterCharacteristic);
                }
            } else if (characteristic.getUuid().equals(PREDICTED_LETTER_CHAR_UUID)) {
                Log.d(TAG, "Letter notifications enabled");
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "All notifications enabled successfully!", Toast.LENGTH_SHORT).show());
            }
        }

        /**
         * Helper method to enable notifications for a characteristic
         */
        @SuppressLint("MissingPermission")
        private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (gatt == null || characteristic == null) return;

            try {
                // Enable local notifications
                boolean success = gatt.setCharacteristicNotification(characteristic, true);
                Log.d(TAG, "Set notification for " + characteristic.getUuid() + ": " + success);

                // Enable remote notifications via descriptor
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID);

                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                    Log.d(TAG, "Writing descriptor for " + characteristic.getUuid());
                } else {
                    Log.w(TAG, "Descriptor not found for " + characteristic.getUuid());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error enabling notifications: " + e.getMessage());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID charUuid = characteristic.getUuid();
            Log.d(TAG, "onCharacteristicChanged inside callback: " + charUuid.toString());

            // Show on UI thread which characteristic changed
            if (SHOW_DEBUG_TOASTS) {
                final String charName;
                if (charUuid.equals(FLEX_CHAR_UUID)) {
                    charName = "FLEX";
                } else if (charUuid.equals(BATTERY_CHAR_UUID)) {
                    charName = "BATTERY";
                } else if (charUuid.equals(PREDICTED_LETTER_CHAR_UUID)) {
                    charName = "LETTER";
                } else {
                    charName = "UNKNOWN";
                }

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        "Received notification: " + charName, Toast.LENGTH_SHORT).show();
                });
            }

            if (charUuid.equals(FLEX_CHAR_UUID)) {
                // Process flex sensor data
                byte[] data = characteristic.getValue();
                if (data != null) {
                    Log.d(TAG, "Received flex data, length: " + data.length);
                    processFlexData(data);
                }
            } else if (charUuid.equals(BATTERY_CHAR_UUID)) {
                // Process battery data
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    batteryLevel = data[0] & 0xFF;

                    Log.d(TAG, "Battery level update: " + batteryLevel + "%");

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        batteryStatus.setText("Battery: " + batteryLevel + "%");
                    });

                    // Send to serial monitor if available
                    if (serialDataListener != null) {
                        serialDataListener.addSerialData("Battery level: " + batteryLevel + "%");
                    }

                    Log.d(TAG, "Received battery level: " + batteryLevel + "%");
                }
            } else if (charUuid.equals(PREDICTED_LETTER_CHAR_UUID)) {
                // Process predicted letter data
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    char letter = (char) data[0];
                    String gesture = String.valueOf(letter);

                    // Log full details about the received letter
                    Log.d(TAG, "Received letter: '" + gesture + "' (ASCII: " + (int) letter + ", Hex: " + String.format("%02X", (int) letter) + ")");

                    // Add to serial monitor if available
                    if (serialDataListener != null) {
                        serialDataListener.addSerialData("Detected letter: '" + gesture + "'");
                        serialDataListener.addSerialData("Letter as hex: " + String.format("%02X", (int) letter));
                    }

                    // Extra debug info
                    if (SHOW_DEBUG_TOASTS) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                "Received letter: " + gesture, Toast.LENGTH_LONG).show();
                        });
                    }

                    // Save the last predicted letter for other activities
                    lastPredictedLetter = gesture;

                    // Update UI on main thread
                    updateLetterUI(gesture);
                } else {
                    Log.e(TAG, "Received empty letter data!");
                    if (SHOW_DEBUG_TOASTS) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                "Received empty letter data", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } else {
                Log.d(TAG, "Received notification for unknown characteristic: " + charUuid);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            processCharacteristicRead(gatt, characteristic, status);
        }
    };

    /**
     * Process data from the onCharacteristicRead callback
     */
    public void processCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "processCharacteristicRead: " + characteristic.getUuid() + ", status: " + status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (characteristic.getUuid().equals(PREDICTED_LETTER_CHAR_UUID)) {
                // Process the letter data
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    char letter = (char) data[0];
                    String gesture = String.valueOf(letter);

                    // Log the data
                    Log.d(TAG, "Read letter characteristic: '" + gesture + "', ASCII: " + (int) letter + 
                          ", HEX: " + String.format("%02X", (int)letter));

                    // Update debug status
                    runOnUiThread(() -> {
                        TextView debugStatus = findViewById(R.id.debugStatusText);
                        if (debugStatus != null) {
                            debugStatus.setText("Letter read success: '" + gesture + "'");
                            debugStatus.setTextColor(Color.GREEN);
                        }
                    });
                    
                    // Extra debug info
                    if (SHOW_DEBUG_TOASTS) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                "Read letter: " + gesture, Toast.LENGTH_LONG).show();
                        });
                    }

                    // Only process valid letters
                    if ((letter >= 'A' && letter <= 'Z') || letter == '?') {
                        // Save and update UI
                        lastPredictedLetter = gesture;
                        updateLetterUI(gesture);
                        
                        // Show a toast for feedback
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                "Letter detected: " + gesture, Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.d(TAG, "Letter read returned invalid char: ASCII=" + (int)letter);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                "Invalid letter code: " + (int)letter, Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.w(TAG, "Letter characteristic read returned empty data");
                    if (SHOW_DEBUG_TOASTS) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                "Letter characteristic is empty", Toast.LENGTH_LONG).show();
                                
                            // Update debug status
                            TextView debugStatus = findViewById(R.id.debugStatusText);
                            if (debugStatus != null) {
                                debugStatus.setText("ERROR: Letter data empty");
                                debugStatus.setTextColor(Color.RED);
                            }
                        });
                    }
                }
            } else if (characteristic.getUuid().equals(FLEX_CHAR_UUID)) {
                // Process flex data
                byte[] data = characteristic.getValue();
                if (data != null) {
                    Log.d(TAG, "Read flex data, length: " + data.length);
                    processFlexData(data);
                }
            }
        } else {
            Log.e(TAG, "Characteristic read failed with status: " + status);
            if (SHOW_DEBUG_TOASTS) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        "Letter read failed: " + status, Toast.LENGTH_LONG).show();
                        
                    // Update debug status
                    TextView debugStatus = findViewById(R.id.debugStatusText);
                    if (debugStatus != null) {
                        debugStatus.setText("ERROR: Read failed, status=" + status);
                        debugStatus.setTextColor(Color.RED);
                    }
                });
            }
        }
    }

    /**
     * Process data from flex sensors
     */
    private void processFlexData(byte[] data) {
        if (data != null && data.length >= 10) {
            // Extract flex sensor values (5 sensors, 2 bytes each)
            int[] sensorValues = new int[5];
            for (int i = 0; i < 5; i++) {
                int lowByte = data[i * 2] & 0xFF;
                int highByte = data[i * 2 + 1] & 0xFF;
                sensorValues[i] = (highByte << 8) | lowByte;
            }
            
            // Store the sensor values
            lastFlexValues = sensorValues;
            
            // Process and display the extracted sensor values
            calculateAnglesAndUpdateUI(sensorValues);
            
            // Extract accelerometer data if it's included in the packet
            if (data.length >= 18) {  // 10 bytes for flex sensors + 8 bytes for accelerometer (2 float values)
                try {
                    Log.d(TAG, "Data packet length: " + data.length);
                    
                    // Print raw bytes in hex for debugging - ESP32 should be sending accelX and accelY
                    StringBuilder rawBytesLog = new StringBuilder("Raw accel bytes: ");
                    for (int i = 10; i < 18; i++) {
                        rawBytesLog.append(String.format("%02X ", data[i] & 0xFF));
                    }
                    Log.d(TAG, rawBytesLog.toString());
                    
                    // Check if all bytes are zero
                    boolean allZeros = true;
                    for (int i = 10; i < 18; i++) {
                        if (data[i] != 0) {
                            allZeros = false;
                            break;
                        }
                    }
                    
                    if (allZeros) {
                        Log.w(TAG, "ERROR: All accelerometer bytes are zero! The ESP32 might not be sending data correctly.");
                        accelX = accelY = 0.0f;
                    } else {
                        // Using ByteBuffer with LITTLE_ENDIAN, which matches ESP32's byte order
                        ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(data, 10, 18));
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        accelX = buffer.getFloat(0);
                        accelY = buffer.getFloat(4);
                        
                        Log.d(TAG, "Extracted accel values: X=" + accelX + ", Y=" + accelY);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error extracting accelerometer data: " + e.getMessage(), e);
                    accelX = accelY = 0.0f;
                }
            }
            
            // Update flex data listener if set
            if (flexDataListener != null) {
                flexDataListener.updateSensorData(sensorValues);
            }
            
            // Log the processed data
            Log.d(TAG, "Flex sensors: " + Arrays.toString(sensorValues));
            
            // Update serial monitor if available
            if (serialDataListener != null) {
                serialDataListener.addSerialData("Flex data processed: " + Arrays.toString(sensorValues));
                serialDataListener.addSerialData("Accel X: " + accelX + ", Y: " + accelY);
            }
        } else {
            Log.e(TAG, "Received incomplete flex data, expected at least 10 bytes");
        }
    }

    /**
     * Calculate finger angles and update the UI
     */
    private void calculateAnglesAndUpdateUI(int[] flexValues) {
        if (flexValues == null || flexValues.length != 5) return;

        float[] angles = new float[5];
        StringBuilder angleDisplay = new StringBuilder();
        
        // Add header for flex angles with red color
        angleDisplay.append("Flex Angles:\n");
        
        // Calculate angles and build display string with each finger on its own line
        for (int i = 0; i < 5; i++) {
            angles[i] = mapSensorToAngle(i, flexValues[i]);
            angleDisplay.append(fingerNames[i]).append(": ").append(String.format("%.2f°", angles[i]));
            if (i < 4) {
                angleDisplay.append("\n");
            }
        }

        // Add accelerometer data on its own line
        angleDisplay.append("\nAccel X: ").append(String.format("%.2f", accelX))
                .append(" | Y: ").append(String.format("%.2f", accelY));

        // Update UI on main thread
        runOnUiThread(() -> {
            if (translationResult != null) {
                translationResult.setText(angleDisplay.toString());
                translationResult.setTextColor(Color.RED);
                translationResult.setTypeface(Typeface.DEFAULT_BOLD);
                translationResult.setTextSize(18);
            }
        });
    }

    /**
     * Process gesture from sensor data
     */
    public void processGesture() {
        // Handle demo mode first
        if (isDemoMode) {
            String[] demoSigns = {"Hello", "Thank you", "Help", "Yes", "No"};
            int randomIndex = (int) (Math.random() * demoSigns.length);
            String sign = demoSigns[randomIndex];

            runOnUiThread(() -> {
                translationResult.setText("Sign: " + sign);
            });

            Toast.makeText(this, "Detected sign: " + sign, Toast.LENGTH_SHORT).show();

            // Notify serial monitor if connected
            if (serialDataListener != null) {
                serialDataListener.addSerialData("Demo mode - detected sign: " + sign);
            }

            return;
        }

        // If not demo mode but no device is connected
        if (connectionState != STATE_CONNECTED) {
            Toast.makeText(this, "Connect to device first", Toast.LENGTH_SHORT).show();
            if (serialDataListener != null) {
                serialDataListener.addSerialData("Cannot process gesture - no device connected");
            }
            return;
        }

        // We're no longer doing gesture recognition here
        // All gesture recognition is done on the ESP32 side and sent via BLE
        Log.d(TAG, "Gesture recognition is handled by the ESP32 device");

        if (serialDataListener != null) {
            serialDataListener.addSerialData("Gesture recognition is handled by the ESP32 device");
        }
    }

    /**
     * Disconnect from the current device
     */
    public void disconnectFromDevice() {
        if (connectionState == STATE_CONNECTED && bluetoothGatt != null) {
            try {
                disconnectGatt();
                runOnUiThread(() -> {
                    connectionStatus.setText("Disconnected");
                    batteryStatus.setText("Battery: N/A");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting: " + e.getMessage());
            }
        }
    }

    /**
     * Close GATT connection
     */
    @SuppressLint("MissingPermission")
    private void disconnectGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            connectionState = STATE_DISCONNECTED;
            flexCharacteristic = null;
            batteryCharacteristic = null;
            predictedLetterCharacteristic = null;
            Log.d(TAG, "GATT connection closed");
        }
    }

    /**
     * Launch the ASL Recognition Activity
     */
    public void launchASLRecognitionActivity() {
        try {
            Intent intent = new Intent(this, com.example.sign_flex.asl.ASLRecognitionActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching ASL Activity: " + e.getMessage());
            Toast.makeText(this, "Cannot launch ASL Recognition: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Launch the ESP32 Sensor Activity
     */
    public void launchESP32SensorActivity() {
        try {
            Log.d(TAG, "Attempting to launch ESP32SensorActivity");

            // Make sure we have a valid context
            Context context = this;
            if (context == null) {
                Log.e(TAG, "Cannot launch ESP32SensorActivity - context is null");
                return;
            }

            // Create intent with explicit class name to avoid ambiguity
            Intent intent = new Intent(context, ESP32SensorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Start the activity
            context.startActivity(intent);
            Log.d(TAG, "ESP32SensorActivity launched successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error launching ESP32 Sensor Activity: " + e.getMessage(), e);
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Launch the Profile Activity
     */
    public void launchProfileActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching profile activity: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening profile page", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Set the ASL Recognition Activity reference
     */
    public void setASLRecognitionActivity(com.example.sign_flex.asl.ASLRecognitionActivity activity) {
        this.aslRecognitionActivity = activity;
    }

    /**
     * Clear the ASL Recognition Activity reference
     */
    public void clearASLRecognitionActivity() {
        this.aslRecognitionActivity = null;
    }

    /**
     * Get the static instance of MainActivity
     */
    public static MainActivity getInstance() {
        return instance;
    }

    /**
     * Check if a device is connected
     */
    public boolean isDeviceConnected() {
        return connectionState == STATE_CONNECTED;
    }

    /**
     * Get the current battery level
     */
    public int getBatteryLevel() {
        return batteryLevel;
    }

    /**
     * Get the last predicted letter
     */
    public String getLastPredictedLetter() {
        return lastPredictedLetter;
    }

    /**
     * Get raw flex sensor values
     */
    public int[] getFlexSensorValues() {
        return lastFlexValues;
    }

    /**
     * Get accelerometer values
     */
    public float[] getAccelerometerValues() {
        return new float[] {accelX, accelY};
    }

    /**
     * Get X accelerometer value
     */
    public float getAccelX() {
        return accelX;
    }

    /**
     * Get Y accelerometer value
     */
    public float getAccelY() {
        return accelY;
    }

    /**
     * Check if demo mode is enabled
     */
    public boolean isDemoMode() {
        return isDemoMode;
    }

    /**
     * Map a raw sensor value to an angle in degrees
     * Implementation matches Arduino's map() function exactly
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
        long result = (long) (sensorValue - flexMin[sensorIndex]) * 90L / (flexMax[sensorIndex] - flexMin[sensorIndex]);
        return (float) result;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
        
        if (bluetoothGatt != null && connectionState == STATE_CONNECTED) {
            Log.d(TAG, "Re-enabling notifications in onResume...");
            
            // First, get the BLE service
            BluetoothGattService espService = bluetoothGatt.getService(SERVICE_UUID);
            if (espService != null) {
                // Re-enable notifications for flex data
                BluetoothGattCharacteristic flexChar = espService.getCharacteristic(FLEX_CHAR_UUID);
                if (flexChar != null) {
                    boolean success = bluetoothGatt.setCharacteristicNotification(flexChar, true);
                    Log.d(TAG, "Re-enabled flex notifications: " + success);
                    
                    // Enable the CCCD
                    BluetoothGattDescriptor descriptor = flexChar.getDescriptor(CLIENT_CONFIG_UUID);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(descriptor);
                        Log.d(TAG, "Wrote flex CCCD for notifications");
                    }
                }
                
                // Re-enable notifications for letter detection
                BluetoothGattCharacteristic letterChar = espService.getCharacteristic(PREDICTED_LETTER_CHAR_UUID);
                if (letterChar != null) {
                    boolean success = bluetoothGatt.setCharacteristicNotification(letterChar, true);
                    Log.d(TAG, "Re-enabled letter notifications: " + success);
                    
                    // Enable the CCCD
                    BluetoothGattDescriptor descriptor = letterChar.getDescriptor(CLIENT_CONFIG_UUID);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(descriptor);
                        Log.d(TAG, "Wrote letter CCCD for notifications");
                    }
                }
                
                // Update UI with last detected letter if available
                if (lastPredictedLetter != null && !lastPredictedLetter.isEmpty()) {
                    updateLetterUI(lastPredictedLetter);
                    Log.d(TAG, "Restored last detected letter to UI: " + lastPredictedLetter);
                }
                
                // Force a letter read to update immediately
                if (letterChar != null) {
                    bluetoothGatt.readCharacteristic(letterChar);
                    Log.d(TAG, "Requested immediate letter characteristic read");
                }
            } else {
                Log.e(TAG, "Could not find ESP32 service in onResume");
            }
        }
        
        // Sync activity with demo mode
        updateUIForDemoMode(isDemoMode);

        // Reload the web content to ensure it's fresh
        if (webView != null) {
            webView.reload();
        }

        // Add debug buttons to help with BLE troubleshooting
        if (SHOW_DEBUG_TOASTS) {
            addDebugButtonRow();
        }
        
        // Restart letter polling timer
        startLetterPollingTimer();

        Log.d(TAG, "onResume completed");
    }

    /**
     * Add a row of debug buttons to help with BLE troubleshooting
     */
    private void addDebugButtonRow() {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        // Check if we already added the debug container
        LinearLayout debugContainer = findViewById(R.id.debugContainer);
        if (debugContainer == null) {
            // Create a container for the debug buttons
            debugContainer = new LinearLayout(this);
            debugContainer.setId(R.id.debugContainer);
            debugContainer.setOrientation(LinearLayout.HORIZONTAL);
            debugContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            debugContainer.setBackgroundColor(Color.rgb(50, 50, 50));
            debugContainer.setPadding(16, 8, 16, 8);

            // Create a parent layout to hold our debug UI
            LinearLayout debugParent = new LinearLayout(this);
            debugParent.setOrientation(LinearLayout.VERTICAL);
            debugParent.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // Add the container to our new parent
            debugParent.addView(debugContainer);

            // Add letter read button
            Button readLetterButton = new Button(this);
            readLetterButton.setText("Read Letter");
            readLetterButton.setBackgroundColor(Color.RED);
            readLetterButton.setTextColor(Color.WHITE);
            readLetterButton.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            readLetterButton.setOnClickListener(v -> forceReadLetterCharacteristic());
            debugContainer.addView(readLetterButton);

            // Add service discovery button
            Button discoverServicesButton = new Button(this);
            discoverServicesButton.setText("Discover Services");
            discoverServicesButton.setBackgroundColor(Color.BLUE);
            discoverServicesButton.setTextColor(Color.WHITE);
            discoverServicesButton.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            discoverServicesButton.setOnClickListener(v -> discoverServices());
            debugContainer.addView(discoverServicesButton);

            // Add a status text view
            TextView debugStatusText = new TextView(this);
            debugStatusText.setId(R.id.debugStatusText);
            debugStatusText.setText("BLE Debug Mode Active");
            debugStatusText.setTextColor(Color.YELLOW);
            debugStatusText.setGravity(Gravity.CENTER);
            debugStatusText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            debugParent.addView(debugStatusText);

            // Add our debug UI to the main activity
            if (rootView instanceof ViewGroup) {
                ((ViewGroup) rootView).addView(debugParent, 0);
            }
        }
    }

    /**
     * Start a timer to periodically poll for the letter characteristic
     * This will ensure we get letter data even if notifications aren't working
     */
    private void startLetterPollingTimer() {
        // Cancel any existing timer
        if (letterPollingRunnable != null) {
            handler.removeCallbacks(letterPollingRunnable);
        }
        
        // Create a new polling task
        letterPollingRunnable = new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                // Only run if we're connected to a device
                if (connectionState == STATE_CONNECTED && bluetoothGatt != null) {
                    Log.d(TAG, "Polling for letter data...");
                    
                    // Try to read the letter characteristic directly
                    BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
                    if (service != null) {
                        // Read the letter characteristic
                        BluetoothGattCharacteristic letterChar = service.getCharacteristic(PREDICTED_LETTER_CHAR_UUID);
                        if (letterChar != null) {
                            // Read directly (this will trigger onCharacteristicRead callback)
                            bluetoothGatt.readCharacteristic(letterChar);
                            
                            // Update debug status if available
                            TextView debugStatus = findViewById(R.id.debugStatusText);
                            if (debugStatus != null) {
                                debugStatus.setText("Polling letter char...");
                                debugStatus.setTextColor(Color.YELLOW);
                            }
                        }
                    }
                }
                
                // Schedule next run (every 1 second)
                handler.postDelayed(this, 1000);
            }
        };
        
        // Start the timer with initial delay
        handler.postDelayed(letterPollingRunnable, 3000); // Start after device connects
        
        Log.d(TAG, "Letter polling timer started");
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Stop the letter polling when app is paused
        if (letterPollingRunnable != null) {
            handler.removeCallbacks(letterPollingRunnable);
        }
    }

    /**
     * Force a direct read of the letter characteristic
     * This is a manual fallback to check if we can read the letter directly
     */
    private void forceReadLetterCharacteristic() {
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            Toast.makeText(this, "BLE not initialized! Connect to device first.", Toast.LENGTH_SHORT).show();

            // Update debug status
            TextView debugStatus = findViewById(R.id.debugStatusText);
            if (debugStatus != null) {
                debugStatus.setText("ERROR: BLE not initialized");
                debugStatus.setTextColor(Color.RED);
            }
            return;
        }

        Log.d(TAG, "Attempting to directly read letter characteristic...");
        Toast.makeText(this, "Reading letter characteristic...", Toast.LENGTH_SHORT).show();

        // Update debug status
        TextView debugStatus = findViewById(R.id.debugStatusText);
        if (debugStatus != null) {
            debugStatus.setText("Reading letter characteristic - waiting for response...");
            debugStatus.setTextColor(Color.YELLOW);
        }

        // Get the service
        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            Log.e(TAG, "Service not found: " + SERVICE_UUID);
            Toast.makeText(this, "BLE service not found!", Toast.LENGTH_SHORT).show();

            // Update debug status
            if (debugStatus != null) {
                debugStatus.setText("ERROR: BLE service not found");
                debugStatus.setTextColor(Color.RED);
            }
            return;
        }

        // Get the letter characteristic
        BluetoothGattCharacteristic letterChar = service.getCharacteristic(PREDICTED_LETTER_CHAR_UUID);
        if (letterChar != null) {
            boolean success = bluetoothGatt.readCharacteristic(letterChar);
            Log.d(TAG, "Letter characteristic read initiated: " + success);

            // Update debug status
            if (debugStatus != null) {
                if (success) {
                    debugStatus.setText("Reading letter characteristic - waiting for response...");
                    debugStatus.setTextColor(Color.YELLOW);

                    // Set a timeout to update the status if we don't get a response
                    handler.postDelayed(() -> {
                        if (debugStatus.getText().toString().contains("waiting for response")) {
                            debugStatus.setText("WARNING: No response received from read request");
                            debugStatus.setTextColor(Color.rgb(255, 165, 0)); // Orange
                        }
                    }, 3000); // 3 second timeout
                } else {
                    debugStatus.setText("ERROR: Failed to initiate letter read");
                    debugStatus.setTextColor(Color.RED);
                }
            }

            Toast.makeText(this, "Letter characteristic read: " + success, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Letter characteristic not found: " + PREDICTED_LETTER_CHAR_UUID);

            // Update debug status
            if (debugStatus != null) {
                debugStatus.setText("ERROR: Letter characteristic not found");
                debugStatus.setTextColor(Color.RED);
            }

            Toast.makeText(this, "Letter characteristic not found!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Interface for receiving flex sensor data updates
     */
    public interface FlexDataListener {
        void updateSensorData(int[] sensorValues);
    }

    /**
     * Interface for receiving serial data updates
     */
    public interface SerialDataListener {
        void addSerialData(String data);
    }

    /**
     * Set a listener for flex data updates
     */
    public void setFlexDataListener(FlexDataListener listener) {
        this.flexDataListener = listener;

        // Send the last known values immediately if we have a new listener
        if (listener != null && isDeviceConnected()) {
            listener.updateSensorData(lastFlexValues);
        }
    }

    /**
     * Set a listener for serial data updates
     */
    public void setSerialDataListener(SerialDataListener listener) {
        this.serialDataListener = listener;

        // Send a connection status message immediately if we have a new listener
        if (listener != null) {
            if (isDeviceConnected()) {
                listener.addSerialData("Device connected: " + getConnectedDeviceName());
                listener.addSerialData("Battery level: " + getBatteryLevel() + "%");
            } else {
                listener.addSerialData("No device connected");
            }
        }
    }

    /**
     * Helper method to check if Bluetooth permissions are granted
     */
    private boolean hasRequiredBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permissions not required for older Android versions
    }

    /**
     * Check if Bluetooth is enabled
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Check if device has BLE support
     */
    public boolean hasBleSupport() {
        return bluetoothAdapter != null;
    }

    /**
     * Get name of connected device
     */
    @SuppressLint("MissingPermission")
    public String getConnectedDeviceName() {
        if (connectedDevice != null) {
            try {
                return connectedDevice.getName();
            } catch (SecurityException se) {
                Log.e(TAG, "Security exception getting device name: " + se.getMessage());
            }
        }
        return "None";
    }

    /**
     * Calibrate flex sensors - sends calibration command to ESP32
     */
    public void calibrateFlexSensors() {
        if (connectionState != STATE_CONNECTED || flexCharacteristic == null) {
            Log.e(TAG, "Cannot calibrate - not connected or characteristic unavailable");
            if (serialDataListener != null) {
                serialDataListener.addSerialData("ERROR: Cannot calibrate - not connected");
            }
            return;
        }

        try {
            // Command code for calibration (example: 0xC5 = calibration command)
            byte[] calibrateCommand = new byte[]{(byte) 0xC5};
            writeCharacteristic(flexCharacteristic, calibrateCommand);

            Log.d(TAG, "Sent calibration command to ESP32");
            if (serialDataListener != null) {
                serialDataListener.addSerialData("Calibration command sent to ESP32");
            }

            // Reset sensor values temporarily during calibration
            Arrays.fill(lastFlexValues, 0);
            if (flexDataListener != null) {
                flexDataListener.updateSensorData(lastFlexValues);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending calibration command: " + e.getMessage());
            if (serialDataListener != null) {
                serialDataListener.addSerialData("ERROR: Calibration failed: " + e.getMessage());
            }
        }
    }

    /**
     * Write data to a characteristic
     */
    @SuppressLint("MissingPermission")
    private void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        if (bluetoothGatt != null && characteristic != null) {
            try {
                characteristic.setValue(data);
                bluetoothGatt.writeCharacteristic(characteristic);
                Log.d(TAG, "Write command sent to characteristic");
            } catch (Exception e) {
                Log.e(TAG, "Error writing to characteristic: " + e.getMessage());
            }
        }
    }

    /**
     * Setup BLE service discovery
     */
    @SuppressLint("MissingPermission")
    private void discoverServices() {
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized. Cannot discover services.");
            return;
        }

        Log.d(TAG, "Starting service discovery");
        boolean started = bluetoothGatt.discoverServices();

        if (started) {
            Log.d(TAG, "Service discovery initiated successfully");
            runOnUiThread(() -> {
                connectionStatus.setText("Discovering services...");
                connectionStatus.setTextColor(Color.YELLOW);
            });
        } else {
            Log.e(TAG, "Service discovery failed to initiate");
            runOnUiThread(() -> {
                connectionStatus.setText("Service discovery failed");
                connectionStatus.setTextColor(Color.RED);
            });
        }
    }

    /**
     * Close the BLE connection
     */
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        
        Log.d(TAG, "Closing GATT connection");
        
        // Only disconnect if we're not keeping the connection alive
        if (!keepConnectionAlive) {
            bluetoothGatt.close();
            bluetoothGatt = null;
            connectionState = STATE_DISCONNECTED;
            Log.d(TAG, "BLE connection closed");
        } else {
            Log.d(TAG, "Keeping BLE connection alive between activities");
        }
    }

    /**
     * Launch the ESP32 Activity
     */
    public void launchESP32Activity() {
        // Only proceed if we have a valid context
        if (getApplicationContext() == null) {
            Log.e(TAG, "Cannot launch ESP32SensorActivity - context is null");
            return;
        }
        
        try {
            Log.d(TAG, "Launching ESP32SensorActivity");

            // Set flag to keep connection alive during activity transition
            keepConnectionAlive = true;
            
            // Create intent using explicit class name to avoid ambiguity
            Intent intent = new Intent(getApplicationContext(), ESP32SensorActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch ESP32SensorActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error launching sensor activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        
        // Only disconnect if we're explicitly exiting the app
        keepConnectionAlive = false;
        close();
    }

    /**
     * Get the best accelerometer value from multiple candidates
     * Looks for values in reasonable range (-10 to +10)
     */
    private float getBestAccelValue(float[] candidates) {
        // First try to find values in the expected range for the accelerometer (-4 to +4)
        for (float val : candidates) {
            if (Math.abs(val) <= 4.0f && Math.abs(val) > 0.01f) {
                return val;
            }
        }
        
        // If none found in ideal range, try a wider range
        for (float val : candidates) {
            if (Math.abs(val) <= 10.0f && Math.abs(val) > 0.01f) {
                return val;
            }
        }
        
        // If still no reasonable value, return the first non-zero value
        for (float val : candidates) {
            if (Math.abs(val) > 0.01f) {
                return val;
            }
        }
        
        // If all values are essentially zero, return 0
        return 0.0f;
    }

    /**
     * Update the UI with the detected letter
     */
    private void updateLetterUI(String letter) {
        runOnUiThread(() -> {
            if (predictedLetterTextView != null) {
                predictedLetterTextView.setText("Detected Letter: " + letter);
                
                // Only change to green for valid letters (A-Z)
                boolean isConfidentLetter = letter.length() == 1 && 
                                          letter.charAt(0) >= 'A' && 
                                          letter.charAt(0) <= 'Z';
                
                // Update all UI elements with appropriate color
                int textColor = isConfidentLetter ? Color.GREEN : Color.RED;
                predictedLetterTextView.setTextColor(textColor);
                
                // Also update other UI elements if we have a confident letter
                if (translationResult != null) {
                    translationResult.setTextColor(textColor);
                }
                
                // Return to normal red color after a delay
                if (isConfidentLetter) {
                    handler.postDelayed(() -> {
                        if (predictedLetterTextView != null) {
                            predictedLetterTextView.setTextColor(Color.RED);
                        }
                        if (translationResult != null) {
                            translationResult.setTextColor(Color.RED);
                        }
                    }, 2000);
                }
                
                // Keep the text styling consistent
                predictedLetterTextView.setTypeface(Typeface.DEFAULT_BOLD);
                predictedLetterTextView.setTextSize(18);
            }
        });
    }
}
