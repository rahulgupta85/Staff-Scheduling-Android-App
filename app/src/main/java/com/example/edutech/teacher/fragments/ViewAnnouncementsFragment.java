package com.example.edutech.teacher.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.Announcement;
import com.example.edutech.R;
import com.example.edutech.adapters.AnnouncementAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewAnnouncementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyView;
    private AnnouncementAdapter adapter;
    private List<Announcement> list;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_announcements_teacher, container, false);

        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.announcementsRecyclerView);
        emptyView = view.findViewById(R.id.empty_view);
        list = new ArrayList<>();
        adapter = new AnnouncementAdapter(list, false, null);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fetchAnnouncements();

        return view;
    }

    private void fetchAnnouncements() {
        db.collection("announcements")
                .whereGreaterThan("expiryTimestamp", Timestamp.now())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (getContext() == null) return;

                    if (task.isSuccessful()) {
                        list.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            list.add(document.toObject(Announcement.class));
                        }
                        adapter.notifyDataSetChanged();

                        // --- THIS IS THE LOGIC THAT SHOWS YOUR IMAGE ---
                        if (list.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);

                            // Find the ImageView inside the empty view
                            ImageView pandaImage = emptyView.findViewById(R.id.imageViewPanda);
                            // Set the static drawable resource
                            pandaImage.setImageResource(R.drawable.ic_panda_sleeping);

                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error fetching announcements", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
