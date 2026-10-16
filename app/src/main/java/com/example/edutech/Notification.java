package com.example.edutech;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Notification {
    private String title;
    private String message;
    private Timestamp timestamp;
    private boolean isRead;

    public Notification() {} // Needed for Firestore

    // Getters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }

    public String getFormattedDate() {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
}