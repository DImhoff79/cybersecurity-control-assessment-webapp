package com.cyberassessment.model;

/**
 * Who is expected to answer controls in owner-facing questionnaires, derived from
 * aggregated {@code questions.ask_owner} (same axis as Question Manager "Ask owners").
 */
public enum ControlResponderAudience {
    /** All linked questions are asked to application owners. */
    APPLICATION_OWNER,
    /** All linked questions are not asked to owners (security / compliance / GRC). */
    SECURITY_COMPLIANCE,
    /** Some questions for owners, some for security/compliance. */
    MIXED;

    public static ControlResponderAudience fromAskOwnerCounts(long askOwnerTrueCount, long totalQuestions) {
        if (totalQuestions <= 0) {
            return APPLICATION_OWNER;
        }
        if (askOwnerTrueCount == totalQuestions) {
            return APPLICATION_OWNER;
        }
        if (askOwnerTrueCount == 0) {
            return SECURITY_COMPLIANCE;
        }
        return MIXED;
    }
}
