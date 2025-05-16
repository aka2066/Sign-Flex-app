/**
 * Direct fix for SignFlex UI issues
 * This is a standalone script that fixes demo mode and registration
 * without relying on complex module interactions
 */

// Wait for the DOM to be fully loaded
document.addEventListener('DOMContentLoaded', function() {
  console.log('Direct fix script loaded');
  
  // Give a small delay to ensure all other scripts are loaded
  setTimeout(function() {
    // Fix demo button
    const demoButton = document.getElementById('demo-login-button');
    if (demoButton) {
      console.log('Found demo button, attaching direct handler');
      
      // Remove any existing handlers by cloning and replacing
      const newDemoButton = demoButton.cloneNode(true);
      demoButton.parentNode.replaceChild(newDemoButton, demoButton);
      
      // Add new direct click handler
      newDemoButton.addEventListener('click', function(e) {
        e.preventDefault();
        console.log('Demo button clicked directly');
        
        // Create demo user directly
        const demoUser = {
          id: 'demo-user-' + Date.now(),
          username: 'demo',
          name: 'Demo User'
        };
        
        // Store in localStorage
        const demoToken = 'demo-token-' + Date.now();
        localStorage.setItem('token', demoToken);
        localStorage.setItem('user', JSON.stringify(demoUser));
        localStorage.setItem('demoMode', 'true');
        
        // Show home screen directly
        showHomeScreenDirect();
      });
    } else {
      console.error('Could not find demo button');
    }
    
    // Fix register link
    const registerLink = document.getElementById('register-link');
    if (registerLink) {
      console.log('Found register link, attaching direct handler');
      
      // Remove any existing handlers
      const newRegisterLink = registerLink.cloneNode(true);
      registerLink.parentNode.replaceChild(newRegisterLink, registerLink);
      
      // Add new direct click handler
      newRegisterLink.addEventListener('click', function(e) {
        e.preventDefault();
        console.log('Register link clicked directly');
        
        // Get form reference
        const loginForm = document.getElementById('login-form');
        if (loginForm) {
          // Change form title
          const title = document.querySelector('.login-container h2');
          if (title) {
            title.textContent = 'Create Account';
          }
          
          // Change button text
          const submitBtn = loginForm.querySelector('.submit-btn');
          if (submitBtn) {
            submitBtn.textContent = 'Register';
          }
          
          // Change register link to login link
          newRegisterLink.textContent = 'Back to Login';
          
          // Remove and replace form submit handler
          const newForm = loginForm.cloneNode(true);
          loginForm.parentNode.replaceChild(newForm, loginForm);
          
          // Add new direct submit handler for registration
          newForm.addEventListener('submit', function(e) {
            e.preventDefault();
            console.log('Registration form submitted directly');
            
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            
            if (!username || !password) {
              alert('Username and password are required');
              return;
            }
            
            if (password.length < 4) {
              alert('Password must be at least 4 characters');
              return;
            }
            
            // Create new user directly
            const newUser = {
              id: 'user-' + Date.now(),
              username: username,
              name: username
            };
            
            // Store in localStorage
            const token = 'token-' + Date.now();
            localStorage.setItem('token', token);
            localStorage.setItem('user', JSON.stringify(newUser));
            
            // Show home screen directly
            showHomeScreenDirect();
          });
          
          // Back to login button handler
          newRegisterLink.addEventListener('click', function(e) {
            e.preventDefault();
            // Simply reload the page to reset everything
            window.location.reload();
          });
        }
      });
    }
    
    // Check URL for demo parameter
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('demo') === 'true') {
      console.log('Demo mode requested via URL parameter');
      
      // Create demo user directly
      const demoUser = {
        id: 'demo-user-' + Date.now(),
        username: 'demo',
        name: 'Demo User'
      };
      
      // Store in localStorage
      const demoToken = 'demo-token-' + Date.now();
      localStorage.setItem('token', demoToken);
      localStorage.setItem('user', JSON.stringify(demoUser));
      localStorage.setItem('demoMode', 'true');
      
      // Show home screen directly
      showHomeScreenDirect();
    }
    
    // Check if already authenticated
    const token = localStorage.getItem('token');
    const userJson = localStorage.getItem('user');
    
    if (token && userJson) {
      console.log('User already authenticated, showing home screen');
      showHomeScreenDirect();
    }

    // Fix the BLE screen handling
    setupBLEScreen();
  }, 1000); // Longer delay to ensure everything else is loaded
});

// Direct function to show home screen
function showHomeScreenDirect() {
  console.log('Directly showing home screen');
  
  // Hide all screens
  const screens = document.querySelectorAll('.screen');
  screens.forEach(function(screen) {
    screen.classList.add('hidden');
  });
  
  // Show home screen
  const homeScreen = document.getElementById('home-screen');
  if (homeScreen) {
    homeScreen.classList.remove('hidden');
    
    // Update user info
    updateUserInfoDirect();
    
    // Setup navigation
    setupNavigationDirect();
  } else {
    console.error('Home screen not found');
  }
}

// Direct function to update user info
function updateUserInfoDirect() {
  console.log('Directly updating user info');
  
  const userInfoEl = document.getElementById('user-info');
  if (!userInfoEl) return;
  
  try {
    const userJson = localStorage.getItem('user');
    if (!userJson) return;
    
    const user = JSON.parse(userJson);
    
    userInfoEl.classList.remove('hidden');
    userInfoEl.innerHTML = `
      <div class="user-avatar">${user.username.charAt(0).toUpperCase()}</div>
      <div class="user-details">
        <div class="username">${user.username}</div>
        <button id="logout-btn-direct" class="logout-btn">Logout</button>
      </div>
    `;
    
    // Add logout handler
    const logoutBtn = document.getElementById('logout-btn-direct');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', function() {
        // Clear storage
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        localStorage.removeItem('demoMode');
        
        // Reload page
        window.location.reload();
      });
    }
  } catch (e) {
    console.error('Error updating user info:', e);
  }
}

// Direct function to setup navigation
function setupNavigationDirect() {
  console.log('Setting up direct navigation');
  
  // Setup ASL button
  const startAslBtn = document.getElementById('start-asl-btn');
  if (startAslBtn) {
    startAslBtn.addEventListener('click', function() {
      // Hide all screens
      const screens = document.querySelectorAll('.screen');
      screens.forEach(function(screen) {
        screen.classList.add('hidden');
      });
      
      // Show ASL screen
      const aslScreen = document.getElementById('asl-screen');
      if (aslScreen) {
        aslScreen.classList.remove('hidden');
      }
    });
  }
  
  // Setup BLE button
  const startBleBtn = document.getElementById('start-ble-btn');
  if (startBleBtn) {
    startBleBtn.addEventListener('click', function() {
      // Hide all screens
      const screens = document.querySelectorAll('.screen');
      screens.forEach(function(screen) {
        screen.classList.add('hidden');
      });
      
      // Show BLE screen
      const bleScreen = document.getElementById('ble-screen');
      if (bleScreen) {
        bleScreen.classList.remove('hidden');
      }
    });
  }
  
  // Setup back buttons
  const backButtons = document.querySelectorAll('.back-btn');
  backButtons.forEach(function(btn) {
    btn.addEventListener('click', function() {
      // Hide all screens
      const screens = document.querySelectorAll('.screen');
      screens.forEach(function(screen) {
        screen.classList.add('hidden');
      });
      
      // Show home screen
      const homeScreen = document.getElementById('home-screen');
      if (homeScreen) {
        homeScreen.classList.remove('hidden');
      }
    });
  });
}

// Fix BLE screen to properly connect to ESP32
function setupBLEScreen() {
  const bleScreen = document.getElementById('ble-screen');
  if (!bleScreen) return;
  
  // Check if Web Bluetooth API is supported
  const isWebBluetoothSupported = 'bluetooth' in navigator;
  
  // Show compatibility message if needed
  const bleCompatibilityMsg = bleScreen.querySelector('.ble-compatibility');
  if (bleCompatibilityMsg) {
    if (!isWebBluetoothSupported) {
      bleCompatibilityMsg.innerHTML = `
        <div class="compatibility-warning">
          <p>⚠️ Your browser doesn't support Web Bluetooth API.</p>
          <p>For BLE functionality, please use Chrome or Edge.</p>
        </div>
      `;
      bleCompatibilityMsg.classList.remove('hidden');
    } else {
      bleCompatibilityMsg.classList.add('hidden');
    }
  }
  
  // Set up scan button
  const scanBtn = document.getElementById('scan-btn');
  if (!scanBtn) return;
  
  // Add click handler for scan button
  scanBtn.addEventListener('click', async function() {
    if (!isWebBluetoothSupported) {
      alert('Web Bluetooth API is not supported in this browser. Please use Chrome or Edge.');
      return;
    }
    
    // Change status
    const deviceStatus = document.getElementById('device-status');
    if (deviceStatus) {
      deviceStatus.textContent = 'Scanning...';
    }
    
    try {
      console.log('Requesting Bluetooth device...');
      // Request device with ESP32 service UUID
      const device = await navigator.bluetooth.requestDevice({
        filters: [{ services: ['4fafc201-1fb5-459e-8fcc-c5c9c331914b'] }]
      });
      
      console.log('Device selected:', device.name);
      
      // Add event listener for disconnection
      device.addEventListener('gattserverdisconnected', function() {
        console.log('Device disconnected');
        if (deviceStatus) {
          deviceStatus.textContent = 'Device disconnected';
        }
        
        // Hide data containers
        const batteryInfo = document.getElementById('battery-info');
        const flexReadings = document.getElementById('flex-readings');
        
        if (batteryInfo) batteryInfo.classList.add('hidden');
        if (flexReadings) flexReadings.classList.add('hidden');
      });
      
      // Update status
      if (deviceStatus) {
        deviceStatus.textContent = 'Connecting...';
      }
      
      // Connect to the device
      console.log('Connecting to GATT server...');
      const server = await device.gatt.connect();
      
      console.log('Getting primary service...');
      const service = await server.getPrimaryService('4fafc201-1fb5-459e-8fcc-c5c9c331914b');
      
      // Get required characteristics
      console.log('Getting flex sensor characteristic...');
      const flexCharacteristic = await service.getCharacteristic('beb5483e-36e1-4688-b7f5-ea07361b26ac');
      
      console.log('Getting battery characteristic...');
      const batteryCharacteristic = await service.getCharacteristic('beb5483e-36e1-4688-b7f5-ea07361b26ad');
      
      // Update status
      if (deviceStatus) {
        deviceStatus.textContent = 'Connected to ' + (device.name || 'ESP32');
      }
      
      // Show data containers
      const batteryInfo = document.getElementById('battery-info');
      const flexReadings = document.getElementById('flex-readings');
      
      if (batteryInfo) batteryInfo.classList.remove('hidden');
      if (flexReadings) flexReadings.classList.remove('hidden');
      
      // Subscribe to battery notifications
      await batteryCharacteristic.startNotifications();
      batteryCharacteristic.addEventListener('characteristicvaluechanged', function(event) {
        try {
          const value = event.target.value;
          const dataView = new DataView(value.buffer);
          
          // Get battery level and voltage
          const level = dataView.getUint8(0);
          const voltage = dataView.getFloat32(1, true);
          
          console.log('Battery update - Level:', level, '%, Voltage:', voltage, 'V');
          
          // Update UI
          const batteryLevel = document.getElementById('battery-level');
          const batteryVoltage = document.getElementById('battery-voltage');
          
          if (batteryLevel) batteryLevel.textContent = level + '%';
          if (batteryVoltage) batteryVoltage.textContent = voltage.toFixed(2) + 'V';
        } catch (error) {
          console.error('Error parsing battery data:', error);
        }
      });
      
      // Subscribe to flex sensor notifications
      await flexCharacteristic.startNotifications();
      flexCharacteristic.addEventListener('characteristicvaluechanged', function(event) {
        try {
          const value = event.target.value;
          const dataView = new DataView(value.buffer);
          
          // Get flex value (assuming it's a Uint16 at offset 0)
          const flexValue = dataView.getUint16(0, true);
          
          console.log('Flex sensor value:', flexValue);
          
          // Update UI
          const flexValueEl = document.getElementById('flex-value');
          if (flexValueEl) flexValueEl.textContent = flexValue;
        } catch (error) {
          console.error('Error parsing flex sensor data:', error);
        }
      });
      
    } catch (error) {
      console.error('Bluetooth error:', error);
      
      // Update status
      if (deviceStatus) {
        deviceStatus.textContent = 'Error: ' + (error.message || 'Connection failed');
      }
    }
  });
}
