/**
 * Android BLE Bridge
 * This script provides a bridge between the Web Bluetooth API and Android native BLE
 * functionality when running in WebView for the SignFlex glove
 */

class AndroidBLEBridge {
  constructor() {
    this.isAndroidApp = localStorage.getItem('IS_ANDROID_APP') === 'true';
    this.hasNativeBLE = typeof AndroidBLEInterface !== 'undefined';
    this.listeners = [];
    this.connected = false;
    this.deviceName = '';
    
    // ESP32 UUIDs for SignFlex
    this.ESP32_SERVICE_UUID = '4fafc201-1fb5-459e-8fcc-c5c9c331914b';
    this.FLEX_CHARACTERISTIC_UUID = 'beb5483e-36e1-4688-b7f5-ea07361b26ac';
    this.BATTERY_CHARACTERISTIC_UUID = 'beb5483e-36e1-4688-b7f5-ea07361b26ad';
    this.ACCEL_CHARACTERISTIC_UUID = 'beb5483e-36e1-4688-b7f5-ea07361b26ae';
    
    // Data storage
    this.flexValues = [0, 0, 0, 0, 0]; // Thumb, Index, Middle, Ring, Pinky
    this.accelValues = [0, 0, 0]; // X, Y, Z
    this.batteryLevel = 0;
    this.batteryVoltage = 0;
    this.lastRecognizedGesture = '';
    
    console.log('SignFlex AndroidBLEBridge initialized, isAndroidApp:', this.isAndroidApp, 'hasNativeBLE:', this.hasNativeBLE);
    
    // Setup Android callbacks if in Android app
    if (this.isAndroidApp && this.hasNativeBLE) {
      this.setupAndroidCallbacks();
    }
  }
  
  // Check if running in Android app with native BLE
  isRunningInAndroidApp() {
    return this.isAndroidApp && this.hasNativeBLE;
  }
  
  // Setup callbacks from Android to JS
  setupAndroidCallbacks() {
    // Define global callback functions for Android to call
    window.onBLEConnected = (deviceName) => {
      console.log('SignFlex BLE connected to:', deviceName);
      this.connected = true;
      this.deviceName = deviceName;
      this._notifyListeners('connected', { deviceName });
      
      // Update UI elements
      const deviceStatus = document.getElementById('device-status');
      if (deviceStatus) {
        deviceStatus.textContent = `Connected to ${deviceName}`;
        deviceStatus.className = 'status connected';
      }
      
      // Show sensor data sections
      const batteryInfo = document.getElementById('battery-info');
      const flexReadings = document.getElementById('flex-readings');
      if (batteryInfo) batteryInfo.classList.remove('hidden');
      if (flexReadings) flexReadings.classList.remove('hidden');
    };
    
    window.onBLEDisconnected = () => {
      console.log('SignFlex BLE disconnected');
      this.connected = false;
      this._notifyListeners('disconnected', {});
      
      // Update UI elements
      const deviceStatus = document.getElementById('device-status');
      if (deviceStatus) {
        deviceStatus.textContent = 'Disconnected';
        deviceStatus.className = 'status';
      }
      
      // Hide sensor data sections
      const batteryInfo = document.getElementById('battery-info');
      const flexReadings = document.getElementById('flex-readings');
      if (batteryInfo) batteryInfo.classList.add('hidden');
      if (flexReadings) flexReadings.classList.add('hidden');
    };
    
    window.onBLEFlexData = (flexDataStr) => {
      try {
        console.log('SignFlex BLE flex data:', flexDataStr);
        // Parse array from string
        const flexData = JSON.parse(flexDataStr.replace(/\[|\]/g, '').split(',').map(Number));
        
        // Store flex values
        if (flexData.length === 5) {
          this.flexValues = flexData;
          
          // Update UI
          const flexValue = document.getElementById('flex-value');
          if (flexValue) {
            flexValue.textContent = `[${this.flexValues.map(v => Math.round(v)).join(', ')}]`;
          }
          
          this._notifyListeners('flexDataUpdate', { values: this.flexValues });
        }
      } catch (e) {
        console.error('Error parsing flex data:', e);
      }
    };
    
    window.onBLEAccelData = (accelDataStr) => {
      try {
        console.log('SignFlex BLE accelerometer data:', accelDataStr);
        // Parse array from string
        const accelData = JSON.parse(accelDataStr.replace(/\[|\]/g, '').split(',').map(Number));
        
        // Store accelerometer values
        if (accelData.length === 3) {
          this.accelValues = accelData;
          
          // Add accelerometer display section if it doesn't exist
          const bleScreen = document.getElementById('ble-screen');
          let accelReadings = document.getElementById('accel-readings');
          
          if (bleScreen && !accelReadings) {
            const accelSection = document.createElement('div');
            accelSection.id = 'accel-readings';
            accelSection.className = 'device-info-section';
            accelSection.innerHTML = `
              <h3>Accelerometer Data</h3>
              <div class="info-grid">
                <div class="info-label">Values (X,Y,Z):</div>
                <div id="accel-value" class="info-value">--</div>
              </div>
            `;
            bleScreen.appendChild(accelSection);
            accelReadings = accelSection;
          }
          
          // Update UI
          const accelValue = document.getElementById('accel-value');
          if (accelValue) {
            accelValue.textContent = `[${this.accelValues.map(v => v.toFixed(2)).join(', ')}]`;
          }
          
          this._notifyListeners('accelDataUpdate', { values: this.accelValues });
        }
      } catch (e) {
        console.error('Error parsing accelerometer data:', e);
      }
    };
    
    window.onBLEBatteryData = (level, voltage) => {
      console.log('SignFlex BLE battery data - Level:', level, '%, Voltage:', voltage, 'V');
      this.batteryLevel = parseInt(level);
      this.batteryVoltage = parseFloat(voltage);
      
      // Update UI elements
      const batteryLevel = document.getElementById('battery-level');
      const batteryVoltage = document.getElementById('battery-voltage');
      if (batteryLevel) batteryLevel.textContent = `${this.batteryLevel}%`;
      if (batteryVoltage) batteryVoltage.textContent = `${this.batteryVoltage.toFixed(2)}V`;
      
      this._notifyListeners('batteryUpdate', { 
        level: this.batteryLevel, 
        voltage: this.batteryVoltage
      });
    };
    
    window.onASLGestureRecognized = (gesture) => {
      console.log('SignFlex ASL gesture recognized:', gesture);
      this.lastRecognizedGesture = gesture;
      
      // Create gesture display if needed
      const bleScreen = document.getElementById('ble-screen');
      let gestureDisplay = document.getElementById('gesture-recognition');
      
      if (bleScreen && !gestureDisplay) {
        const gestureSection = document.createElement('div');
        gestureSection.id = 'gesture-recognition';
        gestureSection.className = 'device-info-section';
        gestureSection.innerHTML = `
          <h3>ASL Gesture Recognition</h3>
          <div class="info-grid">
            <div class="info-label">Recognized Sign:</div>
            <div id="recognized-gesture" class="info-value gesture">--</div>
          </div>
        `;
        bleScreen.appendChild(gestureSection);
        gestureDisplay = gestureSection;
      }
      
      // Update UI
      const recognizedGesture = document.getElementById('recognized-gesture');
      if (recognizedGesture) {
        recognizedGesture.textContent = this.lastRecognizedGesture;
        recognizedGesture.className = 'info-value gesture highlight';
        setTimeout(() => {
          recognizedGesture.className = 'info-value gesture';
        }, 1000);
      }
      
      this._notifyListeners('gestureRecognized', { gesture });
    };
    
    window.onBLEError = (error) => {
      console.error('SignFlex BLE error:', error);
      this._notifyListeners('error', { message: error });
      
      // Update UI
      const deviceStatus = document.getElementById('device-status');
      if (deviceStatus) {
        deviceStatus.textContent = `Error: ${error}`;
        deviceStatus.className = 'status error';
      }
    };
    
    console.log('SignFlex BLE callbacks registered');
  }
  
  // Scan for and connect to ESP32 device
  async connect() {
    console.log('Attempting to connect to SignFlex ESP32 glove...');
    
    if (this.isRunningInAndroidApp()) {
      // Use native Android BLE
      try {
        console.log('Using Android native BLE...');
        AndroidBLEInterface.scanForDevices();
        return true; // Actual result will come through callbacks
      } catch (error) {
        console.error('Error using Android native BLE:', error);
        throw new Error('Failed to connect using Android native BLE: ' + error.message);
      }
    } else {
      // Use Web Bluetooth API if available
      if (!('bluetooth' in navigator)) {
        throw new Error('Web Bluetooth API is not supported in this browser');
      }
      
      try {
        console.log('Using Web Bluetooth API...');
        // Request device with ESP32 service UUID
        const device = await navigator.bluetooth.requestDevice({
          filters: [{ services: [this.ESP32_SERVICE_UUID] }]
        });
        
        console.log('Device selected:', device.name);
        this.deviceName = device.name || 'SignFlex Glove';
        
        // Add event listener for disconnection
        device.addEventListener('gattserverdisconnected', () => {
          console.log('Device disconnected');
          this.connected = false;
          this._notifyListeners('disconnected', {});
        });
        
        // Connect to GATT server
        console.log('Connecting to GATT server...');
        const server = await device.gatt.connect();
        
        // Get the ESP32 service
        console.log('Getting primary service...');
        const service = await server.getPrimaryService(this.ESP32_SERVICE_UUID);
        
        // Get and subscribe to flex sensor characteristic
        console.log('Getting flex sensor characteristic...');
        const flexCharacteristic = await service.getCharacteristic(this.FLEX_CHARACTERISTIC_UUID);
        await flexCharacteristic.startNotifications();
        flexCharacteristic.addEventListener('characteristicvaluechanged', (event) => {
          try {
            const value = event.target.value;
            const dataView = new DataView(value.buffer);
            // Parse 5 flex sensor values (2 bytes each)
            const flexValues = [];
            for (let i = 0; i < 5; i++) {
              flexValues.push(dataView.getUint16(i * 2, true));
            }
            this.flexValues = flexValues;
            this._notifyListeners('flexDataUpdate', { values: flexValues });
          } catch (error) {
            console.error('Error parsing flex sensor data:', error);
          }
        });
        
        // Get and subscribe to accelerometer characteristic
        try {
          console.log('Getting accelerometer characteristic...');
          const accelCharacteristic = await service.getCharacteristic(this.ACCEL_CHARACTERISTIC_UUID);
          await accelCharacteristic.startNotifications();
          accelCharacteristic.addEventListener('characteristicvaluechanged', (event) => {
            try {
              const value = event.target.value;
              const dataView = new DataView(value.buffer);
              // Parse 3 accelerometer values (2 bytes each)
              const accelValues = [];
              for (let i = 0; i < 3; i++) {
                accelValues.push(dataView.getInt16(i * 2, true) / 16384.0); // Scale to G units
              }
              this.accelValues = accelValues;
              this._notifyListeners('accelDataUpdate', { values: accelValues });
            } catch (error) {
              console.error('Error parsing accelerometer data:', error);
            }
          });
        } catch (error) {
          console.warn('Accelerometer characteristic not available:', error);
        }
        
        // Get and subscribe to battery characteristic
        try {
          console.log('Getting battery characteristic...');
          const batteryCharacteristic = await service.getCharacteristic(this.BATTERY_CHARACTERISTIC_UUID);
          await batteryCharacteristic.startNotifications();
          batteryCharacteristic.addEventListener('characteristicvaluechanged', (event) => {
            try {
              const value = event.target.value;
              const dataView = new DataView(value.buffer);
              const level = dataView.getUint8(0);
              const voltage = dataView.getFloat32(1, true);
              this.batteryLevel = level;
              this.batteryVoltage = voltage;
              this._notifyListeners('batteryUpdate', { level, voltage });
            } catch (error) {
              console.error('Error parsing battery data:', error);
            }
          });
        } catch (error) {
          console.warn('Battery characteristic not available:', error);
        }
        
        console.log('SignFlex BLE connection successful!');
        this.connected = true;
        
        this._notifyListeners('connected', { deviceName: this.deviceName });
        
        return true;
      } catch (error) {
        console.error('BLE connection error:', error);
        this.connected = false;
        throw error;
      }
    }
  }
  
  // Disconnect from device
  async disconnect() {
    if (this.isRunningInAndroidApp()) {
      try {
        AndroidBLEInterface.disconnect();
        return true;
      } catch (error) {
        console.error('Error disconnecting with Android native BLE:', error);
        throw error;
      }
    } else {
      // Web BLE disconnection is handled by event listener
      this._notifyListeners('disconnected', {});
      return true;
    }
  }
  
  // Calibrate the flex sensors
  calibrateFlexSensors() {
    if (this.isRunningInAndroidApp()) {
      try {
        AndroidBLEInterface.calibrateFlexSensors();
        return true;
      } catch (error) {
        console.error('Error calibrating flex sensors:', error);
        return false;
      }
    } else {
      console.warn('Flex sensor calibration only available in Android app');
      return false;
    }
  }
  
  // Process flex sensor data to recognize gestures
  processData() {
    if (this.isRunningInAndroidApp()) {
      try {
        AndroidBLEInterface.processFlexSensorData();
        return true;
      } catch (error) {
        console.error('Error processing flex sensor data:', error);
        return false;
      }
    } else {
      console.warn('Flex sensor processing only available in Android app');
      return false;
    }
  }
  
  // Get the current flex sensor values
  getFlexValues() {
    return [...this.flexValues];
  }
  
  // Get the current accelerometer values
  getAccelValues() {
    return [...this.accelValues];
  }
  
  // Get the current battery information
  getBatteryInfo() {
    return {
      level: this.batteryLevel,
      voltage: this.batteryVoltage
    };
  }
  
  // Add listener for events
  addListener(event, callback) {
    this.listeners.push({ event, callback });
    return () => {
      this.listeners = this.listeners.filter(listener => 
        listener.event !== event || listener.callback !== callback
      );
    };
  }
  
  // Notify all listeners of an event
  _notifyListeners(event, data) {
    this.listeners
      .filter(listener => listener.event === event)
      .forEach(listener => {
        try {
          listener.callback(data);
        } catch (e) {
          console.error(`Error in listener for ${event}:`, e);
        }
      });
  }
}

// Create and export a singleton instance
const androidBLEBridge = new AndroidBLEBridge();
window.androidBLEBridge = androidBLEBridge;

// Add event listener to scan button
document.addEventListener('DOMContentLoaded', () => {
  const scanBtn = document.getElementById('scan-btn');
  if (scanBtn) {
    scanBtn.addEventListener('click', () => {
      androidBLEBridge.connect();
    });
  }
});
