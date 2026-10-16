package com.example.edutech.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.Notification;
import com.example.edutech.R;
import com.example.edutech.adapters.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> list;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button markAllReadButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        markAllReadButton = view.findViewById(R.id.btn_mark_all_read);
        markAllReadButton.setOnClickListener(v -> markAllAsRead());

        recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        list = new ArrayList<>();
        adapter = new NotificationAdapter(list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fetchNotifications();

        return view;
    }

    private void fetchNotifications() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (getContext() == null) return; // Safety check

                    if (task.isSuccessful()) {
                        list.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            list.add(document.toObject(Notification.class));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void markAllAsRead() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (getContext() == null) return;

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No unread notifications.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "isRead", true);
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "All notifications marked as read.", Toast.LENGTH_SHORT).show();
                        fetchNotifications(); // Refresh the list
                    });
                });
    }
}