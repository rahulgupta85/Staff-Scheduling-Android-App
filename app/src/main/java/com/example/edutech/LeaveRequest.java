package com.example.edutech;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LeaveRequest {
    private String userId; // <-- THIS FIELD WAS MISSING
    private String userName;
    private String reason;
    private String status;
    private String disapprovalReason;
    private String assignedAdminId;
    private String assignedAdminName;
    private Timestamp startDate;
    private Timestamp endDate;
    private Timestamp requestTimestamp;

    @Exclude
    private String documentId;

    public LeaveRequest() {} // Required for Firestore

    // Getters
    public String getUserId() { return userId; } // <-- THIS GETTER WAS MISSING
    public String getUserName() { return userName; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getDisapprovalReason() { return disapprovalReason; }
    public String getAssignedAdminId() { return assignedAdminId; }
    public String getAssignedAdminName() { return assignedAdminName; }
    public Timestamp getStartDate() { return startDate; }
    public Timestamp getEndDate() { return endDate; }
    public Timestamp getRequestTimestamp() { return requestTimestamp; }
    public String getDocumentId() { return documentId; }

    // Setter
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFormattedDateRange() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        if (startDate != null && endDate != null) {
            return sdf.format(startDate.toDate()) + " - " + sdf.format(endDate.toDate());
        }
        return "N/A";
    }
}
