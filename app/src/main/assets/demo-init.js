/**
 * Demo initialization script for SignFlex
 * This script handles the demo mode initialization and UI fixes
 */

// Function to check if demo mode is enabled in URL
function isDemoModeRequested() {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get('demo') === 'true';
}

// Function to show a specific screen
function showScreen(screenId) {
  console.log('Demo-init: Showing screen:', screenId);
  
  // Hide all screens first
  const screens = document.querySelectorAll('.screen');
  screens.forEach(screen => {
    screen.classList.add('hidden');
  });
  
  // Show the requested screen
  const screen = document.getElementById(screenId);
  if (screen) {
    screen.classList.remove('hidden');
    console.log('Demo-init: Screen now visible:', screenId);
  } else {
    console.error('Demo-init: Screen not found:', screenId);
  }
}

// Update user info in the UI
function updateDemoUserInfo() {
  console.log('Demo-init: Updating demo user info');
  const userInfoEl = document.getElementById('user-info');
  if (userInfoEl) {
    userInfoEl.classList.remove('hidden');
    
    userInfoEl.innerHTML = `
      <div class="user-avatar">D</div>
      <div class="user-details">
        <div class="username">Demo User</div>
        <button id="logout-btn" class="logout-btn">Logout</button>
      </div>
    `;
    
    // Add event listener to logout button
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        if (typeof auth !== 'undefined') {
          auth.logout();
        }
      });
    }
  }
}

// Add demo button to login screen
function addDemoButton() {
  // Directly add click handler to the demo button
  const demoButton = document.getElementById('demo-login-button');
  if (demoButton) {
    console.log('Demo-init: Adding click handler to demo button');
    
    demoButton.addEventListener('click', (e) => {
      e.preventDefault();
      console.log('Demo-init: Demo button clicked');
      
      // Check if auth module is available and enable demo mode
      if (typeof auth !== 'undefined') {
        auth.enableDemoMode();
        showScreen('home-screen');
        updateDemoUserInfo();
      } else {
        console.error('Demo-init: Auth module not available');
      }
    });
  } else {
    console.error('Demo-init: Demo button not found in the DOM');
  }
}

// Add registration functionality
function setupRegistration() {
  const registerLink = document.getElementById('register-link');
  if (registerLink) {
    console.log('Demo-init: Adding register link handler');
    
    registerLink.addEventListener('click', (e) => {
      e.preventDefault();
      console.log('Demo-init: Register link clicked');
      
      // Get login form and convert it to registration form
      const loginForm = document.getElementById('login-form');
      const loginTitle = document.querySelector('.login-container h2');
      
      if (loginForm && loginTitle) {
        // Change title
        loginTitle.textContent = 'Create Account';
        
        // Change submit button text
        const submitBtn = loginForm.querySelector('.submit-btn');
        if (submitBtn) {
          submitBtn.textContent = 'Register';
        }
        
        // Change registration link to login link
        registerLink.textContent = 'Back to Login';
        
        // Store original submit handler
        const originalSubmitHandler = loginForm.onsubmit;
        
        // Change form submission to register instead of login
        loginForm.onsubmit = async (e) => {
          e.preventDefault();
          const username = document.getElementById('username').value;
          const password = document.getElementById('password').value;
          const errorMsg = document.getElementById('login-error');
          
          try {
            console.log('Demo-init: Registering new user');
            if (typeof auth !== 'undefined' && typeof auth.register === 'function') {
              await auth.register(username, password);
              errorMsg.classList.add('hidden');
              showScreen('home-screen');
              
              // Update UI with new user
              if (typeof window.updateUserInfo === 'function') {
                window.updateUserInfo();
              }
            } else {
              // Just use demo mode as fallback
              if (typeof auth !== 'undefined') {
                auth.enableDemoMode();
                showScreen('home-screen');
                updateDemoUserInfo();
              }
            }
          } catch (error) {
            console.error('Registration error:', error);
            errorMsg.textContent = error.message || 'Registration failed. Please try again.';
            errorMsg.classList.remove('hidden');
          }
        };
        
        // Change register link click to go back to login
        registerLink.onclick = (e) => {
          e.preventDefault();
          
          // Restore original form
          loginTitle.textContent = 'Sign In';
          if (submitBtn) {
            submitBtn.textContent = 'Login';
          }
          registerLink.textContent = 'Create Account';
          
          // Restore original submit handler
          loginForm.onsubmit = originalSubmitHandler;
          
          // Reset error message
          const errorMsg = document.getElementById('login-error');
          errorMsg.classList.add('hidden');
          
          // Restore register link handler
          registerLink.onclick = null;
          setupRegistration();
        };
      }
    });
  }
}

// Demo initialization
function initDemo() {
  console.log('Demo-init: Initializing demo mode...');
  
  // Check if we should auto-login with demo mode
  if (isDemoModeRequested()) {
    console.log('Demo-init: Demo mode requested via URL');
    
    // Check if auth module is available
    if (typeof auth !== 'undefined') {
      // Small delay to ensure auth module is fully loaded
      setTimeout(() => {
        auth.enableDemoMode();
        showScreen('home-screen');
        updateDemoUserInfo();
      }, 500);
    }
  }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  console.log('Demo-init: DOM loaded');
  
  // Add demo button click handler
  setTimeout(() => {
    addDemoButton();
    setupRegistration();
    initDemo();
  }, 500); // Delay to ensure DOM is fully loaded
});

// Make functions globally available
window.isDemoModeRequested = isDemoModeRequested;
window.demoShowScreen = showScreen;
window.updateDemoUserInfo = updateDemoUserInfo;
