package com.erp.mock.model;

public enum ErpSimulationScenario {
    SUCCESS,
    LATENCY_5S,
    ERROR_500,
    BUSINESS_REJECT,
    TRANSIENT_FAIL_THEN_SUCCESS,
    INVALID_PAYLOAD
}
