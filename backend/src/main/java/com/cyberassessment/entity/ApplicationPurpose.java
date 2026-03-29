package com.cyberassessment.entity;

/**
 * What the application mainly serves (owner triage).
 */
public enum ApplicationPurpose {
    /** Internal operations, back-office, no direct customer channel */
    INTERNAL_OPERATIONAL,
    /** Store, web, app, or other shopper/customer-facing experiences */
    CUSTOMER_FACING,
    /** B2B partners, suppliers, marketplace counterparties */
    PARTNER_FACING,
    /** More than one of the above */
    MIXED
}
