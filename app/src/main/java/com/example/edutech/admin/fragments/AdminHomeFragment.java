package com.example.edutech.admin.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.R;
import com.example.edutech.Shift;
import com.example.edutech.adapters.ShiftAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AdminHomeFragment extends Fragment {

    private TextView textViewShiftsToday;
    private TextView textViewOnLeave;
    private TextView textViewNoSchedule;

    private RecyclerView todaysScheduleRecyclerView;
    private ShiftAdapter shiftAdapter;
    private final List<Shift> todaysShiftList = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_admin_home, container, false);

        db = FirebaseFirestore.getInstance();

        textViewShiftsToday = view.findViewById(R.id.textViewShiftsTodayCount);
        textViewOnLeave = view.findViewById(R.id.textViewOnLeaveCount);
        textViewNoSchedule = view.findViewById(R.id.textViewNoSchedule);
        todaysScheduleRecyclerView =
                view.findViewById(R.id.todaysScheduleRecyclerView);

        // ✅ READ-ONLY ADAPTER (NO ADMIN ACTIONS)
        shiftAdapter = new ShiftAdapter(
                todaysShiftList,
                false,   // ❌ admin actions disabled
                null
        );

        todaysScheduleRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        todaysScheduleRecyclerView.setAdapter(shiftAdapter);

        loadDashboardData();
        return view;
    }

    // =========================
    // LOAD DASHBOARD DATA
    // =========================
    private void loadDashboardData() {

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Timestamp startOfDay = new Timestamp(cal.getTime());

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Timestamp endOfDay = new Timestamp(cal.getTime());

        fetchShiftsTodayCount(startOfDay, endOfDay);
        fetchOnLeaveTodayCount(startOfDay, endOfDay);
        fetchTodaysSchedule(startOfDay, endOfDay);
    }

    // =========================
    // SHIFTS TODAY COUNT
    // =========================
    private void fetchShiftsTodayCount(Timestamp start, Timestamp end) {

        db.collection("schedules")
                .whereGreaterThanOrEqualTo("startTime", start)
                .whereLessThanOrEqualTo("startTime", end)
                .get()
                .addOnSuccessListener(q ->
                        textViewShiftsToday.setText(
                                String.valueOf(q.size()))
                )
                .addOnFailureListener(e ->
                        textViewShiftsToday.setText("0")
                );
    }

    // =========================
    // TEACHERS ON LEAVE COUNT
    // =========================
    private void fetchOnLeaveTodayCount(Timestamp start, Timestamp end) {

        db.collection("leaveRequests")
                .whereEqualTo("status", "approved")
                .whereLessThanOrEqualTo("startDate", end)
                .whereGreaterThanOrEqualTo("endDate", start)
                .get()
                .addOnSuccessListener(q ->
                        textViewOnLeave.setText(
                                String.valueOf(q.size()))
                )
                .addOnFailureListener(e ->
                        textViewOnLeave.setText("0")
                );
    }

    // =========================
    // TODAY'S SCHEDULE LIST
    // =========================
    private void fetchTodaysSchedule(Timestamp start, Timestamp end) {

        db.collection("schedules")
                .whereGreaterThanOrEqualTo("startTime", start)
                .whereLessThanOrEqualTo("startTime", end)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(query -> {

                    todaysShiftList.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        Shift shift = doc.toObject(Shift.class);
                        shift.setDocumentId(doc.getId());
                        todaysShiftList.add(shift);
                    }

                    shiftAdapter.notifyDataSetChanged();

                    boolean empty = todaysShiftList.isEmpty();
                    todaysScheduleRecyclerView.setVisibility(
                            empty ? View.GONE : View.VISIBLE);
                    textViewNoSchedule.setVisibility(
                            empty ? View.VISIBLE : View.GONE);
                });
    }
}
