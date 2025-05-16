package com.example.sign_flex;

import androidx.appcompat.app.AppCompatActivity;

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
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";
    
    private TextInputEditText emailEditText;
    private Button resetPasswordButton;
    private TextView backToLoginTextView;
    private ProgressBar progressBar;
    
    private AuthService authService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        // Initialize services
        authService = new AuthService(this);
        
        // Initialize UI components
        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        backToLoginTextView = findViewById(R.id.backToLoginTextView);
        progressBar = findViewById(R.id.progressBar);
        
        // Set click listeners
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPasswordReset();
            }
        });
        
        backToLoginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to login activity
            }
        });
    }
    
    private void requestPasswordReset() {
        // Get input values
        String email = emailEditText.getText().toString().trim();
        
        // Validate input
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
        resetPasswordButton.setEnabled(false);
        
        // Request password reset
        authService.requestPasswordReset(email, new AuthService.ResetCallback() {
            @Override
            public void onSuccess(String message) {
                // Hide progress indicator
                progressBar.setVisibility(View.GONE);
                resetPasswordButton.setEnabled(true);
                
                // Show success message
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Password reset link sent to: " + email);
                
                // Return to login screen after a short delay
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 2000);
            }
            
            @Override
            public void onError(String message) {
                // Hide progress indicator
                progressBar.setVisibility(View.GONE);
                resetPasswordButton.setEnabled(true);
                
                // Show error message
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Password reset error: " + message);
            }
        });
    }
}
