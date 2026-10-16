package com.example.edutech.admin.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.adapters.StaffAdapter;
import com.example.edutech.R;
import com.example.edutech.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminStaffFragment extends Fragment {

    private RecyclerView recyclerView;
    private StaffAdapter staffAdapter;
    private List<User> staffList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_staff, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.staffRecyclerView);

        staffList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 🔥 Add delete listener here
        staffAdapter = new StaffAdapter(staffList, position -> {

            User user = staffList.get(position);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Staff")
                    .setMessage("Are you sure you want to delete this staff?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        db.collection("users")
                                .document(user.getUserId())
                                .delete()
                                .addOnSuccessListener(v -> {

                                    staffList.remove(position);
                                    staffAdapter.notifyItemRemoved(position);

                                    Toast.makeText(getContext(),
                                            "Staff deleted successfully",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Delete failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        recyclerView.setAdapter(staffAdapter);

        fetchStaffData();

        return view;
    }

    private void fetchStaffData() {

        db.collection("users")
                .whereEqualTo("role", "Teacher")
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        staffList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {

                            User user = document.toObject(User.class);

                            // 🔥 IMPORTANT
                            user.setUserId(document.getId());

                            staffList.add(user);
                        }

                        staffAdapter.notifyDataSetChanged();

                    } else {

                        Log.w("AdminStaffFragment",
                                "Error getting documents.",
                                task.getException());

                        Toast.makeText(getContext(),
                                "Error fetching staff data.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}