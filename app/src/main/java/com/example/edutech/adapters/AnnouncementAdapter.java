package com.example.edutech.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // <-- Import
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.Announcement;
import com.example.edutech.R;
import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
    private List<Announcement> announcementList;
    private OnAnnouncementListener listener;
    private boolean isAdmin;

    // Interface for click events
    public interface OnAnnouncementListener {
        void onDeleteClick(int position);
    }

    // Constructor updated to accept listener and admin flag
    public AnnouncementAdapter(List<Announcement> list, boolean isAdmin, OnAnnouncementListener listener) {
        this.announcementList = list;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_announcement, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement item = announcementList.get(position);
        holder.title.setText(item.getTitle());
        holder.message.setText(item.getMessage());
        String meta = "Posted by " + item.getAuthorName() + " on " + item.getFormattedDate();
        holder.meta.setText(meta);

        // Only show the delete button if the user is an admin
        if (isAdmin) {
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return announcementList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, meta;
        ImageButton deleteButton; // <-- Add ImageButton

        public ViewHolder(@NonNull View itemView, OnAnnouncementListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewAnnouncementTitle);
            message = itemView.findViewById(R.id.textViewAnnouncementMessage);
            meta = itemView.findViewById(R.id.textViewAnnouncementMeta);
            deleteButton = itemView.findViewById(R.id.btn_delete_announcement); // <-- Find button

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(position);
                    }
                }
            });
        }
    }
}