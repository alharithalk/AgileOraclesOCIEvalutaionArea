package com.agileoracles.leave_portal_app.model;

import java.time.Instant;

public record LeaveResponse(
        String authenticatedUser,
        String fileName,
        LeaveCategory category,
        String matchedKeywords,
        Instant uploadTimestamp,
        String ociObjectName,
        String ociObjectId) {
}
