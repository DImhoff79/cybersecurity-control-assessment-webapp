package com.cyberassessment.entity;

/**
 * Who uses the application (owner triage).
 */
public enum UserAudienceScope {
    /** Associates and similar workforce identities only */
    WORKFORCE_ONLY,
    /** Workforce plus contractors / contingent workers */
    WORKFORCE_AND_CONTRACTORS,
    /** Customers, loyalty members, or the general public */
    CUSTOMER_OR_PUBLIC,
    /** External business partners (B2B) */
    BUSINESS_PARTNERS,
    /** Multiple distinct audiences */
    MIXED
}
