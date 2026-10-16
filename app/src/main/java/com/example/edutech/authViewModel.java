package com.example.edutech;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.edutech.admin.AdminDashboardActivity;
import com.example.edutech.teacher.TeacherDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class authViewModel extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private EditText signupFirstName, signupLastName, signupEmail, signupPassword;
    private Spinner spinnerRole;
    private LinearLayout loginLayout, signupLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private static final String TAG = "AuthViewModel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Login views
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        // Signup views
        signupFirstName = findViewById(R.id.signupFirstName);
        signupLastName = findViewById(R.id.signupLastName);
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        Button btnSignup = findViewById(R.id.btnSignup);

        // Layouts
        loginLayout = findViewById(R.id.loginLayout);
        signupLayout = findViewById(R.id.signupLayout);
        TextView btnSwitchToSignup = findViewById(R.id.btnSwitchToSignup);
        TextView btnSwitchToLogin = findViewById(R.id.btnSwitchToLogin);

        // Spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.user_roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Switch to Signup
        btnSwitchToSignup.setOnClickListener(v -> {
            loginLayout.setVisibility(View.GONE);
            signupLayout.setVisibility(View.VISIBLE);
        });

        // Switch to Login
        btnSwitchToLogin.setOnClickListener(v -> {
            signupLayout.setVisibility(View.GONE);
            loginLayout.setVisibility(View.VISIBLE);
        });

        // Signup logic
        btnSignup.setOnClickListener(v -> {
            String firstName = signupFirstName.getText().toString().trim();
            String lastName = signupLastName.getText().toString().trim();
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "Please fill all fields and use 6+ characters for password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("firstName", firstName);
                            userData.put("lastName", lastName);
                            userData.put("email", email);
                            userData.put("role", role);

                            firestore.collection("users").document(uid)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Signup successful! Please login.", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                        signupLayout.setVisibility(View.GONE);
                                        loginLayout.setVisibility(View.VISIBLE);
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        }
                    })
                    .addOnFailureListener(e -> {

                        String errorCode = "";

                        if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                            errorCode = ((com.google.firebase.auth.FirebaseAuthException) e).getErrorCode();
                        }

                        switch (errorCode) {

                            case "ERROR_USER_NOT_FOUND":
                                Toast.makeText(this, "User not found. Please sign up first.", Toast.LENGTH_SHORT).show();
                                break;

                            case "ERROR_WRONG_PASSWORD":
                                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                                break;

                            case "ERROR_INVALID_EMAIL":
                                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
                                break;

                            default:
                                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    });
        });

        // Login logic
        btnLogin.setOnClickListener(v -> {

            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRoleAndRedirect(user.getUid());
                        }
                    })
                    .addOnFailureListener(e -> {

                        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                            Toast.makeText(this, "Email not registered", Toast.LENGTH_SHORT).show();

                        } else if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Auto-login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserRoleAndRedirect(currentUser.getUid());
        }
    }

    private void fetchUserRoleAndRedirect(String uid) {

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token: " + token);
                        MyFirebaseMessagingService.sendTokenToFirestore(token);
                    } else {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                    }
                });

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(this,
                                "Your account has been deleted by admin.",
                                Toast.LENGTH_LONG).show();

                        mAuth.signOut();

                        loginLayout.setVisibility(View.VISIBLE);
                        signupLayout.setVisibility(View.GONE);

                        return;
                    }


                    String role = documentSnapshot.getString("role");

                    if (role != null) {

                        Toast.makeText(this,
                                "Login successful",
                                Toast.LENGTH_SHORT).show();

                        redirectToDashboard(role.trim());

                    } else {
                        Toast.makeText(this,
                                "Role not found",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error fetching user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void redirectToDashboard(String role) {
        // ✅ Save role in SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putString("role", role).apply();

        Intent intent;
        if ("Admin".equalsIgnoreCase(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if ("Teacher".equalsIgnoreCase(role)) {
            intent = new Intent(this, TeacherDashboardActivity.class);
        } else {
            Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
        finish(); // Prevent going back to login
    }
}
