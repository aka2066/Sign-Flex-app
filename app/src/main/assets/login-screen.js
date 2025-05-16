/**
 * Login Screen for SignFlex
 * Handles user authentication UI
 */

import auth from './auth.js';

class LoginScreen {
  constructor() {
    this.currentView = 'login'; // 'login' or 'register'
    this.isLoading = false;
    this.error = null;
  }

  // Initialize the login screen
  async initialize(containerId) {
    this.container = document.getElementById(containerId);
    if (!this.container) {
      console.error(`Container with ID "${containerId}" not found`);
      return;
    }
    
    // Check if demo mode is enabled in URL or localStorage
    const urlParams = new URLSearchParams(window.location.search);
    const demoParam = urlParams.get('demo');
    
    if (demoParam === 'true' || localStorage.getItem('signflex_demo_mode') === 'true') {
      console.log('Demo mode detected in login screen - auto logging in');
      // Auto login with demo account
      if (typeof auth.loginAsDemo === 'function') {
        auth.loginAsDemo();
        // Show BLE screen
        setTimeout(() => {
          if (typeof showBleScreen === 'function') {
            showBleScreen();
          }
        }, 100);
        return;
      }
    }
    
    this.render();
    this.attachEventListeners();
    console.log('Login screen initialized');
  }

  // Render the login/register form
  render() {
    if (!this.container) return;
    
    const isLoginView = this.currentView === 'login';
    
    this.container.innerHTML = `
      <div class="auth-container">
        <h2>${isLoginView ? 'Login to SignFlex' : 'Create Account'}</h2>
        
        ${this.error ? `<div class="error-message">${this.error}</div>` : ''}
        
        <div class="demo-button-container">
          <button id="demo-login-button" class="demo-button">Enter Demo Mode</button>
          <p class="demo-notice">No login required - test the app instantly</p>
        </div>
        
        <form id="auth-form" class="${this.isLoading ? 'loading' : ''}">
          <div class="form-group">
            <label for="username">Username</label>
            <input 
              type="text" 
              id="username" 
              name="username" 
              required 
              autocomplete="username"
              ${this.isLoading ? 'disabled' : ''}
            >
          </div>
          
          ${!isLoginView ? `
          <div class="form-group">
            <label for="email">Email</label>
            <input 
              type="email" 
              id="email" 
              name="email" 
              required 
              autocomplete="email"
              ${this.isLoading ? 'disabled' : ''}
            >
          </div>
          ` : ''}
          
          <div class="form-group">
            <label for="password">Password</label>
            <input 
              type="password" 
              id="password" 
              name="password" 
              required 
              autocomplete="${isLoginView ? 'current-password' : 'new-password'}"
              ${this.isLoading ? 'disabled' : ''}
            >
          </div>
          
          <div class="form-actions">
            <button 
              type="submit" 
              class="primary-btn" 
              ${this.isLoading ? 'disabled' : ''}
            >
              ${this.isLoading ? 'Please wait...' : isLoginView ? 'Login' : 'Register'}
            </button>
          </div>
        </form>
        
        <div class="auth-toggle">
          ${isLoginView 
            ? "Don't have an account? <a href='#' id='toggle-register'>Register</a>" 
            : "Already have an account? <a href='#' id='toggle-login'>Login</a>"}
        </div>
      </div>
    `;
    
    // Add event listener for demo login button
    document.getElementById('demo-login-button').addEventListener('click', () => {
      console.log('Demo button clicked');
      if (typeof auth.loginAsDemo === 'function') {
        auth.loginAsDemo();
        // Show BLE screen
        if (typeof showBleScreen === 'function') {
          showBleScreen();
        }
      }
    });
  }

  // Attach event listeners
  attachEventListeners() {
    // Toggle between login and register views
    const toggleRegister = document.getElementById('toggle-register');
    const toggleLogin = document.getElementById('toggle-login');
    
    if (toggleRegister) {
      toggleRegister.addEventListener('click', (e) => {
        e.preventDefault();
        this.currentView = 'register';
        this.error = null;
        this.render();
        this.attachEventListeners();
      });
    }
    
    if (toggleLogin) {
      toggleLogin.addEventListener('click', (e) => {
        e.preventDefault();
        this.currentView = 'login';
        this.error = null;
        this.render();
        this.attachEventListeners();
      });
    }
    
    // Handle form submission
    const form = document.getElementById('auth-form');
    if (form) {
      form.addEventListener('submit', (e) => this.handleSubmit(e));
    }
  }

  // Handle form submission
  async handleSubmit(e) {
    e.preventDefault();
    console.log('Form submitted');
    
    if (this.isLoading) {
      console.log('Form is already loading, ignoring submission');
      return;
    }
    
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    
    console.log('Credentials:', { username, password: password ? '(provided)' : '(empty)' });
    
    if (!username || !password) {
      this.error = 'Please enter both username and password';
      this.render();
      this.attachEventListeners();
      return;
    }
    
    this.isLoading = true;
    this.error = null;
    this.render();
    this.attachEventListeners();
    
    try {
      if (this.currentView === 'login') {
        // Handle login
        console.log('Attempting login for:', username);
        await auth.login(username, password);
        console.log('Login successful, refreshing page');
        window.location.reload();
      } else {
        // Handle registration
        const emailInput = document.getElementById('email');
        if (!emailInput) {
          throw new Error('Email field not found');
        }
        
        const email = emailInput.value.trim();
        console.log('Attempting registration for:', username, 'with email:', email);
        
        if (!email) {
          this.error = 'Please provide an email address';
          this.isLoading = false;
          this.render();
          this.attachEventListeners();
          return;
        }
        
        await auth.register(username, email, password);
        console.log('Registration successful, refreshing page');
        window.location.reload();
      }
    } catch (error) {
      console.error('Auth error:', error);
      this.error = error.message || 'An error occurred during authentication';
    } finally {
      this.isLoading = false;
      this.render();
      this.attachEventListeners();
    }
  }
}

// Create and export instance
const loginScreen = new LoginScreen();
export default loginScreen;
