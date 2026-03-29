package com.fluxnova.client.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceResponseTest {

    @Test
    void getState_active_whenNeitherEndedNorSuspended() {
        ProcessInstanceResponse r = new ProcessInstanceResponse();
        assertThat(r.getState()).isEqualTo("ACTIVE");
    }

    @Test
    void getState_suspended_whenSuspendedTrue() {
        ProcessInstanceResponse r = new ProcessInstanceResponse();
        r.setSuspended(true);
        assertThat(r.getState()).isEqualTo("SUSPENDED");
    }

    @Test
    void getState_ended_whenEndedTrue() {
        ProcessInstanceResponse r = new ProcessInstanceResponse();
        r.setEnded(true);
        assertThat(r.getState()).isEqualTo("ENDED");
    }

    @Test
    void getState_ended_takesPriorityOverSuspended() {
        ProcessInstanceResponse r = new ProcessInstanceResponse();
        r.setEnded(true);
        r.setSuspended(true);
        assertThat(r.getState()).isEqualTo("ENDED");
    }

    @Test
    void getProcessDefinitionKey_extractsKeyFromDefinitionId() {
        ProcessInstanceResponse r = new ProcessInstanceResponse();
        r.setProcessDefinitionId("trip-planning-process:1:abc123");
        assertThat(r.getProcessDefinitionKey()).isEqualTo("trip-planning-process");
    }

    @Test
    void getProcessDefinitionKey_returnsNull_whenDefinitionIdIsNull() {
        ProcessInstanceResponse r = new ProcessInstanceResponse();
        assertThat(r.getProcessDefinitionKey()).isNull();
    }
}
