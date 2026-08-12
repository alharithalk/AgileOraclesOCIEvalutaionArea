package com.agileoracles.leave_portal_app.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LeaveCategoryConverter implements AttributeConverter<LeaveCategory, String> {

    @Override
    public String convertToDatabaseColumn(LeaveCategory category) {
        return category == null ? null : category.name();
    }

    @Override
    public LeaveCategory convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = normalize(value);
        for (LeaveCategory category : LeaveCategory.values()) {
            if (normalize(category.name()).equals(normalized)) {
                return category;
            }
        }
        return LeaveCategory.OTHER;
    }

    private String normalize(String value) {
        return value.trim().toUpperCase().replaceAll("[\\s_-]+", "");
    }
}
