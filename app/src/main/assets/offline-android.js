/**
 * SignFlex Offline Android Mode
 * This script enables offline functionality for the Android app
 */

// Add this script to index.html
window.bypassAuthForAndroid = function() {
  console.log('Bypassing auth for Android app');
  
  // Check if we're in Android app
  const isAndroidApp = localStorage.getItem('IS_ANDROID_APP') === 'true';
  
  if (isAndroidApp) {
    console.log('Running in Android app, setting up offline mode');
    
    // Create a mock user session
    const mockUser = {
      id: 'offline-user',
      username: 'Android User',
      email: 'android@signflex.app',
      preferences: {
        theme: 'light',
        language: 'en',
        notifications: true
      }
    };
    
    // Store the mock user in localStorage
    localStorage.setItem('currentUser', JSON.stringify(mockUser));
    localStorage.setItem('authToken', 'offline-token');
    localStorage.setItem('isAuthenticated', 'true');
    
    // Override API calls with offline versions
    if (window.api) {
      // Save original methods
      const originalMethods = {
        login: window.api.login,
        register: window.api.register,
        getUserData: window.api.getUserData,
        updatePreferences: window.api.updatePreferences
      };
      
      // Override API methods for offline mode
      window.api.login = function(email, password) {
        console.log('Offline login:', email);
        return Promise.resolve({ user: mockUser, token: 'offline-token' });
      };
      
      window.api.register = function(username, email, password) {
        console.log('Offline register:', username, email);
        return Promise.resolve({ user: mockUser, token: 'offline-token' });
      };
      
      window.api.getUserData = function() {
        console.log('Offline getUserData');
        return Promise.resolve(mockUser);
      };
      
      window.api.updatePreferences = function(preferences) {
        console.log('Offline updatePreferences:', preferences);
        mockUser.preferences = { ...mockUser.preferences, ...preferences };
        localStorage.setItem('currentUser', JSON.stringify(mockUser));
        return Promise.resolve({ success: true, preferences: mockUser.preferences });
      };
      
      console.log('API methods overridden for offline mode');
    } else {
      console.warn('API object not found, offline mode may not work correctly');
    }
  }
};

// Execute immediately if possible
if (document.readyState === 'complete') {
  // If the document is already loaded
  if (localStorage.getItem('IS_ANDROID_APP') === 'true') {
    window.bypassAuthForAndroid();
  }
} else {
  // If the document is still loading
  window.addEventListener('DOMContentLoaded', function() {
    if (localStorage.getItem('IS_ANDROID_APP') === 'true') {
      window.bypassAuthForAndroid();
    }
  });
}
