package com.fluxnova.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Response from GET /engine-rest/task
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse {
    private String id;
    private String name;

    @JsonProperty("taskDefinitionKey")
    private String taskDefinitionKey;

    @JsonProperty("processInstanceId")
    private String processInstanceId;

    @JsonProperty("processDefinitionId")
    private String processDefinitionId;

    private String assignee;
    private String created;
    private boolean suspended;

    public String getState() {
        return suspended ? "SUSPENDED" : "ACTIVE";
    }
}
