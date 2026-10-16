package com.example.edutech.admin.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.edutech.R;

import java.util.Calendar;

public class AdminDateFilterDialog extends DialogFragment {

    public interface OnApplyListener {
        void onApply(
                int fy, int fm, int fd,
                int ty, int tm, int td
        );
    }

    private OnApplyListener listener;

    private TextView txtFrom, txtTo;
    private int fy, fm, fd, ty, tm, td;

    public AdminDateFilterDialog(OnApplyListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_admin_date_filter, null);

        txtFrom = view.findViewById(R.id.txtFromDate);
        txtTo = view.findViewById(R.id.txtToDate);
        Button btnApply = view.findViewById(R.id.btnApply);

        Calendar cal = Calendar.getInstance();

        fy = ty = cal.get(Calendar.YEAR);
        fm = tm = cal.get(Calendar.MONTH);
        fd = td = cal.get(Calendar.DAY_OF_MONTH);

        updateText();

        txtFrom.setOnClickListener(v -> openPicker(true));
        txtTo.setOnClickListener(v -> openPicker(false));

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApply(fy, fm, fd, ty, tm, td);
            }
            dismiss();
        });

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    private void openPicker(boolean isFrom) {

        int y = isFrom ? fy : ty;
        int m = isFrom ? fm : tm;
        int d = isFrom ? fd : td;

        new DatePickerDialog(
                requireContext(),
                (v, year, month, day) -> {
                    if (isFrom) {
                        fy = year; fm = month; fd = day;
                    } else {
                        ty = year; tm = month; td = day;
                    }
                    updateText();
                },
                y, m, d
        ).show();
    }

    private void updateText() {
        txtFrom.setText(fd + "/" + (fm + 1) + "/" + fy);
        txtTo.setText(td + "/" + (tm + 1) + "/" + ty);
    }
}
