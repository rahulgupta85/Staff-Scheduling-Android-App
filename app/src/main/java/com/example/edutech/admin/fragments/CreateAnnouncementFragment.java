package com.example.edutech.admin.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edutech.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CreateAnnouncementFragment extends Fragment {

    private TextInputEditText editTextTitle, editTextMessage, editTextExpiry;
    private Button btnPost;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar expiryDateTime = Calendar.getInstance();
    private boolean isExpirySet = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_announcement, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editTextTitle = view.findViewById(R.id.editTextAnnouncementTitle);
        editTextMessage = view.findViewById(R.id.editTextAnnouncementMessage);
        btnPost = view.findViewById(R.id.btnPostAnnouncement);
        editTextExpiry = view.findViewById(R.id.editTextExpiry);

        btnPost.setOnClickListener(v -> postAnnouncement());
        editTextExpiry.setOnClickListener(v -> selectExpiryDateTime());

        return view;
    }

    private void selectExpiryDateTime() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            expiryDateTime.set(year, month, dayOfMonth);
            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (timeView, hourOfDay, minute) -> {
                expiryDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                expiryDateTime.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault());
                editTextExpiry.setText(sdf.format(expiryDateTime.getTime()));
                isExpirySet = true;
            }, expiryDateTime.get(Calendar.HOUR_OF_DAY), expiryDateTime.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        }, expiryDateTime.get(Calendar.YEAR), expiryDateTime.get(Calendar.MONTH), expiryDateTime.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void postAnnouncement() {
        String title = editTextTitle.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty() || !isExpirySet) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String adminId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        db.collection("users").document(adminId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String adminName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName");

                        Map<String, Object> announcement = new HashMap<>();
                        announcement.put("title", title);
                        announcement.put("message", message);
                        announcement.put("authorName", adminName);
                        announcement.put("timestamp", Timestamp.now());
                        announcement.put("expiryTimestamp", new Timestamp(expiryDateTime.getTime()));

                        db.collection("announcements").add(announcement)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getContext(), "Announcement posted!", Toast.LENGTH_SHORT).show();
                                    notifyAllTeachers(title, message);
                                    getParentFragmentManager().popBackStack();
                                });
                    }
                });
    }

    private void notifyAllTeachers(String title, String message) {
        db.collection("users").whereEqualTo("role", "Teacher").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot teacherDoc : queryDocumentSnapshots) {
                        String teacherId = teacherDoc.getId();
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", teacherId);
                        notification.put("title", "New Announcement: " + title);
                        notification.put("message", message);
                        notification.put("timestamp", Timestamp.now());
                        notification.put("isRead", false);
                        batch.set(db.collection("notifications").document(), notification);
                    }
                    batch.commit();
                });
    }
}
