package com.example.edutech.teacher.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edutech.Announcement;
import com.example.edutech.LeaveRequest;
import com.example.edutech.R;
import com.example.edutech.Shift;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TeacherHomeFragment extends Fragment {

    private TextView textViewNextShiftTime, textViewNextShiftDetails, textViewLeaveStatus;
    private LinearLayout layoutAnnouncements;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        textViewNextShiftTime = view.findViewById(R.id.textViewNextShiftTime);
        textViewNextShiftDetails = view.findViewById(R.id.textViewNextShiftDetails);
        textViewLeaveStatus = view.findViewById(R.id.textViewLeaveStatus);
        layoutAnnouncements = view.findViewById(R.id.layoutAnnouncements);

        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String currentUserId = currentUser.getUid();

        fetchNextShift(currentUserId);
        fetchLeaveStatus(currentUserId);
        fetchRecentAnnouncements();
    }

    private void fetchNextShift(String userId) {
        db.collection("schedules")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("startTime", Timestamp.now())
                .orderBy("startTime", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (getContext() == null) return;
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Shift nextShift = task.getResult().getDocuments().get(0).toObject(Shift.class);
                        if (nextShift != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d 'at' hh:mm a", Locale.getDefault());
                            textViewNextShiftTime.setText(sdf.format(nextShift.getStartTime().toDate()));
                            textViewNextShiftDetails.setText(nextShift.getShiftDetails());
                        }
                    } else {
                        textViewNextShiftTime.setText("No upcoming shifts");
                        textViewNextShiftDetails.setText("Enjoy your day!");
                    }
                });
    }

    private void fetchLeaveStatus(String userId) {
        db.collection("leaveRequests")
                .whereEqualTo("userId", userId)
                .orderBy("requestTimestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (getContext() == null) return;
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        LeaveRequest lastRequest = task.getResult().getDocuments().get(0).toObject(LeaveRequest.class);
                        if (lastRequest != null) {
                            textViewLeaveStatus.setText("Your last request is " + lastRequest.getStatus());
                        }
                    } else {
                        textViewLeaveStatus.setText("You have not submitted any leave requests.");
                    }
                });
    }

    private void fetchRecentAnnouncements() {
        db.collection("announcements")
                .whereGreaterThan("expiryTimestamp", Timestamp.now())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .addOnCompleteListener(task -> {
                    if (getContext() == null) return;
                    if (task.isSuccessful()) {
                        layoutAnnouncements.removeAllViews();
                        if (task.getResult().isEmpty()) {
                            TextView noAnnouncements = new TextView(getContext());
                            noAnnouncements.setText("No recent announcements.");
                            layoutAnnouncements.addView(noAnnouncements);
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Announcement announcement = document.toObject(Announcement.class);
                                TextView announcementTitle = new TextView(getContext());
                                announcementTitle.setText("• " + announcement.getTitle());
                                // --- THIS IS THE CORRECTED WAY TO SET THE STYLE ---
                                announcementTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Sets text size to 16sp
                                announcementTitle.setTypeface(null, Typeface.NORMAL);
                                layoutAnnouncements.addView(announcementTitle);
                            }
                        }
                    }
                });
    }
}
