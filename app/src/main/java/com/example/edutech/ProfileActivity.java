package com.example.edutech;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView textName, textEmail;
    private Chip chipRole;
    private Button btnLogout;
    private MaterialToolbar topAppBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        topAppBar = findViewById(R.id.topAppBar);
        textName = findViewById(R.id.textViewProfileName);
        textEmail = findViewById(R.id.textViewProfileEmail);
        chipRole = findViewById(R.id.chipProfileRole);
        btnLogout = findViewById(R.id.btnLogout);

        // Set listeners
        topAppBar.setNavigationOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> logoutUser());

        // Load the user's data
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                textName.setText(user.getFirstName() + " " + user.getLastName());
                                textEmail.setText(user.getEmail());
                                chipRole.setText(user.getRole());
                            }
                        } else {
                            Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileActivity", "Error fetching user data", e);
                        Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, authViewModel.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}