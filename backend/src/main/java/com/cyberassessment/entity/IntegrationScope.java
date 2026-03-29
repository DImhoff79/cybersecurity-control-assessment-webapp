package com.cyberassessment.entity;

/**
 * How the application connects to other systems (owner triage).
 */
public enum IntegrationScope {
    /** Few or no integrations; mostly self-contained */
    STANDALONE,
    /** Integrates with other in-scope enterprise systems */
    KROGER_INTEGRATED,
    /** Sends/receives data with external organizations */
    EXTERNAL_EXCHANGE,
    /** Both enterprise and external integrations */
    BOTH
}
