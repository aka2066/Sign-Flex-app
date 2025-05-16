package com.example.sign_flex;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * TestActivity - A completely fresh implementation without any WebView or WebAppInterface
 * This should help isolate the crash issue
 */
public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        
        TextView titleText = findViewById(R.id.test_title);
        if (titleText != null) {
            titleText.setText("SIGN FLEX - BASIC");
        }
        
        TextView statusText = findViewById(R.id.test_status);
        if (statusText != null) {
            statusText.setText("Basic Mode Running");
        }
    }
}
