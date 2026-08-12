package com.agileoracles.leave_portal_app.model;


import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "LEAVE_UPLOAD")
public class LeaveUploadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String attachedFilename;
    private String reasonForLeave;

    @Convert(converter = LeaveCategoryConverter.class)
    private LeaveCategory leaveCategory;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant createdAt;
    private String ociObjectName;
    private String ociObjectId;
    private String ociBucketName;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getAttachedFilename() { return attachedFilename; }
    public void setAttachedFilename(String attachedFilename) { this.attachedFilename = attachedFilename; }
    public String getReasonForLeave() { return reasonForLeave; }
    public void setReasonForLeave(String reasonForLeave) { this.reasonForLeave = reasonForLeave; }
    public LeaveCategory getLeaveCategory() { return leaveCategory; }
    public void setLeaveCategory(LeaveCategory leaveCategory) { this.leaveCategory = leaveCategory; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getOciObjectName() { return ociObjectName; }
    public void setOciObjectName(String ociObjectName) { this.ociObjectName = ociObjectName; }
    public String getOciObjectId() { return ociObjectId; }
    public void setOciObjectId(String ociObjectId) { this.ociObjectId = ociObjectId; }
    public String getOciBucketName() { return ociBucketName; }
    public void setOciBucketName(String ociBucketName) { this.ociBucketName = ociBucketName; }
}