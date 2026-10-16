package com.example.edutech.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.LeaveRequest;
import com.example.edutech.R;
import com.example.edutech.adapters.LeaveRequestAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ViewMyLeaveRequestsActivity extends AppCompatActivity implements LeaveRequestAdapter.OnLeaveRequestListener {

    private RecyclerView recyclerView;
    private LeaveRequestAdapter adapter;
    private List<LeaveRequest> requestList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_leave_requests);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fab_add_request);
        fab.setOnClickListener(v -> startActivity(new Intent(this, RequestLeaveActivity.class)));

        recyclerView = findViewById(R.id.myLeaveRequestsRecyclerView);
        requestList = new ArrayList<>();
        adapter = new LeaveRequestAdapter(requestList, false, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void fetchMyLeaveRequests() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("leaveRequests")
                .whereEqualTo("userId", currentUserId)
                .orderBy("requestTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        requestList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LeaveRequest request = document.toObject(LeaveRequest.class);
                            request.setDocumentId(document.getId());
                            requestList.add(request);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMyLeaveRequests();
    }

    @Override
    public void onDeleteClick(int position) {
        LeaveRequest request = requestList.get(position);
        new AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to permanently delete this leave request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("leaveRequests").document(request.getDocumentId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Request deleted", Toast.LENGTH_SHORT).show();
                                fetchMyLeaveRequests();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Empty methods required by the interface, but not used by the teacher
    @Override public void onApproveClick(int position) {}
    @Override public void onDisapproveClick(int position) {}
}