package com.agileoracles.leave_portal_app.service;

import com.agileoracles.leave_portal_app.model.LeaveCategory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategorizationService {

    private static final Map<LeaveCategory, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(LeaveCategory.SICK_LEAVE, List.of("sick", "fever", "medical", "ill", "doctor", "hospital"));
        KEYWORDS.put(LeaveCategory.ANNUAL_LEAVE, List.of("annual", "vacation", "holiday", "leave", "trip"));
        KEYWORDS.put(LeaveCategory.EMERGENCY_LEAVE, List.of("emergency", "urgent", "accident", "crisis"));
        KEYWORDS.put(LeaveCategory.MATERNITY_LEAVE, List.of("maternity", "pregnancy", "baby", "birth", "newborn"));
        KEYWORDS.put(LeaveCategory.UNPAID_LEAVE, List.of("unpaid", "without pay", "no pay"));
    }

    public LeaveCategory categorize(String content) {
        String lowerContent = content.toLowerCase();
        for (Map.Entry<LeaveCategory, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerContent.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return LeaveCategory.OTHER;
    }

    public String findMatchedKeywords(String content) {
        String lowerContent = content.toLowerCase();
        return KEYWORDS.values().stream()
                .flatMap(List::stream)
                .filter(lowerContent::contains)
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
