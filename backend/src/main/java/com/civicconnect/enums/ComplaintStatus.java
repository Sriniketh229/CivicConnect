package com.civicconnect.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle statuses for a complaint in CivicConnect.
 *
 * <p>Valid transition graph:
 * <pre>
 *   SUBMITTED   ──> UNDER_REVIEW  ──> IN_PROGRESS ──> RESOLVED
 *       │               │                 │               │
 *       └──> REJECTED <─┘                 └──> REJECTED   └──> REOPENED
 *                │                                             │
 *                └──────────────> REOPENED ──> UNDER_REVIEW ──┘
 * </pre>
 *
 * <p>Transitions are enforced at the service layer by calling
 * {@link #canTransitionTo(ComplaintStatus)}. Any attempt to move to an
 * invalid status returns {@code false} and the service should reject it
 * with a 400 Bad Request.
 *
 * <p>The four statuses visible in the current frontend UI
 * (Submitted, In Progress, Resolved, Rejected) map directly to
 * SUBMITTED, IN_PROGRESS, RESOLVED and REJECTED. UNDER_REVIEW and
 * REOPENED are backend-only statuses that the frontend will surface
 * when the UI is extended in Phase 5.
 */
public enum ComplaintStatus {

    /** Complaint has been submitted but not yet reviewed by staff. */
    SUBMITTED,

    /** A staff member has opened the complaint for review. */
    UNDER_REVIEW,

    /** Work is actively in progress by the assigned department. */
    IN_PROGRESS,

    /** The issue has been resolved and verified. */
    RESOLVED,

    /** The complaint was rejected (e.g. outside jurisdiction). */
    REJECTED,

    /** Citizen has reopened a previously resolved or rejected complaint. */
    REOPENED;

    // ── Transition rules ────────────────────────────────────────────────────

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(ComplaintStatus.class);
        VALID_TRANSITIONS.put(SUBMITTED,     EnumSet.of(UNDER_REVIEW, REJECTED));
        VALID_TRANSITIONS.put(UNDER_REVIEW,  EnumSet.of(IN_PROGRESS, REJECTED));
        VALID_TRANSITIONS.put(IN_PROGRESS,   EnumSet.of(RESOLVED, REJECTED));
        VALID_TRANSITIONS.put(RESOLVED,      EnumSet.of(REOPENED));
        VALID_TRANSITIONS.put(REJECTED,      EnumSet.of(REOPENED));
        VALID_TRANSITIONS.put(REOPENED,      EnumSet.of(UNDER_REVIEW));
    }

    /**
     * Returns {@code true} if transitioning from this status to
     * {@code next} is permitted by the complaint lifecycle rules.
     *
     * @param next the target status
     * @return {@code true} if the transition is valid
     */
    public boolean canTransitionTo(ComplaintStatus next) {
        Set<ComplaintStatus> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(next);
    }

    /**
     * Returns the set of statuses this status can legally transition to.
     *
     * @return immutable view of valid next statuses
     */
    public Set<ComplaintStatus> allowedTransitions() {
        return VALID_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(ComplaintStatus.class));
    }
}
