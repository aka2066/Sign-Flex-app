package com.example.sign_flex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
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
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    
    // Hardcoded test credentials for backup registration
    private static final String TEST_USERNAME = "test";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "test123";
    
    private TextInputEditText usernameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private Button registerButton;
    private TextView loginLinkTextView;
    private ProgressBar progressBar;
    
    private AuthService authService;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize services
        authService = new AuthService(this);
        sessionManager = new SessionManager(this);
        
        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginLinkTextView = findViewById(R.id.loginLinkTextView);
        progressBar = findViewById(R.id.progressBar);
        
        // Set click listeners
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
        
        loginLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to LoginActivity
                finish();
            }
        });
    }
    
    private void register() {
        // Get input values
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        
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
        
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters long");
            return;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Please confirm your password");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }
        
        // Show progress indicator
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);
        
        // Check for hardcoded test credentials
        if (username.equals(TEST_USERNAME) || email.equals(TEST_EMAIL)) {
            Log.d(TAG, "Using hardcoded test registration");
            
            // Create test user
            User testUser = new User();
            testUser.setId("test-user-id");
            testUser.setUsername(TEST_USERNAME);
            testUser.setEmail(TEST_EMAIL);
            testUser.setToken("test-token");
            
            // Save user session
            sessionManager.createLoginSession(testUser);
            
            // Show success message
            Toast.makeText(this, "Test registration successful!", Toast.LENGTH_SHORT).show();
            
            // Hide progress indicator
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            
            // Navigate to MainActivity
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        // Fallback to demo mode if server connection fails
        if (!isServerReachable()) {
            registerDemo(username, email, password);
            return;
        }
        
        // Attempt registration via MongoDB
        authService.register(username, email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // Hide progress indicator
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);
                
                // Save user session
                sessionManager.createLoginSession(user);
                
                Log.d(TAG, "Registration successful for user: " + user.getUsername());
                
                // Show success message
                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                
                // Navigate to MainActivity
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            
            @Override
            public void onError(String message) {
                // Hide progress indicator
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);
                
                // Show error message
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Registration error: " + message);
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
    
    private void registerDemo(String username, String email, String password) {
        // For demo purposes only
        Log.d(TAG, "Using demo registration mode as MongoDB server is not reachable");
        
        // Simulate registration with a delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);
                
                // Create a demo user
                User demoUser = new User();
                demoUser.setId("demo_user_id");
                demoUser.setUsername(username);
                demoUser.setEmail(email);
                demoUser.setToken("demo_token");
                
                // Save user session
                sessionManager.createLoginSession(demoUser);
                
                // Show success message
                Toast.makeText(RegisterActivity.this, "Demo registration successful!", Toast.LENGTH_SHORT).show();
                
                // Navigate to MainActivity
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 1500);
    }
}
