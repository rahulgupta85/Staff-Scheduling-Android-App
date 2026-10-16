package com.example.edutech.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.R;
import com.example.edutech.User;

import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    private List<User> staffList;
    private OnStaffDeleteListener listener;

    public interface OnStaffDeleteListener {
        void onDeleteClick(int position);
    }

    public StaffAdapter(List<User> staffList, OnStaffDeleteListener listener) {
        this.staffList = staffList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_staff, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        User user = staffList.get(position);

        holder.staffName.setText(user.getFirstName() + " " + user.getLastName());
        holder.staffEmail.setText(user.getEmail());

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView staffName, staffEmail;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            staffName = itemView.findViewById(R.id.textViewStaffName);
            staffEmail = itemView.findViewById(R.id.textViewStaffEmail);
            deleteButton = itemView.findViewById(R.id.btn_delete_staff);
        }
    }
}