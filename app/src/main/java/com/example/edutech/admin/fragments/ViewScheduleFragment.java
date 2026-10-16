package com.example.edutech.admin.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.example.edutech.admin.dialogs.AdminDateFilterDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ViewScheduleFragment extends Fragment
        implements ShiftAdapter.OnShiftListener {

    private RecyclerView scheduleRecyclerView;
    private View emptyView;
    private ShiftAdapter shiftAdapter;
    private final List<Shift> shiftList = new ArrayList<>();
    private FirebaseFirestore db;

    // =====================
    // LIFECYCLE
    // =====================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_schedule, container, false);

        db = FirebaseFirestore.getInstance();

        scheduleRecyclerView = view.findViewById(R.id.scheduleRecyclerView);
        emptyView = view.findViewById(R.id.empty_view);

        shiftAdapter = new ShiftAdapter(
                shiftList,
                true,   // ADMIN ACTIONS ENABLED
                this
        );

        scheduleRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        scheduleRecyclerView.setAdapter(shiftAdapter);

        loadTodaySchedules();

        return view;
    }

    // =====================
    // MENU FILTER
    // =====================

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu,
                                    @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_schedule_filter, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_filter) {

            new AdminDateFilterDialog(
                    (fy, fm, fd, ty, tm, td) ->
                            applyDateFilter(fy, fm, fd, ty, tm, td)
            ).show(getChildFragmentManager(), "date_filter");

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // =====================
    // LOAD SCHEDULES
    // =====================

    private void loadTodaySchedules() {

        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        Timestamp start = new Timestamp(cal.getTime());

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        Timestamp end = new Timestamp(cal.getTime());

        fetchSchedulesByDate(start, end);
    }

    private void fetchSchedulesByDate(Timestamp from, Timestamp to) {

        db.collection("schedules")
                .whereGreaterThanOrEqualTo("startTime", from)
                .whereLessThanOrEqualTo("startTime", to)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(query -> {

                    shiftList.clear();

                    for (QueryDocumentSnapshot doc : query) {

                        Shift shift = doc.toObject(Shift.class);
                        shift.setDocumentId(doc.getId());
                        shiftList.add(shift);
                    }

                    shiftAdapter.notifyDataSetChanged();

                    boolean empty = shiftList.isEmpty();

                    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                    scheduleRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminViewSchedule", "Load failed", e);
                    Toast.makeText(getContext(),
                            "Failed to load schedules",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void applyDateFilter(int fy, int fm, int fd,
                                 int ty, int tm, int td) {

        Calendar from = Calendar.getInstance();
        from.set(fy, fm, fd, 0, 0);

        Calendar to = Calendar.getInstance();
        to.set(ty, tm, td, 23, 59);

        fetchSchedulesByDate(
                new Timestamp(from.getTime()),
                new Timestamp(to.getTime())
        );
    }

    // =====================
    // DELETE
    // =====================

    @Override
    public void onDeleteClick(int position) {

        Shift shift = shiftList.get(position);

        if (!"scheduled".equals(shift.getStatus())) {
            Toast.makeText(getContext(),
                    "Only scheduled shifts can be deleted",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Schedule")
                .setMessage("Delete this scheduled shift?")
                .setPositiveButton("Delete", (d, w) ->
                        db.collection("schedules")
                                .document(shift.getDocumentId())
                                .delete()
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(getContext(),
                                            "Schedule deleted",
                                            Toast.LENGTH_SHORT).show();
                                    loadTodaySchedules();
                                })
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =====================
    // REASSIGN (WITH PROXY SUPPORT)
    // =====================

    @Override
    public void onReassignClick(int position) {

        Shift shift = shiftList.get(position);

        if (!"unavailable".equals(shift.getStatus())) {
            Toast.makeText(getContext(),
                    "Only unavailable shifts can be reassigned",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔒 BLOCK IF TIME FINISHED
        long now = System.currentTimeMillis();
        long shiftEnd = shift.getEndTime().toDate().getTime();

        if (now > shiftEnd) {
            Toast.makeText(getContext(),
                    "Cannot reassign. Lecture time already finished.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        showReassignDialog(shift);
    }

    private void showReassignDialog(Shift shift) {

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_reassign_shift, null);

        TextView reasonText = view.findViewById(R.id.textReason);
        Spinner staffSpinner = view.findViewById(R.id.spinnerStaff);

        reasonText.setText("Reason:\n" + shift.getUnavailableReason());

        long newStart = shift.getStartTime().toDate().getTime();
        long newEnd = shift.getEndTime().toDate().getTime();

        db.collection("users")
                .whereEqualTo("role", "Teacher")
                .get()
                .addOnSuccessListener(users -> {

                    List<String> names = new ArrayList<>();
                    List<String> ids = new ArrayList<>();

                    int totalTeachers = users.size();
                    final int[] checkedCount = {0};

                    for (DocumentSnapshot userDoc : users) {

                        String teacherId = userDoc.getId();

                        // ❌ Skip same teacher
                        if (shift.getUserId() != null &&
                                shift.getUserId().equals(teacherId)) {
                            checkedCount[0]++;
                            continue;
                        }

                        // 🔍 CHECK SCHEDULE CONFLICT
                        db.collection("schedules")
                                .whereEqualTo("userId", teacherId)
                                .get()
                                .addOnSuccessListener(scheduleSnap -> {

                                    boolean isAvailable = true;

                                    for (DocumentSnapshot doc : scheduleSnap) {

                                        Timestamp startT = doc.getTimestamp("startTime");
                                        Timestamp endT = doc.getTimestamp("endTime");

                                        if (startT == null || endT == null) continue;

                                        long existingStart = startT.toDate().getTime();
                                        long existingEnd = endT.toDate().getTime();

                                        // 🔥 TIME OVERLAP CHECK
                                        if (newStart < existingEnd && newEnd > existingStart) {
                                            isAvailable = false;
                                            break;
                                        }
                                    }

                                    // 🔍 CHECK LEAVE
                                    boolean finalIsAvailable = isAvailable;
                                    db.collection("leaveRequests")
                                            .whereEqualTo("userId", teacherId)
                                            .whereEqualTo("status", "Approved")
                                            .get()
                                            .addOnSuccessListener(leaveSnap -> {

                                                boolean isOnLeave = false;

                                                for (DocumentSnapshot leaveDoc : leaveSnap) {

                                                    Timestamp leaveDate = leaveDoc.getTimestamp("requestDate");

                                                    if (leaveDate != null) {

                                                        long leaveTime = leaveDate.toDate().getTime();

                                                        // 🔥 SAME DAY CHECK
                                                        if (Math.abs(leaveTime - newStart) < (24 * 60 * 60 * 1000)) {
                                                            isOnLeave = true;
                                                            break;
                                                        }
                                                    }
                                                }

                                                // ✅ FINAL CONDITION
                                                if (finalIsAvailable && !isOnLeave) {
                                                    names.add(userDoc.getString("firstName"));
                                                    ids.add(teacherId);
                                                }

                                                checkedCount[0]++;

                                                // ✅ WAIT UNTIL ALL TEACHERS CHECKED
                                                if (checkedCount[0] == totalTeachers) {

                                                    if (names.isEmpty()) {
                                                        Toast.makeText(getContext(),
                                                                "No available teacher",
                                                                Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }

                                                    ArrayAdapter<String> adapter =
                                                            new ArrayAdapter<>(requireContext(),
                                                                    android.R.layout.simple_spinner_item,
                                                                    names);

                                                    adapter.setDropDownViewResource(
                                                            android.R.layout.simple_spinner_dropdown_item);

                                                    staffSpinner.setAdapter(adapter);

                                                    new AlertDialog.Builder(requireContext())
                                                            .setTitle("Reassign Shift")
                                                            .setView(view)
                                                            .setPositiveButton("Assign", (d, w) -> {

                                                                int pos = staffSpinner.getSelectedItemPosition();
                                                                reassignShift(shift,
                                                                        ids.get(pos),
                                                                        names.get(pos));
                                                            })
                                                            .setNegativeButton("Cancel", null)
                                                            .show();
                                                }
                                            });
                                });
                    }
                });
    }

    private void reassignShift(Shift shift,
                               String userId,
                               String userName) {

        db.collection("schedules")
                .document(shift.getDocumentId())
                .update(
                        "userId", userId,
                        "userName", userName,
                        "status", "scheduled",
                        "unavailableReason", null,
                        "isProxy", true   // 🔥 PROXY FLAG
                )
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(),
                            "Shift reassigned successfully",
                            Toast.LENGTH_SHORT).show();
                    loadTodaySchedules();
                });
    }

    // =====================
    // UNUSED (TEACHER)
    // =====================

    @Override public void onMarkCompleted(int position) {}
    @Override public void onMarkUnavailable(int position) {}
}