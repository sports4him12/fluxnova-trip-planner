package com.fluxnova.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Response from GET /engine-rest/process-instance/{id}
 * and POST /engine-rest/process-definition/key/{key}/start
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessInstanceResponse {
    private String id;

    @JsonProperty("definitionId")
    private String processDefinitionId;

    @JsonProperty("businessKey")
    private String businessKey;

    private boolean suspended;
    private boolean ended;

    /** Derived: "ACTIVE", "SUSPENDED", or "ENDED" */
    public String getState() {
        if (ended)     return "ENDED";
        if (suspended) return "SUSPENDED";
        return "ACTIVE";
    }

    public String getProcessDefinitionKey() {
        // definitionId format: "key:version:id" — extract the key portion
        return processDefinitionId != null ? processDefinitionId.split(":")[0] : null;
    }
}
