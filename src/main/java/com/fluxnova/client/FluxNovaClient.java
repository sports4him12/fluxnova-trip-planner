package com.fluxnova.client;

import com.fluxnova.client.dto.CamundaVariable;
import com.fluxnova.client.dto.ProcessInstanceRequest;
import com.fluxnova.client.dto.ProcessInstanceResponse;
import com.fluxnova.client.dto.TaskResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thin wrapper around the FluxNova engine-rest API (Camunda REST API compatible).
 * Base URL: http://<host>/engine-rest  (configured via fluxnova.api.base-url)
 */
@Component
public class FluxNovaClient {

    private final WebClient webClient;

    public FluxNovaClient(WebClient fluxNovaWebClient) {
        this.webClient = fluxNovaWebClient;
    }

    /**
     * Start a new process instance.
     * POST /process-definition/key/{key}/start
     */
    public ProcessInstanceResponse startProcess(String processDefinitionKey, Map<String, Object> variables) {
        ProcessInstanceRequest request = ProcessInstanceRequest.builder()
                .variables(toCamundaVariables(variables))
                .build();

        return webClient.post()
                .uri("/process-definition/key/{key}/start", processDefinitionKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProcessInstanceResponse.class)
                .block();
    }

    /**
     * Retrieve all active user tasks for a process instance.
     * GET /task?processInstanceId={id}
     */
    public List<TaskResponse> getTasksForInstance(String processInstanceId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/task")
                        .queryParam("processInstanceId", processInstanceId)
                        .build())
                .retrieve()
                .bodyToFlux(TaskResponse.class)
                .collectList()
                .block();
    }

    /**
     * Complete a user task, advancing the workflow.
     * POST /task/{id}/complete
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        Map<String, Object> body = Map.of("variables", toCamundaVariables(variables));

        webClient.post()
                .uri("/task/{id}/complete", taskId)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    /**
     * Fetch a process instance by ID.
     * GET /process-instance/{id}
     */
    public ProcessInstanceResponse getProcessInstance(String processInstanceId) {
        return webClient.get()
                .uri("/process-instance/{id}", processInstanceId)
                .retrieve()
                .bodyToMono(ProcessInstanceResponse.class)
                .block();
    }

    /**
     * Delete (cancel) a running process instance.
     * DELETE /process-instance/{id}
     */
    public void cancelProcessInstance(String processInstanceId) {
        webClient.delete()
                .uri("/process-instance/{id}", processInstanceId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private Map<String, CamundaVariable> toCamundaVariables(Map<String, Object> raw) {
        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> CamundaVariable.of(e.getValue())
                ));
    }
}
