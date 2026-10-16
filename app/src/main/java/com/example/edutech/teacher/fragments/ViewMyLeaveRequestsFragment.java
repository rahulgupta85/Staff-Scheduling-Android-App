package com.example.edutech.teacher.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.LeaveRequest;
import com.example.edutech.R;
import com.example.edutech.adapters.LeaveRequestAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ViewMyLeaveRequestsFragment extends Fragment implements LeaveRequestAdapter.OnLeaveRequestListener {

    private RecyclerView recyclerView;
    private LeaveRequestAdapter adapter;
    private List<LeaveRequest> requestList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_my_leave_requests, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FloatingActionButton fab = view.findViewById(R.id.fab_add_request);

        // --- THIS IS THE CORRECTED CLICK LISTENER ---
        fab.setOnClickListener(v -> {
            // Replace this fragment with the one for creating a new request
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new RequestLeaveFragment());
            transaction.addToBackStack(null); // Allows user to press back
            transaction.commit();
        });

        recyclerView = view.findViewById(R.id.myLeaveRequestsRecyclerView);
        requestList = new ArrayList<>();
        adapter = new LeaveRequestAdapter(requestList, false, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return view;
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
    public void onResume() {
        super.onResume();
        fetchMyLeaveRequests();
    }

    @Override
    public void onDeleteClick(int position) {
        if (getContext() == null) return;

        LeaveRequest request = requestList.get(position);
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to permanently delete this leave request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("leaveRequests").document(request.getDocumentId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Request deleted", Toast.LENGTH_SHORT).show();
                                fetchMyLeaveRequests();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public void onApproveClick(int position) {}
    @Override public void onDisapproveClick(int position) {}
}