package com.civicconnect.enums;

/**
 * Complaint categories available to citizens when reporting an issue.
 *
 * <p>Each constant maps exactly to the category names displayed in the
 * existing React frontend ({@code src/App.tsx} categories array), ensuring
 * that values submitted from the UI can be deserialised without transformation.
 *
 * <p>Stored as a VARCHAR in the database via {@code @Enumerated(EnumType.STRING)}.
 * The {@link #getDisplayName()} method returns the human-readable label for
 * use in API responses and notifications.
 */
public enum ComplaintCategory {

    GARBAGE_COLLECTION("Garbage Collection"),
    ROADS_AND_POTHOLES("Roads & Potholes"),
    DRAINAGE_PROBLEMS("Drainage Problems"),
    STREETLIGHT_ISSUES("Streetlight Issues"),
    WATER_SUPPLY("Water Supply");

    private final String displayName;

    ComplaintCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable category label as shown in the frontend UI.
     *
     * @return display name string
     */
    public String getDisplayName() {
        return displayName;
    }
}
