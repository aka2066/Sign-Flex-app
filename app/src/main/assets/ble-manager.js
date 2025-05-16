/**
 * BLE Manager for SignFlex
 * Handles connection to ESP32 devices and data subscription
 */

class BLEManager {
  constructor(serviceUUID) {
    this.ESP32_SERVICE_UUID = serviceUUID || '4fafc201-1fb5-459e-8fcc-c5c9c331914b';
    this.FLEX_CHARACTERISTIC_UUID = 'beb5483e-36e1-4688-b7f5-ea07361b26ac';
    this.BATTERY_CHARACTERISTIC_UUID = 'beb5483e-36e1-4688-b7f5-ea07361b26ad';
    
    this.device = null;
    this.server = null;
    this.service = null;
    this.flexCharacteristic = null;
    this.batteryCharacteristic = null;
    this.flexCallback = null;
    this.batteryCallback = null;
    this.isConnected = false;
    
    this.listeners = [];
    
    // Check if Web Bluetooth is supported
    this.supported = 'bluetooth' in navigator;
    
    console.log('BLE Manager initialized, Web Bluetooth supported:', this.supported);
  }
  
  // Check if Web Bluetooth is supported
  isSupported() {
    return this.supported;
  }
  
  // Connect to ESP32 device
  async connect() {
    console.log('Attempting to connect to ESP32 device...');
    
    if (!this.supported) {
      throw new Error('Web Bluetooth is not supported in this browser');
    }
    
    try {
      // Request device with ESP32 service UUID
      console.log('Requesting device with service UUID:', this.ESP32_SERVICE_UUID);
      this.device = await navigator.bluetooth.requestDevice({
        filters: [{ services: [this.ESP32_SERVICE_UUID] }]
      });
      
      console.log('Device selected:', this.device.name || 'unnamed device');
      
      // Add event listener for disconnection
      this.device.addEventListener('gattserverdisconnected', this.onDisconnected.bind(this));
      
      // Connect to GATT server
      console.log('Connecting to GATT server...');
      this.server = await this.device.gatt.connect();
      
      // Get the ESP32 service
      console.log('Getting primary service...');
      this.service = await this.server.getPrimaryService(this.ESP32_SERVICE_UUID);
      
      // Get the flex sensor characteristic
      console.log('Getting flex sensor characteristic...');
      this.flexCharacteristic = await this.service.getCharacteristic(this.FLEX_CHARACTERISTIC_UUID);
      
      // Get the battery characteristic
      console.log('Getting battery characteristic...');
      this.batteryCharacteristic = await this.service.getCharacteristic(this.BATTERY_CHARACTERISTIC_UUID);
      
      console.log('BLE connection successful!');
      this.isConnected = true;
      
      // Notify listeners
      this._notifyListeners('connected', { deviceName: this.device.name || 'ESP32 Device' });
      
      return true;
    } catch (error) {
      console.error('BLE connection error:', error);
      this.isConnected = false;
      throw error;
    }
  }
  
  // Disconnect from device
  async disconnect() {
    if (this.device && this.device.gatt.connected) {
      console.log('Disconnecting from device...');
      await this.device.gatt.disconnect();
    }
    
    this.isConnected = false;
    this._notifyListeners('disconnected', {});
    
    console.log('Disconnected from device');
    return true;
  }
  
  // Handle disconnection event
  onDisconnected(event) {
    console.log('Device disconnected');
    this.isConnected = false;
    this._notifyListeners('disconnected', {});
  }
  
  // Subscribe to flex sensor updates
  async subscribeToFlexSensor(callback) {
    if (!this.flexCharacteristic) {
      throw new Error('Flex sensor characteristic not available');
    }
    
    this.flexCallback = callback;
    
    try {
      console.log('Subscribing to flex sensor notifications...');
      
      // Add event listener for characteristic value changes
      this.flexCharacteristic.addEventListener('characteristicvaluechanged', this.handleFlexSensorData.bind(this));
      
      // Start notifications
      await this.flexCharacteristic.startNotifications();
      
      console.log('Subscribed to flex sensor notifications');
      return true;
    } catch (error) {
      console.error('Error subscribing to flex sensor:', error);
      throw error;
    }
  }
  
  // Subscribe to battery updates
  async subscribeToBatteryUpdates(callback) {
    if (!this.batteryCharacteristic) {
      throw new Error('Battery characteristic not available');
    }
    
    this.batteryCallback = callback;
    
    try {
      console.log('Subscribing to battery notifications...');
      
      // Add event listener for characteristic value changes
      this.batteryCharacteristic.addEventListener('characteristicvaluechanged', this.handleBatteryData.bind(this));
      
      // Start notifications
      await this.batteryCharacteristic.startNotifications();
      
      console.log('Subscribed to battery notifications');
      return true;
    } catch (error) {
      console.error('Error subscribing to battery:', error);
      throw error;
    }
  }
  
  // Handle flex sensor data
  handleFlexSensorData(event) {
    try {
      const value = event.target.value;
      const dataView = value.buffer ? new DataView(value.buffer) : new DataView(value);
      
      // Get the flex value (assuming it's a Uint16 at offset 0)
      const flexValue = dataView.getUint16(0, true);
      
      console.log('Flex sensor value:', flexValue);
      
      // Call the callback if set
      if (this.flexCallback) {
        this.flexCallback(flexValue);
      }
      
      // Notify listeners
      this._notifyListeners('flexDataUpdate', { value: flexValue });
    } catch (error) {
      console.error('Error handling flex sensor data:', error);
    }
  }
  
  // Handle battery data
  handleBatteryData(event) {
    try {
      const value = event.target.value;
      const dataView = value.buffer ? new DataView(value.buffer) : new DataView(value);
      
      // Get battery level (0-100%) and voltage (float)
      const level = dataView.getUint8(0); // First byte is percentage
      const voltage = dataView.getFloat32(1, true); // Next 4 bytes are voltage as float
      
      console.log('Battery update - Level:', level, '%, Voltage:', voltage, 'V');
      
      // Call the callback if set
      if (this.batteryCallback) {
        this.batteryCallback({ level, voltage });
      }
      
      // Notify listeners
      this._notifyListeners('batteryUpdate', { level, voltage });
    } catch (error) {
      console.error('Error handling battery data:', error);
    }
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
export default BLEManager;
