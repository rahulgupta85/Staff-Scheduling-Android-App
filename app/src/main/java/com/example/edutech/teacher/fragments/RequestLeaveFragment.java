package com.example.edutech.teacher.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edutech.R;
import com.example.edutech.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class RequestLeaveFragment extends Fragment {

    private AutoCompleteTextView autoCompleteAdmins;
    private TextInputEditText editTextStartDate, editTextEndDate, etReason;
    private Button btnSubmit;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private List<User> adminList = new ArrayList<>();
    private List<String> adminNameList = new ArrayList<>();
    private ArrayAdapter<String> adminAdapter;
    private User selectedAdmin;
    private boolean isStartDateSet = false;
    private boolean isEndDateSet = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request_leave, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        autoCompleteAdmins = view.findViewById(R.id.autoCompleteTextViewAdmins);
        editTextStartDate = view.findViewById(R.id.editTextStartDate);
        editTextEndDate = view.findViewById(R.id.editTextEndDate);
        btnSubmit = view.findViewById(R.id.btnSubmitLeaveRequest);
        etReason = view.findViewById(R.id.editTextLeaveReason);

        adminAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, adminNameList);
        autoCompleteAdmins.setAdapter(adminAdapter);

        populateAdminDropdown();

        autoCompleteAdmins.setOnItemClickListener((parent, v, position, id) -> selectedAdmin = adminList.get(position));
        editTextStartDate.setOnClickListener(v -> showDatePicker(true));
        editTextEndDate.setOnClickListener(v -> showDatePicker(false));
        btnSubmit.setOnClickListener(v -> submitLeaveRequest());

        return view;
    }

    private void populateAdminDropdown() {
        db.collection("users").whereEqualTo("role", "Admin").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    adminList.clear();
                    adminNameList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User admin = document.toObject(User.class);
                        admin.setUserId(document.getId());
                        adminList.add(admin);
                        adminNameList.add(admin.getFirstName() + " " + admin.getLastName());
                    }
                    adminAdapter.notifyDataSetChanged();
                });
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar cal = isStartDate ? startDate : endDate;
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            cal.set(year, month, day);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
            if (isStartDate) {
                editTextStartDate.setText(sdf.format(cal.getTime()));
                isStartDateSet = true;
            } else {
                editTextEndDate.setText(sdf.format(cal.getTime()));
                isEndDateSet = true;
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void submitLeaveRequest() {
        String reason = etReason.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (selectedAdmin == null || !isStartDateSet || !isEndDateSet || reason.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) return;

        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);

        if (endDate.before(startDate)) {
            Toast.makeText(getContext(), "End date cannot be before the start date", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String userName = currentUser.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = currentUser.getEmail();
        }

        Map<String, Object> leaveRequest = new HashMap<>();
        leaveRequest.put("userId", userId);
        leaveRequest.put("userName", userName);
        leaveRequest.put("reason", reason);
        leaveRequest.put("startDate", new Timestamp(startDate.getTime()));
        leaveRequest.put("endDate", new Timestamp(endDate.getTime()));
        leaveRequest.put("status", "pending");
        leaveRequest.put("requestTimestamp", Timestamp.now());
        leaveRequest.put("assignedAdminId", selectedAdmin.getUserId());
        leaveRequest.put("assignedAdminName", selectedAdmin.getFirstName() + " " + selectedAdmin.getLastName());

        final String finalUserName = userName;
        db.collection("leaveRequests").add(leaveRequest)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Leave request submitted", Toast.LENGTH_SHORT).show();

                    // --- ADDED THIS NOTIFICATION LOGIC ---
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("userId", selectedAdmin.getUserId()); // Notify the selected admin
                    notification.put("title", "New Leave Request");
                    notification.put("message", finalUserName + " has requested leave.");
                    notification.put("timestamp", Timestamp.now());
                    notification.put("isRead", false);
                    db.collection("notifications").add(notification);
                    // --- END OF NEW LOGIC ---

                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to submit request", Toast.LENGTH_SHORT).show());
    }
}
