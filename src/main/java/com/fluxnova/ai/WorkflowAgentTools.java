package com.fluxnova.ai;

import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.model.Destination;
import com.fluxnova.model.Season;
import com.fluxnova.model.Trip;
import com.fluxnova.service.TripService;
import com.fluxnova.service.WorkflowService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LangChain4J tools that let the AI agent interact with the trip data and FluxNova workflows.
 */
@Component
@RequiredArgsConstructor
public class WorkflowAgentTools {

    private final TripService tripService;
    private final WorkflowService workflowService;

    @Tool("List all planned trips, optionally filtered by season (SPRING, SUMMER, FALL, WINTER)")
    public String listTrips(String season) {
        List<Trip> trips = season != null && !season.isBlank()
                ? tripService.getTripsBySeason(Season.valueOf(season.toUpperCase()))
                : tripService.getAllTrips();

        if (trips.isEmpty()) return "No trips found.";

        return trips.stream()
                .map(t -> String.format("Trip[id=%d, title=%s, season=%s, status=%s, destination=%s]",
                        t.getId(), t.getTitle(), t.getSeason(),
                        t.getStatus(),
                        t.getDestination() != null ? t.getDestination().getName() : "TBD"))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Get destinations that are best visited in a given season (SPRING, SUMMER, FALL, WINTER)")
    public String getDestinationsForSeason(String season) {
        List<Destination> destinations = tripService.getDestinationsBySeason(Season.valueOf(season.toUpperCase()));
        if (destinations.isEmpty()) return "No destinations found for season: " + season;

        return destinations.stream()
                .map(d -> String.format("Destination[id=%d, name=%s, region=%s, tags=%s]",
                        d.getId(), d.getName(), d.getRegion(), d.getTags()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Get the active workflow tasks for a trip by its ID")
    public String getWorkflowTasks(Long tripId) {
        List<TaskResponse> tasks = workflowService.getTasksForTrip(tripId);
        if (tasks.isEmpty()) return "No active tasks for trip " + tripId + ". Workflow may not be started yet.";

        return tasks.stream()
                .map(t -> String.format("Task[id=%s, name=%s, key=%s]",
                        t.getId(), t.getName(), t.getTaskDefinitionKey()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Start the FluxNova planning workflow for a trip by its ID")
    public String startWorkflow(Long tripId) {
        var instance = workflowService.startTripWorkflow(tripId);
        return String.format("Workflow started. Instance ID: %s, State: %s", instance.getId(), instance.getState());
    }

    @Tool("Get the current workflow status for a trip by its ID")
    public String getWorkflowStatus(Long tripId) {
        var instance = workflowService.getWorkflowStatus(tripId);
        return String.format("Workflow[instanceId=%s, processKey=%s, state=%s]",
                instance.getId(), instance.getProcessDefinitionKey(), instance.getState());
    }
}
