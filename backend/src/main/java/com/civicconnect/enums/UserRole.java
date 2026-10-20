package com.civicconnect.enums;

/**
 * Roles assigned to users in CivicConnect.
 *
 * <ul>
 *   <li>{@link #CITIZEN} — Any registered member of the public who can submit
 *       and track their own complaints.</li>
 *   <li>{@link #OFFICER} — Municipal staff member who can view all complaints,
 *       update statuses, and add notes. Belongs to one department.</li>
 *   <li>{@link #DEPARTMENT_HEAD} — Senior staff member who can assign complaints
 *       to individual officers within their department.</li>
 *   <li>{@link #ADMIN} — System administrator with full access including user
 *       management, role changes, and department configuration.</li>
 * </ul>
 *
 * <p>Stored as a VARCHAR in the database via {@code @Enumerated(EnumType.STRING)}.
 */
public enum UserRole {

    CITIZEN,
    OFFICER,
    DEPARTMENT_HEAD,
    ADMIN
}
