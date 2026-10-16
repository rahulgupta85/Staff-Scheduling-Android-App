package com.example.edutech;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Shift {

    // =====================
    // BASIC FIELDS
    // =====================
    private String userId;
    private String userName;
    private String shiftDetails;
    private Timestamp startTime;
    private Timestamp endTime;

    // =====================
    // STATUS
    // =====================
    private String status = "scheduled"; // scheduled / completed / unavailable
    private String unavailableReason;

    // =====================
    // 🔥 NEW FIELD (PROXY SUPPORT)
    // =====================
    private boolean isProxy = false;

    // Firestore document ID (local use only)
    @Exclude
    private String documentId;

    // Required empty constructor
    public Shift() {}

    // =====================
    // GETTERS
    // =====================
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getShiftDetails() { return shiftDetails; }
    public Timestamp getStartTime() { return startTime; }
    public Timestamp getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getUnavailableReason() { return unavailableReason; }
    public boolean isProxy() { return isProxy; }
    public String getDocumentId() { return documentId; }

    // =====================
    // SETTERS
    // =====================
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setShiftDetails(String shiftDetails) { this.shiftDetails = shiftDetails; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    public void setStatus(String status) { this.status = status; }
    public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
    public void setProxy(boolean proxy) { isProxy = proxy; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}