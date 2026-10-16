package com.example.edutech.admin.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.LeaveRequest;
import com.example.edutech.R;
import com.example.edutech.adapters.LeaveRequestAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageLeaveRequestsFragment extends Fragment implements LeaveRequestAdapter.OnLeaveRequestListener {

    private RecyclerView recyclerView;
    private View emptyView;
    private LeaveRequestAdapter adapter;
    private List<LeaveRequest> requestList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_leave_requests, container, false);

        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.leaveRequestsRecyclerView);
        emptyView = view.findViewById(R.id.empty_view);
        requestList = new ArrayList<>();
        adapter = new LeaveRequestAdapter(requestList, true, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fetchLeaveRequests();

        return view;
    }

    private void fetchLeaveRequests() {
        db.collection("leaveRequests")
                .whereGreaterThanOrEqualTo("endDate", Timestamp.now())
                .orderBy("endDate", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (getContext() == null) return;
                    if (task.isSuccessful()) {
                        requestList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LeaveRequest request = document.toObject(LeaveRequest.class);
                            request.setDocumentId(document.getId());
                            requestList.add(request);
                        }
                        adapter.notifyDataSetChanged();
                        if (requestList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                    }
                });
    }

    @Override
    public void onApproveClick(int position) {
        updateRequestStatus(position, "approved", null);
    }

    @Override
    public void onDisapproveClick(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reason for Disapproval");
        final EditText input = new EditText(requireContext());
        builder.setView(input);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (!reason.isEmpty()) {
                updateRequestStatus(position, "disapproved", reason);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onDeleteClick(int position) {
        // Not used by admin
    }

    private void updateRequestStatus(int position, String status, String reason) {
        if (position >= requestList.size() || position < 0) return;

        LeaveRequest request = requestList.get(position);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        if (reason != null) {
            updates.put("disapprovalReason", reason);
        }

        db.collection("leaveRequests").document(request.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request " + status, Toast.LENGTH_SHORT).show();

                    // --- ADDED THIS NOTIFICATION LOGIC ---
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("userId", request.getUserId()); // Notify the teacher
                    notification.put("title", "Leave Request " + status);
                    notification.put("message", "Your leave request for " + request.getFormattedDateRange() + " has been " + status + ".");
                    notification.put("timestamp", Timestamp.now());
                    notification.put("isRead", false);
                    db.collection("notifications").add(notification);
                    // --- END OF NEW LOGIC ---

                    fetchLeaveRequests();
                });
    }
}
