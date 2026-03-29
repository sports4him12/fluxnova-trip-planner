# FluxNova Trip Planner

Spring Boot service that manages family trip planning and orchestrates planning workflows via the FluxNova/Camunda 7 BPM engine.

## Overview

The trip planner stores destinations, travel windows (available days per season), and trips in MySQL. Each trip can be linked to a FluxNova BPMN workflow that guides the family through the planning steps: destination review, date setting, and final approval.

A LangChain4J AI assistant is also available at `/api/ai/chat` to suggest destinations by season and query active workflow tasks conversationally.

## Architecture

```
Custom Web Frontend
        │
        ▼
FluxNova Trip Planner (this service) :8080
        │                    │
        ▼                    ▼
   MySQL / Aurora    FluxNova Engine :8090
   (fluxnova_trips)  (engine-rest API)
```

## Tech Stack

- Java 21, Spring Boot 3.4.4, Maven 3.9.14
- Spring Data JPA + Liquibase (schema migrations)
- MySQL / Aurora MySQL Serverless v2
- WebClient → FluxNova engine-rest (Camunda 7 compatible)
- LangChain4J 0.31.0 (OpenAI)

## Project Structure

```
src/main/java/com/fluxnova/
├── ai/           LangChain4J assistant + workflow agent tools
├── client/       FluxNova engine-rest WebClient + DTOs
├── config/       WebClient config, CORS
├── controller/   REST endpoints (/api/trips, /api/ai)
├── model/        Trip, Destination, TravelWindow, Season, TripStatus
├── repository/   Spring Data JPA repositories
└── service/      TripService, WorkflowService
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/trips` | List all trips (optional `?season=SUMMER`) |
| POST | `/api/trips` | Create a trip |
| PUT | `/api/trips/{id}` | Update a trip |
| DELETE | `/api/trips/{id}` | Delete a trip |
| GET | `/api/trips/destinations` | List destinations (optional `?season=`) |
| POST | `/api/trips/destinations` | Add a destination |
| GET | `/api/trips/travel-windows` | List travel windows |
| POST | `/api/trips/travel-windows` | Add a travel window |
| POST | `/api/trips/{id}/workflow/start` | Start FluxNova workflow for a trip |
| GET | `/api/trips/{id}/workflow/status` | Get workflow instance state |
| GET | `/api/trips/{id}/workflow/tasks` | List active user tasks |
| POST | `/api/trips/{id}/workflow/tasks/{taskId}/complete` | Complete a task |
| DELETE | `/api/trips/{id}/workflow` | Cancel workflow |
| POST | `/api/ai/chat` | Chat with AI trip planning assistant |

## Local Development

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL running at `localhost:3306`
- FluxNova engine server running at `localhost:8090` (see [fluxnova-server](https://github.com/sports4him12/fluxnova-server))

### Setup

1. Create the database:
   ```bash
   mysql -u root -p -e "CREATE DATABASE fluxnova_trips CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   ```

2. Update credentials in `src/main/resources/application.properties` if needed:
   ```properties
   spring.datasource.password=yourpassword
   ```

3. Set your OpenAI key (optional — AI chat endpoint only):
   ```bash
   export OPENAI_API_KEY=sk-...
   ```

4. Build and run:
   ```bash
   mvn clean install
   java -jar target/fluxnova-ai-0.0.1-SNAPSHOT.jar
   ```

Liquibase runs automatically on startup and creates the `destinations`, `travel_windows`, and `trips` tables.

### Quick Test

```bash
# Add a destination
curl -s -X POST http://localhost:8080/api/trips/destinations \
  -H "Content-Type: application/json" \
  -d '{"name":"Yellowstone","country":"USA","region":"Wyoming","bestSeasons":["SUMMER","FALL"],"tags":"hiking,wildlife,geysers"}' | jq .

# Create a trip
curl -s -X POST "http://localhost:8080/api/trips?destinationId=1" \
  -H "Content-Type: application/json" \
  -d '{"title":"Summer Yellowstone Trip","season":"SUMMER","status":"DRAFT"}' | jq .

# Start the planning workflow
curl -s -X POST http://localhost:8080/api/trips/1/workflow/start | jq .
```

## AWS Deployment

Infrastructure is managed by the [fluxnova-cdk](https://github.com/sports4him12/fluxnova-cdk) project, which deploys this service as an ECS Fargate task behind an internet-facing Application Load Balancer.

Database credentials and the FluxNova engine URL are injected at runtime via environment variables — no changes to `application.properties` are needed for production.
