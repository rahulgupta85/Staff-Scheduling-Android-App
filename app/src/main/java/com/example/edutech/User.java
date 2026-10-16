package com.example.edutech;

public class User {
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String userId; // Field to hold the document ID

    // Required empty public constructor for Firestore
    public User() {}

    // --- Getters ---
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getUserId() { return userId; }

    // --- Setters ---
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // ADDED THIS METHOD
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // ADDED THIS METHOD
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
