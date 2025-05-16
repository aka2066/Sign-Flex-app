import React, { useState, useEffect } from 'react';
import bleManager from './ble-manager';
import './ble-screen.css';

const BLEScreen = () => {
  const [isSupported, setIsSupported] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [isScanning, setIsScanning] = useState(false);
  const [ledState, setLedState] = useState(false);
  const [temperature, setTemperature] = useState(null);
  const [accelerometer, setAccelerometer] = useState({ x: 0, y: 0, z: 0 });
  const [gyroscope, setGyroscope] = useState({ x: 0, y: 0, z: 0 });
  const [error, setError] = useState(null);

  // Check if Web Bluetooth is supported
  useEffect(() => {
    setIsSupported(bleManager.isSupported());
  }, []);

  // Setup event listeners
  useEffect(() => {
    if (isSupported) {
      const connectionListener = bleManager.addListener('connectionChange', ({ connected }) => {
        setIsConnected(connected);
        if (!connected) {
          // Reset sensor data when disconnected
          setTemperature(null);
          setAccelerometer({ x: 0, y: 0, z: 0 });
          setGyroscope({ x: 0, y: 0, z: 0 });
        }
      });
      
      const tempListener = bleManager.addListener('temperatureUpdate', ({ temperature }) => {
        setTemperature(temperature);
      });
      
      const accelListener = bleManager.addListener('accelerometerUpdate', (data) => {
        setAccelerometer(data);
      });
      
      const gyroListener = bleManager.addListener('gyroscopeUpdate', (data) => {
        setGyroscope(data);
      });
      
      // Cleanup listeners on unmount
      return () => {
        connectionListener();
        tempListener();
        accelListener();
        gyroListener();
      };
    }
  }, [isSupported]);

  // Start scanning for devices
  const handleStartScan = async () => {
    try {
      setError(null);
      setIsScanning(true);
      await bleManager.startScan();
      setIsScanning(false);
    } catch (err) {
      setError(err.message);
      setIsScanning(false);
    }
  };

  // Connect to selected device
  const handleConnect = async () => {
    try {
      setError(null);
      await bleManager.connectToDevice();
      // Subscribe to sensor data after connection
      await bleManager.subscribeToTemperature();
      await bleManager.subscribeToAccelerometer();
      await bleManager.subscribeToGyroscope();
    } catch (err) {
      setError(err.message);
    }
  };

  // Disconnect from device
  const handleDisconnect = async () => {
    try {
      setError(null);
      await bleManager.disconnect();
    } catch (err) {
      setError(err.message);
    }
  };

  // Toggle LED state
  const handleToggleLED = async () => {
    try {
      setError(null);
      const newState = !ledState;
      await bleManager.toggleLED(newState);
      setLedState(newState);
    } catch (err) {
      setError(err.message);
    }
  };

  if (!isSupported) {
    return (
      <div className="ble-screen not-supported">
        <h2>BLE Not Supported</h2>
        <p>Your browser does not support Web Bluetooth API.</p>
        <p>Please use Chrome on Android or desktop for BLE functionality.</p>
      </div>
    );
  }

  return (
    <div className="ble-screen">
      <h2>Bluetooth Low Energy (BLE)</h2>
      
      <div className="connection-status">
        <div className={`status-indicator ${isConnected ? 'connected' : 'disconnected'}`}></div>
        <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
      </div>
      
      <div className="control-buttons">
        {!isConnected ? (
          <>
            <button 
              className="scan-button" 
              onClick={handleStartScan} 
              disabled={isScanning}
            >
              {isScanning ? 'Scanning...' : 'Scan for Devices'}
            </button>
            <button 
              className="connect-button" 
              onClick={handleConnect} 
              disabled={!bleManager.device || isConnected}
            >
              Connect
            </button>
          </>
        ) : (
          <>
            <button 
              className="disconnect-button" 
              onClick={handleDisconnect}
            >
              Disconnect
            </button>
            <button 
              className={`led-button ${ledState ? 'led-on' : 'led-off'}`} 
              onClick={handleToggleLED}
            >
              {ledState ? 'Turn LED OFF' : 'Turn LED ON'}
            </button>
          </>
        )}
      </div>
      
      {error && (
        <div className="error-message">
          <p>Error: {error}</p>
        </div>
      )}
      
      <div className="sensor-data">
        <h3>Sensor Data</h3>
        
        <div className="sensor-card">
          <h4>Temperature</h4>
          <p className="sensor-value">
            {temperature !== null ? `${temperature.toFixed(1)}°C` : '—'}
          </p>
        </div>
        
        <div className="sensor-card">
          <h4>Accelerometer</h4>
          <p className="sensor-value">
            X: {accelerometer.x.toFixed(2)} | 
            Y: {accelerometer.y.toFixed(2)} | 
            Z: {accelerometer.z.toFixed(2)}
          </p>
        </div>
        
        <div className="sensor-card">
          <h4>Gyroscope</h4>
          <p className="sensor-value">
            X: {gyroscope.x.toFixed(2)} | 
            Y: {gyroscope.y.toFixed(2)} | 
            Z: {gyroscope.z.toFixed(2)}
          </p>
        </div>
      </div>

      <div className="info-box">
        <h3>How to Use</h3>
        <ol>
          <li>Click "Scan for Devices" to search for ESP32 devices</li>
          <li>Select your ESP32 device from the browser dialog</li>
          <li>Click "Connect" to establish a connection</li>
          <li>Once connected, you can toggle the LED and view sensor data</li>
        </ol>

        <h3>Requirements</h3>
        <ul>
          <li>Chrome browser (Android or Desktop)</li>
          <li>ESP32 with BLE service UUID: 4fafc201-1fb5-459e-8fcc-c5c9c331914b</li>
          <li>Bluetooth enabled on your device</li>
        </ul>
      </div>
    </div>
  );
};

export default BLEScreen;
