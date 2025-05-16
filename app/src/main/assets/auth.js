/**
 * Authentication module for SignFlex
 */
class Auth {
  constructor() {
    this.token = null;
    this.user = null;
    this.demoMode = false;
  }

  async init() {
    console.log('Auth: Initializing...');
    // Check local storage for existing token
    this.token = localStorage.getItem('token');
    const userJson = localStorage.getItem('user');
    
    if (userJson) {
      try {
        this.user = JSON.parse(userJson);
        console.log('Auth: User loaded from storage', this.user.username);
      } catch (e) {
        console.error('Auth: Error parsing user JSON', e);
        this.user = null;
      }
    }
    
    // Check if demo mode was previously enabled
    this.demoMode = localStorage.getItem('demoMode') === 'true';
    
    if (this.demoMode) {
      console.log('Auth: Demo mode active');
      this.setupDemoUser();
    }
    
    console.log('Auth: Initialization complete. Authenticated:', this.isAuthenticated());
  }

  async login(username, password) {
    try {
      console.log('Auth: Attempting login for', username);
      // Here you would normally make an API call to a backend server
      // For demo/development, we'll simulate authentication
      if (username && password) {
        // Simulate successful login
        this.token = 'sample-jwt-token-' + Date.now();
        this.user = {
          id: 'user-' + Date.now(),
          username: username,
          name: username
        };
        
        // Store in local storage
        localStorage.setItem('token', this.token);
        localStorage.setItem('user', JSON.stringify(this.user));
        
        console.log('Auth: Login successful');
        return { success: true, user: this.user };
      } else {
        throw new Error('Invalid credentials');
      }
    } catch (error) {
      console.error('Auth: Login error', error);
      throw error;
    }
  }
  
  async register(username, password) {
    try {
      console.log('Auth: Registering new user', username);
      
      if (!username || !password) {
        throw new Error('Username and password are required');
      }
      
      if (password.length < 6) {
        throw new Error('Password must be at least 6 characters');
      }
      
      // In a real app, this would make an API call to register the user
      // For demo purposes, we'll simulate a successful registration
      
      // Simulate successful registration and auto-login
      this.token = 'sample-jwt-token-' + Date.now();
      this.user = {
        id: 'user-' + Date.now(),
        username: username,
        name: username
      };
      
      // Store in local storage
      localStorage.setItem('token', this.token);
      localStorage.setItem('user', JSON.stringify(this.user));
      
      console.log('Auth: Registration successful');
      return { success: true, user: this.user };
    } catch (error) {
      console.error('Auth: Registration error', error);
      throw error;
    }
  }

  logout() {
    console.log('Auth: Logging out');
    this.token = null;
    this.user = null;
    this.demoMode = false;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('demoMode');
    
    // After logout, refresh to go back to login screen
    window.location.reload();
  }

  isAuthenticated() {
    return !!this.token;
  }
  
  // Demo mode functions
  enableDemoMode() {
    console.log('Auth: Enabling demo mode');
    this.demoMode = true;
    localStorage.setItem('demoMode', 'true');
    this.setupDemoUser();
  }
  
  setupDemoUser() {
    // Create a demo user and token
    this.token = 'demo-token-' + Date.now();
    this.user = {
      id: 'demo-user',
      username: 'demo',
      name: 'Demo User'
    };
    
    // Store in local storage
    localStorage.setItem('token', this.token);
    localStorage.setItem('user', JSON.stringify(this.user));
    
    console.log('Auth: Demo user created');
  }
  
  isDemoMode() {
    return this.demoMode;
  }
}

// Create and export a single instance
const auth = new Auth();
export default auth;
