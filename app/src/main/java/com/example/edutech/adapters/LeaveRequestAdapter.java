package com.example.edutech.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.edutech.LeaveRequest;
import com.example.edutech.R;
import com.google.android.material.chip.Chip;
import java.util.List;

public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.ViewHolder> {

    private List<LeaveRequest> requestList;
    private OnLeaveRequestListener listener;
    private boolean isAdmin;

    // THIS INTERFACE MUST HAVE ALL THREE METHODS
    public interface OnLeaveRequestListener {
        void onApproveClick(int position);
        void onDisapproveClick(int position);
        void onDeleteClick(int position);
    }

    public LeaveRequestAdapter(List<LeaveRequest> requestList, boolean isAdmin, OnLeaveRequestListener listener) {
        this.requestList = requestList;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_leave_request, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaveRequest request = requestList.get(position);
        holder.userName.setText(request.getUserName());
        holder.dateRange.setText(request.getFormattedDateRange());
        holder.reason.setText(request.getReason());

        String status = request.getStatus();
        holder.status.setText(status);

        holder.disapprovalReason.setVisibility(View.GONE);
        holder.actionButtonsLayout.setVisibility(View.GONE);
        holder.deleteButton.setVisibility(View.GONE);

        if ("approved".equalsIgnoreCase(status)) {
            holder.status.setChipBackgroundColorResource(android.R.color.holo_green_light);
        } else if ("disapproved".equalsIgnoreCase(status)) {
            holder.status.setChipBackgroundColorResource(android.R.color.holo_red_light);
            if (request.getDisapprovalReason() != null && !request.getDisapprovalReason().isEmpty()) {
                holder.disapprovalReason.setText("Reason: " + request.getDisapprovalReason());
                holder.disapprovalReason.setVisibility(View.VISIBLE);
            }
        } else { // "pending"
            holder.status.setChipBackgroundColorResource(android.R.color.holo_orange_light);
            if (isAdmin) {
                holder.actionButtonsLayout.setVisibility(View.VISIBLE);
            } else {
                holder.deleteButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() { return requestList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName, dateRange, reason, disapprovalReason;
        Chip status;
        Button btnApprove, btnDisapprove;
        ImageButton deleteButton;
        LinearLayout actionButtonsLayout;

        public ViewHolder(@NonNull View itemView, OnLeaveRequestListener listener) {
            super(itemView);
            userName = itemView.findViewById(R.id.textViewUserName);
            dateRange = itemView.findViewById(R.id.textViewDateRange);
            reason = itemView.findViewById(R.id.textViewReason);
            status = itemView.findViewById(R.id.chipStatus);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDisapprove = itemView.findViewById(R.id.btnDisapprove);
            actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
            disapprovalReason = itemView.findViewById(R.id.textViewDisapprovalReason);
            deleteButton = itemView.findViewById(R.id.btn_delete_request);

            btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) listener.onApproveClick(position);
                }
            });

            btnDisapprove.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) listener.onDisapproveClick(position);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) listener.onDeleteClick(position);
                }
            });
        }
    }
}