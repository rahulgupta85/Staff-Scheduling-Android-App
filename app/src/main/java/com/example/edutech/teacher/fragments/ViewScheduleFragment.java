package com.example.edutech.teacher.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.R;
import com.example.edutech.Shift;
import com.example.edutech.adapters.ShiftAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ViewScheduleFragment extends Fragment
        implements ShiftAdapter.OnShiftListener {

    private CalendarView calendarView;
    private RecyclerView scheduleRecyclerView;
    private ShiftAdapter shiftAdapter;
    private List<Shift> shiftList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_view_schedule_teacher,
                container,
                false
        );

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        calendarView = view.findViewById(R.id.calendarView);
        scheduleRecyclerView = view.findViewById(R.id.scheduleRecyclerView);

        shiftList = new ArrayList<>();

        shiftAdapter = new ShiftAdapter(
                shiftList,
                false, // Teacher view
                this
        );

        scheduleRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        scheduleRecyclerView.setAdapter(shiftAdapter);

        fetchShiftsForDate(Calendar.getInstance());

        calendarView.setOnDateChangeListener((v, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            fetchShiftsForDate(selectedDate);
        });

        return view;
    }

    // ------------------------------------------------
    // FETCH SHIFTS
    // ------------------------------------------------

    private void fetchShiftsForDate(Calendar selectedDate) {

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
        selectedDate.set(Calendar.MINUTE, 0);
        selectedDate.set(Calendar.SECOND, 0);
        Timestamp startOfDay = new Timestamp(selectedDate.getTime());

        selectedDate.set(Calendar.HOUR_OF_DAY, 23);
        selectedDate.set(Calendar.MINUTE, 59);
        selectedDate.set(Calendar.SECOND, 59);
        Timestamp endOfDay = new Timestamp(selectedDate.getTime());

        db.collection("schedules")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThanOrEqualTo("startTime", endOfDay)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(query -> {

                    shiftList.clear();

                    for (QueryDocumentSnapshot document : query) {

                        Shift shift = document.toObject(Shift.class);
                        shift.setDocumentId(document.getId());

                        // 🔥 AUTO LOCK RULE
                        long now = System.currentTimeMillis();
                        long shiftEnd = shift.getEndTime()
                                .toDate()
                                .getTime();

                        long oneDayMillis = 24 * 60 * 60 * 1000;

                        boolean expired =
                                now > (shiftEnd + oneDayMillis);

                        if (expired &&
                                "scheduled".equals(shift.getStatus())) {

                            db.collection("schedules")
                                    .document(shift.getDocumentId())
                                    .update("status", "unavailable");

                            shift.setStatus("unavailable");
                        }

                        shiftList.add(shift);
                    }

                    shiftAdapter.notifyDataSetChanged();

                    if (shiftList.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No shifts scheduled for this day",
                                Toast.LENGTH_SHORT).show();
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("TeacherViewSchedule",
                            "Error fetching schedule",
                            e);
                    Toast.makeText(getContext(),
                            "Error fetching schedule",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ------------------------------------------------
    // MARK COMPLETED
    // ------------------------------------------------

    @Override
    public void onMarkCompleted(int position) {

        if (position >= shiftList.size()) return;

        Shift shift = shiftList.get(position);

        long now = System.currentTimeMillis();
        long startTime = shift.getStartTime().toDate().getTime();
        long endTime = shift.getEndTime().toDate().getTime();

        long twelveHoursMillis = 12 * 60 * 60 * 1000;

        // ✅ Allowed between startTime and endTime + 12 hrs
        if (now < startTime || now > (endTime + twelveHoursMillis)) {

            Toast.makeText(getContext(),
                    "You can mark completed only during lecture time.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("schedules")
                .document(shift.getDocumentId())
                .update("status", "completed",
                        "unavailableReason", null)
                .addOnSuccessListener(v -> {

                    shift.setStatus("completed");
                    shiftAdapter.notifyItemChanged(position);

                    Toast.makeText(getContext(),
                            "Lecture marked as completed",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ------------------------------------------------
    // MARK UNAVAILABLE
    // ------------------------------------------------

    @Override
    public void onMarkUnavailable(int position) {

        if (position >= shiftList.size()) return;

        Shift shift = shiftList.get(position);

        long now = System.currentTimeMillis();
        long startTime = shift.getStartTime().toDate().getTime();

        // ❌ Only allowed BEFORE lecture starts
        if (now >= startTime) {

            Toast.makeText(getContext(),
                    "You can mark unavailable only before lecture starts.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(requireContext());

        builder.setTitle("Reason for Unavailability");

        EditText input = new EditText(requireContext());
        input.setHint("Enter reason...");
        input.setMinLines(3);

        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {

            String reason = input.getText().toString().trim();

            if (reason.isEmpty()) {
                Toast.makeText(getContext(),
                        "Reason cannot be empty",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("schedules")
                    .document(shift.getDocumentId())
                    .update("status", "unavailable",
                            "unavailableReason", reason)
                    .addOnSuccessListener(v -> {

                        shift.setStatus("unavailable");
                        shiftAdapter.notifyItemChanged(position);

                        Toast.makeText(getContext(),
                                "Marked as unavailable",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Not used in teacher
    @Override public void onDeleteClick(int position) {}
    @Override public void onReassignClick(int position) {}
}