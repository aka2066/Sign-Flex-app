package com.example.sign_flex.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.example.sign_flex.model.User;

/**
 * Session manager for handling user authentication state
 */
public class SessionManager {
    private static final String TAG = "SessionManager";
    
    // SharedPreferences file name
    private static final String PREF_NAME = "SignFlexSession";
    
    // SharedPreferences key names
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_TOKEN = "token";
    
    // SharedPreferences and Editor
    private SharedPreferences sharedPreferences;
    private Editor editor;
    
    // Constructor
    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
    
    /**
     * Create login session
     */
    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_TOKEN, user.getToken());
        editor.commit();
        
        Log.d(TAG, "User login session created");
    }
    
    /**
     * Get stored user data
     */
    public User getUserDetails() {
        // Check login status
        if (!isLoggedIn()) {
            Log.d(TAG, "User is not logged in");
            return null;
        }
        
        // Get stored user data
        User user = new User();
        user.setId(sharedPreferences.getString(KEY_USER_ID, ""));
        user.setUsername(sharedPreferences.getString(KEY_USERNAME, ""));
        user.setEmail(sharedPreferences.getString(KEY_EMAIL, ""));
        user.setToken(sharedPreferences.getString(KEY_TOKEN, ""));
        
        return user;
    }
    
    /**
     * Get authentication token
     */
    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }
    
    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * Clear session details and log out user
     */
    public void logoutUser() {
        // Clear all data from Shared Preferences
        editor.clear();
        editor.commit();
        
        Log.d(TAG, "User logged out, session cleared");
    }
}
