package com.example.edutech.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edutech.R;
import com.example.edutech.Shift;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ViewHolder> {

    private final List<Shift> shiftList;
    private final boolean showAdminActions;
    private final OnShiftListener listener;

    // ====================
    // LISTENER
    // ====================

    public interface OnShiftListener {
        void onDeleteClick(int position);
        void onReassignClick(int position);
        void onMarkCompleted(int position);
        void onMarkUnavailable(int position);
    }

    // ====================
    // CONSTRUCTOR
    // ====================

    public ShiftAdapter(List<Shift> shiftList,
                        boolean showAdminActions,
                        OnShiftListener listener) {
        this.shiftList = shiftList;
        this.showAdminActions = showAdminActions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_shift, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {

        Shift shift = shiftList.get(position);

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE, MMM d, yyyy",
                        Locale.getDefault());

        SimpleDateFormat timeFormat =
                new SimpleDateFormat("hh:mm a",
                        Locale.getDefault());

        holder.shiftDate.setText(
                dateFormat.format(shift.getStartTime().toDate())
        );

        holder.userName.setText(shift.getUserName());
        holder.shiftDetails.setText(shift.getShiftDetails());

        holder.shiftTime.setText(
                timeFormat.format(shift.getStartTime().toDate())
                        + " - " +
                        timeFormat.format(shift.getEndTime().toDate())
        );

        // ====================
        // STATUS + PROXY
        // ====================

        String status = shift.getStatus() != null
                ? shift.getStatus()
                : "scheduled";

        String displayStatus = status.toUpperCase();

// 🔥 PROXY DISPLAY FIX
        if (shift.isProxy()) {
            displayStatus = "PROXY • " + displayStatus;
        }

        holder.statusText.setText(displayStatus);

        switch (status) {
            case "completed":
                holder.statusText.setTextColor(
                        Color.parseColor("#2E7D32"));
                break;

            case "unavailable":
                holder.statusText.setTextColor(
                        Color.parseColor("#C62828"));
                break;

            default:
                holder.statusText.setTextColor(
                        Color.parseColor("#1565C0"));
        }

        // ====================
        // READ ONLY MODE
        // ====================

        if (listener == null) {
            hideAllActions(holder);
            return;
        }

        // ====================
        // ADMIN VIEW
        // ====================

        if (showAdminActions) {

            holder.actionButtons.setVisibility(View.GONE);

            // DELETE
            if ("scheduled".equals(status)) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(
                        v -> listener.onDeleteClick(position)
                );
            } else {
                holder.deleteButton.setVisibility(View.GONE);
            }

            // REASSIGN
            if ("unavailable".equals(status) &&
                    shift.getUnavailableReason() != null &&
                    !shift.getUnavailableReason().isEmpty()) {

                holder.unavailableReason.setVisibility(View.VISIBLE);

                holder.unavailableReason.setText(
                        "Reason: "
                                + shift.getUnavailableReason()
                                + "\n(Tap to reassign)"
                );

                holder.unavailableReason.setOnClickListener(
                        v -> listener.onReassignClick(position)
                );

            } else {
                holder.unavailableReason.setVisibility(View.GONE);
            }

            return;
        }

        // ====================
        // TEACHER VIEW
        // ====================

        holder.deleteButton.setVisibility(View.GONE);
        holder.unavailableReason.setVisibility(View.GONE);

        if ("scheduled".equals(status)) {

            holder.actionButtons.setVisibility(View.VISIBLE);

            holder.btnCompleted.setOnClickListener(
                    v -> listener.onMarkCompleted(position)
            );

            holder.btnUnavailable.setOnClickListener(
                    v -> listener.onMarkUnavailable(position)
            );

        } else {
            holder.actionButtons.setVisibility(View.GONE);
        }
    }

    private void hideAllActions(ViewHolder holder) {
        holder.deleteButton.setVisibility(View.GONE);
        holder.unavailableReason.setVisibility(View.GONE);
        holder.actionButtons.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    // ====================
    // VIEW HOLDER
    // ====================

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView shiftDate, userName, shiftTime, shiftDetails;
        TextView statusText, unavailableReason;
        ImageButton deleteButton;
        LinearLayout actionButtons;
        MaterialButton btnCompleted, btnUnavailable;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            shiftDate = itemView.findViewById(R.id.textViewShiftDate);
            userName = itemView.findViewById(R.id.textViewUserName);
            shiftTime = itemView.findViewById(R.id.textViewShiftTime);
            shiftDetails = itemView.findViewById(R.id.textViewShiftDetails);

            statusText = itemView.findViewById(R.id.textStatus);
            unavailableReason =
                    itemView.findViewById(R.id.textUnavailableReason);

            deleteButton =
                    itemView.findViewById(R.id.btn_delete_shift);

            actionButtons =
                    itemView.findViewById(R.id.layoutActionButtons);

            btnCompleted =
                    itemView.findViewById(R.id.btnMarkCompleted);

            btnUnavailable =
                    itemView.findViewById(R.id.btnMarkUnavailable);
        }
    }
}