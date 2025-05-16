/**
 * Main entry point for SignFlex web application
 * Manages authentication, UI and BLE operations
 */

import auth from './auth.js';
import BLEManager from './ble-manager.js';
import { SpeechManager } from './speech-manager.js';

// Initialize Bluetooth manager with service UUID
const bleManager = new BLEManager('4fafc201-1fb5-459e-8fcc-c5c9c331914b');
const speechManager = new SpeechManager();

// Global device information
let deviceConnected = false;
let simulationMode = false;

// DOM References
const loginScreen = document.getElementById('login-screen');
const homeScreen = document.getElementById('home-screen');
const aslScreen = document.getElementById('asl-screen');
const bleScreen = document.getElementById('ble-screen');
const userInfoDisplay = document.getElementById('user-info');

// Button references
const loginForm = document.getElementById('login-form');
const startAslBtn = document.getElementById('start-asl-btn');
const startBleBtn = document.getElementById('start-ble-btn');
const scanBtn = document.getElementById('scan-btn');
const aslBackBtn = document.getElementById('asl-back-btn');
const bleBackBtn = document.getElementById('ble-back-btn');
const logoutBtn = document.getElementById('logout-btn');

/**
 * Screen management functions
 */
function showScreen(screenId) {
  console.log('Showing screen:', screenId);
  
  // Hide all screens
  loginScreen?.classList.add('hidden');
  homeScreen?.classList.add('hidden');
  aslScreen?.classList.add('hidden');
  bleScreen?.classList.add('hidden');
  
  // Show the requested screen
  const screen = document.getElementById(screenId);
  if (screen) {
    screen.classList.remove('hidden');
    console.log('Screen shown:', screenId);
  } else {
    console.error('Screen not found:', screenId);
  }
}

function showLoginScreen() {
  showScreen('login-screen');
}

function showHomeScreen() {
  showScreen('home-screen');
  updateUserInfo();
}

function showASLScreen() {
  showScreen('asl-screen');
}

function showBLEScreen() {
  showScreen('ble-screen');
}

/**
 * Update user information display
 */
function updateUserInfo() {
  console.log('Updating user info');
  if (!userInfoDisplay) return;
  
  const user = auth.user;
  if (user) {
    userInfoDisplay.classList.remove('hidden');
    userInfoDisplay.innerHTML = `
      <div class="user-avatar">${user.username.charAt(0).toUpperCase()}</div>
      <div class="user-details">
        <div class="username">${user.username}</div>
        <button id="logout-btn" class="logout-btn">Logout</button>
      </div>
    `;
    
    // Add event listener to logout button
    document.getElementById('logout-btn')?.addEventListener('click', () => {
      auth.logout();
    });
  } else {
    userInfoDisplay.classList.add('hidden');
  }
}

/**
 * Login form handler
 */
function setupLoginForm() {
  if (!loginForm) return;
  
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMsg = document.getElementById('login-error');
    
    try {
      await auth.login(username, password);
      errorMsg.classList.add('hidden');
      showHomeScreen();
    } catch (error) {
      console.error('Login error:', error);
      errorMsg.textContent = error.message || 'Login failed. Please try again.';
      errorMsg.classList.remove('hidden');
    }
  });
}

/**
 * BLE connection setup
 */
function setupBleListeners() {
  if (!scanBtn) return;
  
  scanBtn.addEventListener('click', async () => {
    try {
      const deviceStatus = document.getElementById('device-status');
      deviceStatus.textContent = 'Scanning...';
      
      await bleManager.connect();
      deviceConnected = true;
      
      deviceStatus.textContent = 'Connected';
      document.getElementById('battery-info').classList.remove('hidden');
      document.getElementById('flex-readings').classList.remove('hidden');
      
      // Start monitoring for flex sensor data
      bleManager.subscribeToBatteryUpdates((value) => {
        document.getElementById('battery-level').textContent = value.level + '%';
        document.getElementById('battery-voltage').textContent = value.voltage.toFixed(2) + 'V';
      });
      
      bleManager.subscribeToFlexSensor((value) => {
        // Update UI with flex sensor data
        document.getElementById('flex-value').textContent = value;
        
        // Process ASL sign if value is valid
        if (value > 0) {
          speechManager.processPotentialSign(value);
        }
      });
      
    } catch (error) {
      console.error('BLE connection error:', error);
      document.getElementById('device-status').textContent = 'Error: ' + error.message;
    }
  });
}

/**
 * Navigation setup
 */
function setupNavigation() {
  // Start ASL button
  startAslBtn?.addEventListener('click', () => {
    showASLScreen();
  });
  
  // Start BLE button
  startBleBtn?.addEventListener('click', () => {
    showBLEScreen();
  });
  
  // Back buttons
  aslBackBtn?.addEventListener('click', () => {
    showHomeScreen();
  });
  
  bleBackBtn?.addEventListener('click', () => {
    showHomeScreen();
  });
}

/**
 * Check if we're on Chrome or a browser with Web Bluetooth API
 */
function isWebBluetoothSupported() {
  return 'bluetooth' in navigator;
}

/**
 * Display Web Bluetooth compatibility message
 */
function checkBluetoothSupport() {
  const bleContainer = document.querySelector('.ble-compatibility');
  if (!bleContainer) return;
  
  if (!isWebBluetoothSupported()) {
    bleContainer.innerHTML = `
      <div class="compatibility-warning">
        <p>⚠️ Your browser doesn't support Web Bluetooth API.</p>
        <p>For BLE functionality, please use Chrome or Edge.</p>
      </div>
    `;
    bleContainer.classList.remove('hidden');
  } else {
    bleContainer.classList.add('hidden');
  }
}

/**
 * Initialize the application
 */
async function initializeApp() {
  console.log('Initializing SignFlex application...');
  
  // Initialize auth
  await auth.init();
  
  // Set up UI event listeners
  setupLoginForm();
  setupNavigation();
  setupBleListeners();
  
  // Check BLE compatibility
  checkBluetoothSupport();
  
  // Initialize speech manager
  speechManager.init();
  
  // Show the appropriate screen based on authentication
  if (auth.isAuthenticated()) {
    console.log('User is authenticated, showing home screen');
    showHomeScreen();
  } else {
    console.log('User is not authenticated, showing login screen');
    showLoginScreen();
  }
  
  console.log('App initialization complete');
}

// Wait for DOM to be fully loaded
document.addEventListener('DOMContentLoaded', () => {
  console.log('DOM fully loaded, initializing app...');
  initializeApp();
});

// Export functions for use in other modules
window.showHomeScreen = showHomeScreen;
window.showLoginScreen = showLoginScreen;
window.updateUserInfo = updateUserInfo;
