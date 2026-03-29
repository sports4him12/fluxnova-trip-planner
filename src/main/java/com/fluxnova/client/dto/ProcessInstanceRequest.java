package com.fluxnova.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Request body for POST /engine-rest/process-definition/key/{key}/start
 */
@Getter
@Builder
public class ProcessInstanceRequest {
    private Map<String, CamundaVariable> variables;
}
