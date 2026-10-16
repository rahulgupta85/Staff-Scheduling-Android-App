package com.example.edutech.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.edutech.R;
import com.example.edutech.User;
import com.example.edutech.authViewModel;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView textName, textEmail;
    private Chip chipRole;
    private Button btnLogout, btnEditProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUserData; // To store the loaded user data

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        textName = view.findViewById(R.id.textViewProfileName);
        textEmail = view.findViewById(R.id.textViewProfileEmail);
        chipRole = view.findViewById(R.id.chipProfileRole);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // Set listeners
        btnLogout.setOnClickListener(v -> logoutUser());
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Load the user's data
        loadUserProfile();

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUserData = documentSnapshot.toObject(User.class);
                            if (currentUserData != null) {
                                updateUI();
                            }
                        }
                    });
        }
    }

    private void updateUI() {
        if (currentUserData != null) {
            textName.setText(currentUserData.getFirstName() + " " + currentUserData.getLastName());
            textEmail.setText(currentUserData.getEmail());
            chipRole.setText(currentUserData.getRole());
        }
    }

    private void showEditProfileDialog() {
        if (currentUserData == null || getContext() == null) {
            Toast.makeText(getContext(), "User data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Profile");

        // Create a layout for the dialog
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText firstNameInput = new EditText(getContext());
        firstNameInput.setHint("First Name");
        firstNameInput.setText(currentUserData.getFirstName());
        layout.addView(firstNameInput);

        final EditText lastNameInput = new EditText(getContext());
        lastNameInput.setHint("Last Name");
        lastNameInput.setText(currentUserData.getLastName());
        layout.addView(lastNameInput);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newFirstName = firstNameInput.getText().toString().trim();
            String newLastName = lastNameInput.getText().toString().trim();

            if (newFirstName.isEmpty() || newLastName.isEmpty()) {
                Toast.makeText(getContext(), "Names cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserNameInFirestore(newFirstName, newLastName);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUserNameInFirestore(String firstName, String lastName) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            Map<String, Object> nameUpdates = new HashMap<>();
            nameUpdates.put("firstName", firstName);
            nameUpdates.put("lastName", lastName);

            db.collection("users").document(uid).update(nameUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        // Update the local data and refresh the UI
                        currentUserData.setFirstName(firstName);
                        currentUserData.setLastName(lastName);
                        updateUI();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show());
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        if (getContext() != null) {
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), authViewModel.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
