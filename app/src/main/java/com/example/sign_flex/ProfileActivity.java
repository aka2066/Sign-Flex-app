package com.example.sign_flex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.sign_flex.api.AuthService;
import com.example.sign_flex.auth.SessionManager;
import com.example.sign_flex.model.User;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    
    private TextInputEditText usernameEditText;
    private TextInputEditText emailEditText;
    private Button updateProfileButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private ProgressBar progressBar;
    
    private AuthService authService;
    private SessionManager sessionManager;
    private User currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Initialize services
        authService = new AuthService(this);
        sessionManager = new SessionManager(this);
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            // User is not logged in, redirect to LoginActivity
            Toast.makeText(this, "Please log in to access your profile", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }
        
        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        updateProfileButton = findViewById(R.id.updateProfileButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        logoutButton = findViewById(R.id.logoutButton);
        progressBar = findViewById(R.id.progressBar);
        
        // Load current user data
        loadUserData();
        
        // Set click listeners
        updateProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
            }
        });
        
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // In a real app, you would open a dialog or activity for changing password
                Toast.makeText(ProfileActivity.this, "Change password functionality coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
        
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }
    
    private void loadUserData() {
        // Show progress indicator
        progressBar.setVisibility(View.VISIBLE);
        
        // Get user from session
        currentUser = sessionManager.getUserDetails();
        
        // Try to refresh user data from server
        if (currentUser != null && currentUser.getToken() != null) {
            authService.getCurrentUser(currentUser.getToken(), new AuthService.AuthCallback() {
                @Override
                public void onSuccess(User user) {
                    // Hide progress indicator
                    progressBar.setVisibility(View.GONE);
                    
                    // Update current user
                    currentUser = user;
                    
                    // Update UI with user data
                    updateUI();
                    
                    // Update session
                    sessionManager.createLoginSession(user);
                }
                
                @Override
                public void onError(String message) {
                    // Hide progress indicator
                    progressBar.setVisibility(View.GONE);
                    
                    Log.e(TAG, "Error refreshing user data: " + message);
                    
                    // Try to use cached data
                    if (currentUser != null) {
                        updateUI();
                    }
                }
            });
        } else {
            // Use cached data
            progressBar.setVisibility(View.GONE);
            updateUI();
        }
    }
    
    private void updateUI() {
        if (currentUser != null) {
            usernameEditText.setText(currentUser.getUsername());
            emailEditText.setText(currentUser.getEmail());
        }
    }
    
    private void updateProfile() {
        // Get input values
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            return;
        }
        
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            return;
        }
        
        // Show progress indicator
        progressBar.setVisibility(View.VISIBLE);
        updateProfileButton.setEnabled(false);
        
        // Check if any changes were made
        boolean hasChanges = false;
        if (!username.equals(currentUser.getUsername())) {
            hasChanges = true;
        }
        if (!email.equals(currentUser.getEmail())) {
            hasChanges = true;
        }
        
        if (!hasChanges) {
            // No changes made
            progressBar.setVisibility(View.GONE);
            updateProfileButton.setEnabled(true);
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update profile
        authService.updateProfile(
                currentUser.getToken(),
                username,
                email,
                new AuthService.AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        // Hide progress indicator
                        progressBar.setVisibility(View.GONE);
                        updateProfileButton.setEnabled(true);
                        
                        // Update current user
                        currentUser = user;
                        
                        // Update session
                        sessionManager.createLoginSession(user);
                        
                        // Show success message
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onError(String message) {
                        // Hide progress indicator
                        progressBar.setVisibility(View.GONE);
                        updateProfileButton.setEnabled(true);
                        
                        // Show error message
                        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Profile update error: " + message);
                    }
                }
        );
    }
    
    private void logout() {
        // Clear session
        sessionManager.logoutUser();
        
        // Navigate to login screen
        navigateToLogin();
        
        // Show logout message
        Toast.makeText(this, "You have been logged out", Toast.LENGTH_SHORT).show();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle action bar item clicks, specifically for the home/back button
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
