package com.example.edutech.admin.fragments;

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

import com.example.edutech.Announcement;
import com.example.edutech.R;
import com.example.edutech.adapters.AnnouncementAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminAnnouncementsFragment extends Fragment implements AnnouncementAdapter.OnAnnouncementListener {

    private RecyclerView recyclerView;
    private AnnouncementAdapter adapter;
    private List<Announcement> list;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_announcements_admin, container, false);

        db = FirebaseFirestore.getInstance();

        FloatingActionButton fab = view.findViewById(R.id.fab_add_announcement);

        // --- THIS IS THE CORRECTED CLICK LISTENER ---
        fab.setOnClickListener(v -> {
            // Create an instance of the fragment you want to open
            Fragment createFragment = new CreateAnnouncementFragment();

            // Use the FragmentManager to replace the current fragment
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, createFragment);
            transaction.addToBackStack(null); // This allows the user to press the back button
            transaction.commit();
        });

        recyclerView = view.findViewById(R.id.announcementsRecyclerView);
        list = new ArrayList<>();
        adapter = new AnnouncementAdapter(list, true, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fetch announcements every time the fragment is shown
        fetchAnnouncements();
    }

    private void fetchAnnouncements() {
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        list.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Announcement announcement = document.toObject(Announcement.class);
                            announcement.setDocumentId(document.getId());
                            list.add(announcement);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error fetching announcements", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onDeleteClick(int position) {
        if (getContext() == null) return; // Prevent crash if fragment is detached

        Announcement announcementToDelete = list.get(position);
        String docId = announcementToDelete.getDocumentId();

        String dialogTitle;
        String dialogMessage;

        if (announcementToDelete.getExpiryTimestamp() != null &&
                announcementToDelete.getExpiryTimestamp().toDate().before(new java.util.Date())) {
            dialogTitle = "Delete Expired Announcement?";
            dialogMessage = "This is already hidden from users. Are you sure you want to permanently delete it?";
        } else {
            dialogTitle = "Delete for Everyone?";
            dialogMessage = "This will permanently delete the announcement for all users.";
        }

        new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setMessage(dialogMessage)
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("announcements").document(docId).delete()
                            .addOnSuccessListener(aVoid -> {
                                list.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(getContext(), "Announcement deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error deleting announcement", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_delete)
                .show();
    }
}