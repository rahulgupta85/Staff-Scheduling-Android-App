package com.example.edutech.admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateScheduleActivity extends AppCompatActivity {

    // Updated UI component variables
    private AutoCompleteTextView autoCompleteTextViewStaff;
    private TextInputEditText editTextDate, editTextStartTime, editTextEndTime, editTextShiftDetails;
    private Button btnSaveSchedule;

    private FirebaseFirestore db;
    private List<User> staffList = new ArrayList<>();
    private List<String> staffNameList = new ArrayList<>();
    private ArrayAdapter<String> staffAdapter;

    private Calendar selectedDate = Calendar.getInstance();
    private Calendar startTime = Calendar.getInstance();
    private Calendar endTime = Calendar.getInstance();
    private User selectedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_schedule);

        db = FirebaseFirestore.getInstance();

        // Initialize NEW Views from your XML layout
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        autoCompleteTextViewStaff = findViewById(R.id.autoCompleteTextViewStaff);
        editTextDate = findViewById(R.id.editTextDate);
        editTextStartTime = findViewById(R.id.editTextStartTime);
        editTextEndTime = findViewById(R.id.editTextEndTime);
        editTextShiftDetails = findViewById(R.id.editTextShiftDetails);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);

        // Setup Toolbar's back arrow
        topAppBar.setNavigationOnClickListener(v -> finish());

        // Setup the adapter for the new dropdown menu
        staffAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, staffNameList);
        autoCompleteTextViewStaff.setAdapter(staffAdapter);

        // Fetch staff list and set up button clicks
        populateStaffDropdown();
        setupClickListeners();
    }

    private void populateStaffDropdown() {
        db.collection("users").whereEqualTo("role", "Teacher").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    staffList.clear();
                    staffNameList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());
                        staffList.add(user);
                        staffNameList.add(user.getFirstName() + " " + user.getLastName());
                    }
                    if (staffNameList.isEmpty()) {
                        Toast.makeText(this, "No teachers found in database", Toast.LENGTH_LONG).show();
                    }
                    staffAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching staff list", Toast.LENGTH_SHORT).show());
    }

    private void setupClickListeners() {
        // Set listener for when a staff member is selected from the dropdown
        autoCompleteTextViewStaff.setOnItemClickListener((parent, view, position, id) -> {
            selectedUser = staffList.get(position);
        });

        // Set listener for the date field
        editTextDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                editTextDate.setText(new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(selectedDate.getTime()));
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // Set listener for the start time field
        editTextStartTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                startTime.set(Calendar.MINUTE, minute);
                editTextStartTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(startTime.getTime()));
            }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        });

        // Set listener for the end time field
        editTextEndTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endTime.set(Calendar.MINUTE, minute);
                editTextEndTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(endTime.getTime()));
            }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        });

        // Set listener for the Save Button
        btnSaveSchedule.setOnClickListener(v -> saveSchedule());
    }

    private void saveSchedule() {
        // --- Validation ---
        if (selectedUser == null) {
            Toast.makeText(this, "Please select a staff member from the dropdown", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editTextDate.getText().toString().isEmpty() ||
                editTextStartTime.getText().toString().isEmpty() ||
                editTextEndTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select a date, start time, and end time", Toast.LENGTH_SHORT).show();
            return;
        }
        String shiftDetails = editTextShiftDetails.getText().toString().trim();
        if (shiftDetails.isEmpty()) {
            Toast.makeText(this, "Please enter shift details", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Date & Time Combination ---
        Calendar startDateTime = (Calendar) selectedDate.clone();
        startDateTime.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
        startDateTime.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));

        Calendar endDateTime = (Calendar) selectedDate.clone();
        endDateTime.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
        endDateTime.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));

        if (startDateTime.before(Calendar.getInstance())) {
            Toast.makeText(this, "Cannot schedule for a past date or time", Toast.LENGTH_SHORT).show();
            return;
        }

        checkForConflicts(selectedUser, startDateTime.getTime(), endDateTime.getTime(), shiftDetails);
    }

    private void checkForConflicts(User user, Date newStartTime, Date newEndTime, String shiftDetails) {
        db.collection("schedules")
                .whereEqualTo("userId", user.getUserId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isConflict = false;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Date existingStartTime = document.getTimestamp("startTime").toDate();
                            Date existingEndTime = document.getTimestamp("endTime").toDate();
                            if (newStartTime.before(existingEndTime) && newEndTime.after(existingStartTime)) {
                                isConflict = true;
                                break;
                            }
                        }
                        if (isConflict) {
                            Toast.makeText(this, "Time slot is full. This teacher is already scheduled.", Toast.LENGTH_LONG).show();
                        } else {
                            proceedToSave(user, newStartTime, newEndTime, shiftDetails);
                        }
                    } else {
                        Toast.makeText(this, "Could not check for conflicts.", Toast.LENGTH_SHORT).show();
                        Log.e("ConflictCheck", "Error: ", task.getException());
                    }
                });
    }

    private void proceedToSave(User user, Date startTime, Date endTime, String shiftDetails) {
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("userId", user.getUserId());
        scheduleData.put("userName", user.getFirstName() + " " + user.getLastName());
        scheduleData.put("startTime", new Timestamp(startTime));
        scheduleData.put("endTime", new Timestamp(endTime));
        scheduleData.put("shiftDetails", shiftDetails);

        db.collection("schedules").add(scheduleData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}