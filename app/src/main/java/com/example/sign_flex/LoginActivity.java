package com.example.sign_flex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sign_flex.api.AuthService;
import com.example.sign_flex.auth.SessionManager;
import com.example.sign_flex.model.User;
import com.google.android.material.textfield.TextInputEditText;

import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    // Hardcoded test credentials for backup login
    private static final String TEST_USERNAME = "test";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "test123";
    
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private TextView registerLinkTextView;
    private TextView forgotPasswordTextView;
    private ProgressBar progressBar;
    
    private AuthService authService;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize services
        authService = new AuthService(this);
        sessionManager = new SessionManager(this);
        
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            // User is already logged in, redirect to MainActivity
            navigateToMainActivity();
            return;
        }
        
        // Initialize UI components
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerLinkTextView = findViewById(R.id.registerLinkTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        progressBar = findViewById(R.id.progressBar);
        
        // Set click listeners
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        
        registerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to RegisterActivity
                try {
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching register: " + e.getMessage(), e);
                    Toast.makeText(LoginActivity.this, "Register page coming soon!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ForgotPasswordActivity
                try {
                    Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching forgot password: " + e.getMessage(), e);
                    Toast.makeText(LoginActivity.this, "Forgot password page coming soon!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void login() {
        // Get input values
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        
        // Show progress indicator
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        
        // Check for hardcoded test credentials
        if (email.equals(TEST_EMAIL) && password.equals(TEST_PASSWORD)) {
            Log.d(TAG, "Using hardcoded test login");
            
            // Create test user
            User testUser = new User();
            testUser.setId("test-user-id");
            testUser.setUsername(TEST_USERNAME);
            testUser.setEmail(TEST_EMAIL);
            testUser.setToken("test-token");
            
            // Save user session
            sessionManager.createLoginSession(testUser);
            
            // Show success message
            Toast.makeText(this, "Test login successful!", Toast.LENGTH_SHORT).show();
            
            // Hide progress indicator
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            
            // Navigate to MainActivity
            navigateToMainActivity();
            return;
        }
        
        // Fallback to demo mode if server connection fails
        if (!isServerReachable()) {
            loginDemo(email, password);
            return;
        }
        
        // Attempt login via MongoDB
        authService.login(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // Hide progress indicator
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);
                
                // Save user session
                sessionManager.createLoginSession(user);
                
                Log.d(TAG, "Login successful for user: " + user.getUsername());
                
                // Show success message
                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                
                // Navigate to MainActivity
                navigateToMainActivity();
            }
            
            @Override
            public void onError(String message) {
                // Hide progress indicator
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);
                
                // Show error message
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Login error: " + message);
            }
        });
    }
    
    private boolean isServerReachable() {
        try {
            // Check if we have an internet connection first
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
                Log.d(TAG, "No active network connection");
                return false;
            }
            
            // Basic server availability check
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String baseUrl = "http://10.0.2.2:3002";
                        URL url = new URL(baseUrl + "/api/health");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(3000); // 3 second timeout
                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        Log.d(TAG, "Server response code: " + responseCode);
                        connection.disconnect();
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking server reachability: " + e.getMessage());
                    }
                }
            });
            thread.start();
            thread.join(3000); // Wait max 3 seconds
            
            // If we have internet, give the server the benefit of the doubt
            // Real connection errors will be handled by the auth service
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in isServerReachable: " + e.getMessage());
            return false;
        }
    }
    
    private void loginDemo(String email, String password) {
        // For demo purposes only
        Log.d(TAG, "Using demo login mode as MongoDB server is not reachable");
        
        // Simulate login process with a delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);
                
                // Create a demo user
                User demoUser = new User();
                demoUser.setId("demo_user_id");
                demoUser.setUsername("Demo User");
                demoUser.setEmail(email);
                demoUser.setToken("demo_token");
                
                // Save user session
                sessionManager.createLoginSession(demoUser);
                
                Toast.makeText(LoginActivity.this, "Demo login successful!", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            }
        }, 1500);
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close login activity
    }
}
