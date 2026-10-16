package com.example.edutech.teacher;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.edutech.R;
import com.example.edutech.User;
import com.google.android.material.appbar.MaterialToolbar;
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

public class RequestLeaveActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteAdmins;
    private Button btnStartDate, btnEndDate, btnSubmit;
    private TextInputEditText etReason;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private List<User> adminList = new ArrayList<>();
    private List<String> adminNameList = new ArrayList<>();
    private ArrayAdapter<String> adminAdapter;
    private User selectedAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_leave);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        autoCompleteAdmins = findViewById(R.id.autoCompleteTextViewAdmins);
        btnStartDate = findViewById(R.id.btnSelectStartDate);
        btnEndDate = findViewById(R.id.btnSelectEndDate);
        btnSubmit = findViewById(R.id.btnSubmitLeaveRequest);
        etReason = findViewById(R.id.editTextLeaveReason);

        topAppBar.setNavigationOnClickListener(v -> finish());
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnSubmit.setOnClickListener(v -> submitLeaveRequest());

        adminAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, adminNameList);
        autoCompleteAdmins.setAdapter(adminAdapter);

        populateAdminDropdown();

        autoCompleteAdmins.setOnItemClickListener((parent, view, position, id) -> selectedAdmin = adminList.get(position));
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
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
            if (isStartDate) btnStartDate.setText(sdf.format(cal.getTime()));
            else btnEndDate.setText(sdf.format(cal.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void submitLeaveRequest() {
        String reason = etReason.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (selectedAdmin == null) {
            Toast.makeText(this, "Please select an admin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) return;

        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);

        if (endDate.before(startDate)) {
            Toast.makeText(this, "End date cannot be before the start date", Toast.LENGTH_SHORT).show();
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

        db.collection("leaveRequests").add(leaveRequest)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Leave request submitted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to submit request", Toast.LENGTH_SHORT).show());
    }
}
