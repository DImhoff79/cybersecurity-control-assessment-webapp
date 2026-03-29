package com.cyberassessment.entity;

/**
 * Where the application runs (owner triage).
 */
public enum HostingModel {
    /** Data center or cloud estate operated under enterprise standards */
    KROGER_MANAGED,
    /** Vendor SaaS or vendor-hosted (e.g. major SaaS platforms) */
    VENDOR_SAAS,
    /** Split across enterprise and vendor */
    HYBRID
}
