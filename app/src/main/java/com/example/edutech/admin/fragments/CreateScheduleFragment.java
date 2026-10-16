package com.example.edutech.admin.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.fragment.app.Fragment;
import com.example.edutech.R;
import com.example.edutech.User;
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

public class CreateScheduleFragment extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_schedule, container, false);

        db = FirebaseFirestore.getInstance();

        autoCompleteTextViewStaff = view.findViewById(R.id.autoCompleteTextViewStaff);
        editTextDate = view.findViewById(R.id.editTextDate);
        editTextStartTime = view.findViewById(R.id.editTextStartTime);
        editTextEndTime = view.findViewById(R.id.editTextEndTime);
        editTextShiftDetails = view.findViewById(R.id.editTextShiftDetails);
        btnSaveSchedule = view.findViewById(R.id.btnSaveSchedule);

        staffAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, staffNameList);
        autoCompleteTextViewStaff.setAdapter(staffAdapter);

        populateStaffDropdown();
        setupClickListeners();

        return view;
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
                    staffAdapter.notifyDataSetChanged();
                });
    }

    private void setupClickListeners() {
        autoCompleteTextViewStaff.setOnItemClickListener((parent, view, position, id) -> selectedUser = staffList.get(position));
        editTextDate.setOnClickListener(v -> showDatePicker());
        editTextStartTime.setOnClickListener(v -> showTimePicker(true));
        editTextEndTime.setOnClickListener(v -> showTimePicker(false));
        btnSaveSchedule.setOnClickListener(v -> saveSchedule());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (dpView, year, month, day) -> {
            selectedDate.set(year, month, day);
            editTextDate.setText(new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(selectedDate.getTime()));
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar cal = isStartTime ? startTime : endTime;
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (tpView, hour, minute) -> {
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            TextInputEditText targetEditText = isStartTime ? editTextStartTime : editTextEndTime;
            targetEditText.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime()));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private void saveSchedule() {
        if (selectedUser == null || editTextDate.getText().toString().isEmpty() ||
                editTextStartTime.getText().toString().isEmpty() || editTextEndTime.getText().toString().isEmpty() ||
                editTextShiftDetails.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String shiftDetails = editTextShiftDetails.getText().toString().trim();
        Calendar startDateTime = (Calendar) selectedDate.clone();
        startDateTime.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
        startDateTime.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));

        Calendar endDateTime = (Calendar) selectedDate.clone();
        endDateTime.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
        endDateTime.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));

        if (startDateTime.before(Calendar.getInstance())) {
            Toast.makeText(getContext(), "Cannot schedule for a past date or time", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- NEW LOGIC START: Check for leave conflict first ---
        checkForLeaveConflict(selectedUser, startDateTime.getTime(), endDateTime.getTime(), shiftDetails);
    }

    // --- NEW METHOD: Checks if teacher is on approved leave ---
    private void checkForLeaveConflict(User user, Date newStartTime, Date newEndTime, String shiftDetails) {
        db.collection("leaveRequests")
                .whereEqualTo("userId", user.getUserId())
                .whereEqualTo("status", "approved") // Only check approved leaves
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isOnLeave = false;
                    String leaveMessage = "";

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Date leaveStart = document.getTimestamp("startDate").toDate();
                        Date leaveEnd = document.getTimestamp("endDate").toDate();

                        // Check if the schedule overlaps with the leave dates
                        if (newStartTime.before(leaveEnd) && newEndTime.after(leaveStart)) {
                            isOnLeave = true;
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                            leaveMessage = "Teacher is on approved leave from " +
                                    sdf.format(leaveStart) + " to " + sdf.format(leaveEnd) + ".";
                            break;
                        }
                    }

                    if (isOnLeave) {
                        // Show a warning popup
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Schedule Conflict")
                                .setMessage(leaveMessage)
                                .setPositiveButton("OK", null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } else {
                        // No leave conflict, proceed to check for other schedule conflicts
                        checkForConflicts(user, newStartTime, newEndTime, shiftDetails);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error checking leave status", Toast.LENGTH_SHORT).show());
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
                            Toast.makeText(getContext(), "This teacher is already booked for this time.", Toast.LENGTH_LONG).show();
                        } else {
                            proceedToSave(user, newStartTime, newEndTime, shiftDetails);
                        }
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
                    Toast.makeText(getContext(), "Schedule saved!", Toast.LENGTH_SHORT).show();

                    // --- NOTIFICATION LOGIC ---
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("userId", user.getUserId());
                    notification.put("title", "New Schedule Assigned");
                    notification.put("message", "You have a new shift: " + shiftDetails);
                    notification.put("timestamp", Timestamp.now());
                    notification.put("isRead", false);
                    db.collection("notifications").add(notification);
                    // --- END NOTIFICATION LOGIC ---

                    getParentFragmentManager().popBackStack();
                });
    }
}