package com.example.sign_flex.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sign_flex.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling authentication-related API calls
 */
public class AuthService {
    private static final String TAG = "AuthService";
    
    // Base URL Configuration (Choose the right one for your environment)
    // For Android Emulator use: 10.0.2.2 to access host machine's localhost
    // For physical device testing, replace with your computer's actual local IP address
    private static final String BASE_URL = "http://10.0.2.2:3002"; // Update with your local IP address
    
    // API endpoints
    private static final String LOGIN_URL = BASE_URL + "/api/auth/login";
    private static final String REGISTER_URL = BASE_URL + "/api/auth/register";
    private static final String REQUEST_RESET_URL = BASE_URL + "/api/auth/request-password-reset";
    private static final String RESET_PASSWORD_URL = BASE_URL + "/api/auth/reset-password";
    private static final String PROFILE_URL = BASE_URL + "/api/auth/profile";
    private static final String ME_URL = BASE_URL + "/api/auth/me";
    
    private RequestQueue requestQueue;
    private Context context;
    
    public AuthService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }
    
    /**
     * Interface for authentication callbacks
     */
    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }
    
    /**
     * Interface for reset password callbacks
     */
    public interface ResetCallback {
        void onSuccess(String message);
        void onError(String message);
    }
    
    /**
     * Login user with email and password
     */
    public void login(String email, String password, final AuthCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    LOGIN_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.has("user") && response.has("token")) {
                                    JSONObject userJson = response.getJSONObject("user");
                                    String token = response.getString("token");
                                    
                                    User user = new User();
                                    user.setId(userJson.getString("_id"));
                                    user.setUsername(userJson.getString("username"));
                                    user.setEmail(userJson.getString("email"));
                                    user.setToken(token);
                                    
                                    callback.onSuccess(user);
                                } else {
                                    callback.onError("Invalid response from server");
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                callback.onError("Error parsing server response");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "Login failed";
                            if (error.networkResponse != null) {
                                if (error.networkResponse.statusCode == 401) {
                                    errorMessage = "Invalid email or password";
                                } else if (error.networkResponse.statusCode == 500) {
                                    errorMessage = "Server error, please try again later";
                                }
                            }
                            Log.e(TAG, "Login error: " + error.getMessage());
                            callback.onError(errorMessage);
                        }
                    }
            );
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            callback.onError("Error creating request");
        }
    }
    
    /**
     * Register new user
     */
    public void register(String username, String email, String password, final AuthCallback callback) {
        try {
            // Log registration attempt
            Log.d(TAG, "Attempting to register user: " + email);
            
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            
            // Log request body
            Log.d(TAG, "Registration request to: " + REGISTER_URL);
            Log.d(TAG, "Registration request body: " + jsonBody.toString());
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    REGISTER_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "Registration response received: " + response.toString());
                            try {
                                if (response.has("user") && response.has("token")) {
                                    JSONObject userJson = response.getJSONObject("user");
                                    String token = response.getString("token");
                                    
                                    User user = new User();
                                    user.setId(userJson.getString("_id"));
                                    user.setUsername(userJson.getString("username"));
                                    user.setEmail(userJson.getString("email"));
                                    user.setToken(token);
                                    
                                    Log.d(TAG, "Registration successful for: " + user.getEmail());
                                    callback.onSuccess(user);
                                } else {
                                    Log.e(TAG, "Invalid response format: " + response.toString());
                                    callback.onError("Invalid response from server");
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                callback.onError("Error parsing server response");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "Registration failed";
                            
                            // Log detailed error information
                            Log.e(TAG, "Registration error occurred");
                            
                            if (error.networkResponse != null) {
                                Log.e(TAG, "Status code: " + error.networkResponse.statusCode);
                                
                                // Try to get response data
                                if (error.networkResponse.data != null) {
                                    try {
                                        String responseBody = new String(error.networkResponse.data, "utf-8");
                                        Log.e(TAG, "Error response body: " + responseBody);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing error response: " + e.getMessage());
                                    }
                                }
                                
                                if (error.networkResponse.statusCode == 400) {
                                    errorMessage = "Email already in use";
                                } else if (error.networkResponse.statusCode == 500) {
                                    errorMessage = "Server error, please try again later";
                                }
                            } else {
                                // No network response usually means a connection issue
                                Log.e(TAG, "Network response is null, likely a connection issue");
                                if (error.getMessage() != null) {
                                    Log.e(TAG, "Error message: " + error.getMessage());
                                }
                                errorMessage = "Connection error, please check your internet connection";
                            }
                            
                            Log.e(TAG, "Final error message: " + errorMessage);
                            callback.onError(errorMessage);
                        }
                    }
            );
            
            // Set a longer timeout for the request
            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000, // 15 seconds
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            Log.d(TAG, "Adding registration request to queue");
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            callback.onError("Error creating request");
        }
    }
    
    /**
     * Request password reset
     */
    public void requestPasswordReset(String email, final ResetCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    REQUEST_RESET_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String message = response.getString("message");
                                callback.onSuccess(message);
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                callback.onError("Error parsing server response");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "Failed to send reset link";
                            if (error.networkResponse != null) {
                                if (error.networkResponse.statusCode == 404) {
                                    errorMessage = "Email not found";
                                } else if (error.networkResponse.statusCode == 500) {
                                    errorMessage = "Server error, please try again later";
                                }
                            }
                            Log.e(TAG, "Reset request error: " + error.getMessage());
                            callback.onError(errorMessage);
                        }
                    }
            );
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            callback.onError("Error creating request");
        }
    }
    
    /**
     * Reset password with token
     */
    public void resetPassword(String resetToken, String newPassword, final ResetCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("resetToken", resetToken);
            jsonBody.put("newPassword", newPassword);
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    RESET_PASSWORD_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String message = response.getString("message");
                                callback.onSuccess(message);
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                callback.onError("Error parsing server response");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "Failed to reset password";
                            if (error.networkResponse != null) {
                                if (error.networkResponse.statusCode == 400) {
                                    errorMessage = "Invalid or expired reset token";
                                } else if (error.networkResponse.statusCode == 500) {
                                    errorMessage = "Server error, please try again later";
                                }
                            }
                            Log.e(TAG, "Password reset error: " + error.getMessage());
                            callback.onError(errorMessage);
                        }
                    }
            );
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            callback.onError("Error creating request");
        }
    }
    
    /**
     * Update user profile
     */
    public void updateProfile(String token, String username, String email, final AuthCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            if (username != null) jsonBody.put("username", username);
            if (email != null) jsonBody.put("email", email);
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    PROFILE_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.has("user")) {
                                    JSONObject userJson = response.getJSONObject("user");
                                    
                                    User user = new User();
                                    user.setId(userJson.getString("_id"));
                                    user.setUsername(userJson.getString("username"));
                                    user.setEmail(userJson.getString("email"));
                                    user.setToken(token); // Reuse the existing token
                                    
                                    callback.onSuccess(user);
                                } else {
                                    callback.onError("Invalid response from server");
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                callback.onError("Error parsing server response");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "Profile update failed";
                            if (error.networkResponse != null) {
                                if (error.networkResponse.statusCode == 400) {
                                    errorMessage = "Email already in use";
                                } else if (error.networkResponse.statusCode == 404) {
                                    errorMessage = "User not found";
                                } else if (error.networkResponse.statusCode == 500) {
                                    errorMessage = "Server error, please try again later";
                                }
                            }
                            Log.e(TAG, "Profile update error: " + error.getMessage());
                            callback.onError(errorMessage);
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + token);
                    return headers;
                }
            };
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            callback.onError("Error creating request");
        }
    }
    
    /**
     * Get current user data
     */
    public void getCurrentUser(String token, final AuthCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ME_URL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("user")) {
                                JSONObject userJson = response.getJSONObject("user");
                                
                                User user = new User();
                                user.setId(userJson.getString("_id"));
                                user.setUsername(userJson.getString("username"));
                                user.setEmail(userJson.getString("email"));
                                user.setToken(token); // Reuse the existing token
                                
                                callback.onSuccess(user);
                            } else {
                                callback.onError("Invalid response from server");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            callback.onError("Error parsing server response");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Failed to get user data";
                        if (error.networkResponse != null) {
                            if (error.networkResponse.statusCode == 401) {
                                errorMessage = "Unauthorized, please login again";
                            } else if (error.networkResponse.statusCode == 404) {
                                errorMessage = "User not found";
                            } else if (error.networkResponse.statusCode == 500) {
                                errorMessage = "Server error, please try again later";
                            }
                        }
                        Log.e(TAG, "Get user error: " + error.getMessage());
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
}
