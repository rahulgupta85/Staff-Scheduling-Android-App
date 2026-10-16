package com.example.edutech.teacher;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.Announcement;
import com.example.edutech.R;
import com.example.edutech.adapters.AnnouncementAdapter;
import com.google.firebase.Timestamp; // Import Timestamp
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewAnnouncementsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AnnouncementAdapter adapter;
    private List<Announcement> list;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_view_announcements_teacher);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.announcementsRecyclerView);
        list = new ArrayList<>();
        adapter = new AnnouncementAdapter(list, false, null);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchAnnouncements();
    }

    private void fetchAnnouncements() {
        db.collection("announcements")
                // NEW: This line only gets announcements that have not expired
                .whereGreaterThan("expiryTimestamp", Timestamp.now())
                // We still order by the original post time to see newest first
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        list.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            list.add(document.toObject(Announcement.class));
                        }
                        adapter.notifyDataSetChanged();
                        if (list.isEmpty()) {
                            Toast.makeText(this, "No active announcements found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching announcements", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}