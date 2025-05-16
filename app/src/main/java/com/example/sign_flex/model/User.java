package com.example.sign_flex.model;

/**
 * User model class to represent user data
 */
public class User {
    private String id;
    private String username;
    private String email;
    private String password; // This will only be used for registration - not stored locally
    private String token;
    
    // Default constructor required for MongoDB document mapping
    public User() {
    }
    
    // Constructor for login
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    // Constructor for registration
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}
