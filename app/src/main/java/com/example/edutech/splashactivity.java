package com.example.edutech;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.edutech.authViewModel; // ✅ Add this line
import com.example.edutech.admin.AdminDashboardActivity;
import com.example.edutech.teacher.TeacherDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

@SuppressLint("CustomSplashScreen")
public class splashactivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid();
                db.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String role = documentSnapshot.getString("role");
                                if ("Admin".equalsIgnoreCase(role)) {
                                    startActivity(new Intent(splashactivity.this, AdminDashboardActivity.class));
                                } else if ("Teacher".equalsIgnoreCase(role)) {
                                    startActivity(new Intent(splashactivity.this, TeacherDashboardActivity.class));
                                } else {
                                    Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(splashactivity.this, authViewModel.class));
                                }
                            } else {
                                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(splashactivity.this, authViewModel.class));
                            }
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error fetching user role", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(splashactivity.this, authViewModel.class));
                            finish();
                        });
            } else {
                startActivity(new Intent(splashactivity.this, authViewModel.class));
                finish();
            }
        }, 2000); // 2 seconds delay
    }
}
