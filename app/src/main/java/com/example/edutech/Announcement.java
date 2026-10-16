package com.example.edutech;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude; // <-- IMPORTANT IMPORT
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Announcement {
    private String title;
    private String message;
    private String authorName;
    private Timestamp timestamp;
    private Timestamp expiryTimestamp; // Keep this for the teacher's view

    // This field will hold the document's ID from Firestore
    @Exclude // This tells Firestore not to save this field back to the database
    private String documentId;

    public Announcement() {} // Needed for Firestore

    // Getters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getAuthorName() { return authorName; }
    public Timestamp getTimestamp() { return timestamp; }
    public Timestamp getExpiryTimestamp() { return expiryTimestamp; }
    public String getDocumentId() { return documentId; }

    // Setter for the document ID
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFormattedDate() {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
}