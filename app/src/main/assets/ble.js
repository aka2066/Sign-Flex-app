// SignFlex - Simulated BLE for ESP32
// This is a simulation of BLE functionality for demonstration purposes

// DOM elements
const homeScreen = document.getElementById('home-screen');
const dataScreen = document.getElementById('data-screen');
const connectBtn = document.getElementById('ble-connect-btn');
const disconnectBtn = document.getElementById('disconnect-btn');
const ledToggleBtn = document.getElementById('led-toggle-btn');
const requestDataBtn = document.getElementById('request-data-btn');
const updateRateSlider = document.getElementById('update-rate');
const updateRateValue = document.getElementById('update-rate-value');
const connectionStatus = document.getElementById('connection-status');

// Data elements
const temperatureValue = document.getElementById('temperature-value');
const accelerometerValue = document.getElementById('accelerometer-value');
const gyroscopeValue = document.getElementById('gyroscope-value');
const customValue = document.getElementById('custom-value');
const dataChart = document.getElementById('data-chart');

// BLE simulation state
let isConnected = false;
let isLedOn = false;
let updateInterval = 1000;
let updateIntervalId = null;
let temperatureData = [];
let timeLabels = [];

// Initialize Chart.js
const chart = new Chart(dataChart, {
    type: 'line',
    data: {
        labels: Array(6).fill(''),
        datasets: [{
            label: 'Temperature (°C)',
            data: Array(6).fill(25),
            backgroundColor: 'rgba(98, 0, 238, 0.2)',
            borderColor: 'rgba(98, 0, 238, 1)',
            borderWidth: 2,
            tension: 0.4
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: false,
                min: 15,
                max: 35
            }
        },
        animation: {
            duration: 500
        }
    }
});

// Simulated scanning for ESP32 devices
function scanForDevices() {
    connectionStatus.textContent = 'Scanning...';
    
    // Simulate scanning delay
    setTimeout(() => {
        // Simulate a found device
        const foundDevice = {
            id: 'esp32-signflex-01',
            name: 'ESP32_SignFlex'
        };
        
        // Connect to the device
        connectToDevice(foundDevice);
    }, 2000);
}

// Simulated connection to ESP32 device
function connectToDevice(device) {
    connectionStatus.textContent = `Connecting to ${device.name}...`;
    
    // Simulate connection delay
    setTimeout(() => {
        isConnected = true;
        connectionStatus.textContent = `Connected to ${device.name}`;
        
        // Show data screen
        homeScreen.classList.add('hidden');
        dataScreen.classList.remove('hidden');
        
        // Start simulated data updates
        startDataUpdates();
    }, 1500);
}

// Disconnect from device
function disconnectFromDevice() {
    // Stop data updates
    if (updateIntervalId) {
        clearInterval(updateIntervalId);
        updateIntervalId = null;
    }
    
    isConnected = false;
    connectionStatus.textContent = 'Not connected';
    
    // Reset data values
    temperatureValue.textContent = '--';
    accelerometerValue.textContent = '--';
    gyroscopeValue.textContent = '--';
    customValue.textContent = '--';
    
    // Show home screen
    homeScreen.classList.remove('hidden');
    dataScreen.classList.add('hidden');
}

// Toggle LED state
function toggleLed() {
    isLedOn = !isLedOn;
    ledToggleBtn.textContent = isLedOn ? 'Turn LED OFF' : 'Turn LED ON';
    
    // Visual feedback
    if (isLedOn) {
        ledToggleBtn.classList.add('active-led');
    } else {
        ledToggleBtn.classList.remove('active-led');
    }
}

// Request a data update
function requestData() {
    // Simulate immediate data update
    updateSensorData();
    
    // Visual feedback
    requestDataBtn.classList.add('active-request');
    setTimeout(() => {
        requestDataBtn.classList.remove('active-request');
    }, 300);
}

// Update the data update interval
function updateRate() {
    updateInterval = parseInt(updateRateSlider.value);
    updateRateValue.textContent = updateInterval + 'ms';
    
    // Restart data updates with new interval
    if (isConnected) {
        startDataUpdates();
    }
}

// Start simulated data updates
function startDataUpdates() {
    // Clear existing interval
    if (updateIntervalId) {
        clearInterval(updateIntervalId);
    }
    
    // Get initial temperature value from 20-30°C
    let temp = 25 + (Math.random() * 5 - 2.5);
    
    // Set up new interval for data updates
    updateIntervalId = setInterval(() => {
        // Update temperature with small random changes
        temp = temp + (Math.random() * 1 - 0.5);
        
        // Keep temperature in reasonable range
        if (temp < 15) temp = 15;
        if (temp > 35) temp = 35;
        
        // Set sensor values
        temperatureValue.textContent = temp.toFixed(1) + '°C';
        
        // Random accelerometer values
        const accelX = Math.floor(Math.random() * 2000 - 1000);
        const accelY = Math.floor(Math.random() * 2000 - 1000);
        const accelZ = Math.floor(Math.random() * 2000 - 1000);
        accelerometerValue.textContent = `X: ${accelX}, Y: ${accelY}, Z: ${accelZ}`;
        
        // Random gyroscope values
        const gyroX = Math.floor(Math.random() * 500 - 250);
        const gyroY = Math.floor(Math.random() * 500 - 250);
        const gyroZ = Math.floor(Math.random() * 500 - 250);
        gyroscopeValue.textContent = `X: ${gyroX}, Y: ${gyroY}, Z: ${gyroZ}`;
        
        // Random custom value
        customValue.textContent = `Value: ${Math.floor(Math.random() * 100)}`;
        
        // Update the chart data
        updateChartData(temp);
    }, updateInterval);
}

// Update sensor data (called when requesting data)
function updateSensorData() {
    if (!isConnected) return;
    
    // Get current temperature value
    let currentTemp = parseFloat(temperatureValue.textContent);
    if (isNaN(currentTemp)) currentTemp = 25;
    
    // Add small variation
    currentTemp = currentTemp + (Math.random() * 2 - 1);
    
    // Keep temperature in reasonable range
    if (currentTemp < 15) currentTemp = 15;
    if (currentTemp > 35) currentTemp = 35;
    
    // Set sensor values
    temperatureValue.textContent = currentTemp.toFixed(1) + '°C';
    
    // Random accelerometer values
    const accelX = Math.floor(Math.random() * 2000 - 1000);
    const accelY = Math.floor(Math.random() * 2000 - 1000);
    const accelZ = Math.floor(Math.random() * 2000 - 1000);
    accelerometerValue.textContent = `X: ${accelX}, Y: ${accelY}, Z: ${accelZ}`;
    
    // Random gyroscope values
    const gyroX = Math.floor(Math.random() * 500 - 250);
    const gyroY = Math.floor(Math.random() * 500 - 250);
    const gyroZ = Math.floor(Math.random() * 500 - 250);
    gyroscopeValue.textContent = `X: ${gyroX}, Y: ${gyroY}, Z: ${gyroZ}`;
    
    // Random custom value
    customValue.textContent = `Value: ${Math.floor(Math.random() * 100)}`;
    
    // Update the chart
    updateChartData(currentTemp);
}

// Update chart data
function updateChartData(tempValue) {
    // Get current time
    const now = new Date();
    const timeString = now.getHours() + ':' + String(now.getMinutes()).padStart(2, '0') + ':' + String(now.getSeconds()).padStart(2, '0');
    
    // Update chart data
    chart.data.labels.push(timeString);
    chart.data.datasets[0].data.push(tempValue);
    
    // Remove oldest data point if we have more than 6
    if (chart.data.labels.length > 6) {
        chart.data.labels.shift();
        chart.data.datasets[0].data.shift();
    }
    
    // Update chart
    chart.update();
}

// Setup event listeners
function setupEventListeners() {
    // Connect button
    connectBtn.addEventListener('click', scanForDevices);
    
    // Disconnect button
    disconnectBtn.addEventListener('click', disconnectFromDevice);
    
    // LED toggle button
    ledToggleBtn.addEventListener('click', toggleLed);
    
    // Request data button
    requestDataBtn.addEventListener('click', requestData);
    
    // Update rate slider
    updateRateSlider.addEventListener('input', updateRate);
    
    // Show initial update rate
    updateRateValue.textContent = updateRateSlider.value + 'ms';
}

// Initialize the application
function init() {
    setupEventListeners();
    console.log('SignFlex BLE simulation initialized');
}

// Start the application when the DOM is fully loaded
document.addEventListener('DOMContentLoaded', init);
