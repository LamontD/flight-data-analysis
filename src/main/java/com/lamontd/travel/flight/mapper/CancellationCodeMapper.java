package com.lamontd.travel.flight.mapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps cancellation codes to their descriptions
 */
public class CancellationCodeMapper {
    private static final Map<String, String> CANCELLATION_REASONS = new HashMap<>();
    private static CancellationCodeMapper defaultInstance;

    static {
        CANCELLATION_REASONS.put("A", "Carrier Caused");
        CANCELLATION_REASONS.put("B", "Weather");
        CANCELLATION_REASONS.put("C", "National Aviation System");
        CANCELLATION_REASONS.put("D", "Security");
    }

    private CancellationCodeMapper() {
        // Private constructor for singleton
    }

    /**
     * Gets the default singleton instance
     */
    public static synchronized CancellationCodeMapper getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new CancellationCodeMapper();
        }
        return defaultInstance;
    }

    /**
     * Gets the description for a cancellation code
     *
     * @param code The cancellation code (A, B, C, or D)
     * @return The description, or the code itself if not found
     */
    public String getDescription(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "Unknown";
        }
        return CANCELLATION_REASONS.getOrDefault(code.trim().toUpperCase(), code);
    }

    /**
     * Gets the full description with the code
     *
     * @param code The cancellation code
     * @return Formatted string like "A - Carrier Caused"
     */
    public String getFullDescription(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "Unknown";
        }
        String cleanCode = code.trim().toUpperCase();
        String description = CANCELLATION_REASONS.get(cleanCode);
        if (description != null) {
            return cleanCode + " - " + description;
        }
        return cleanCode;
    }

    /**
     * Checks if a code is valid
     */
    public boolean isValidCode(String code) {
        return code != null && CANCELLATION_REASONS.containsKey(code.trim().toUpperCase());
    }

    /**
     * Gets all valid cancellation codes
     */
    public Map<String, String> getAllCodes() {
        return new HashMap<>(CANCELLATION_REASONS);
    }
}
