package com.agileoracles.leave_portal_app.model;

import java.time.Instant;

public record LeaveRecordSummary(
        Long id,
        String fileName,
        LeaveCategory category,
        Instant createdAt,
        String objectName) {
}
