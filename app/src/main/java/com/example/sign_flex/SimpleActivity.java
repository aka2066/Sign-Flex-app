package com.example.sign_flex;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Absolute minimal Activity - no dependencies, no complex code
 */
public class SimpleActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a TextView programmatically (no layout dependency)
        TextView textView = new TextView(this);
        textView.setText("Hello World");
        textView.setTextSize(24);
        textView.setPadding(20, 20, 20, 20);
        
        // Set the TextView as our content
        setContentView(textView);
    }
}
