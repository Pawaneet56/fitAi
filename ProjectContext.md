# FitAI Backend - Project Context

## Project Vision

FitAI is a production-grade backend application built to learn and demonstrate modern backend engineering practices.

The goal is **not just to build a workout tracker**, but to build software using the same architecture and patterns used in large technology companies.

The application will eventually support:

- Workout logging
- AI workout summaries
- AI exercise recommendations
- Progress analytics
- Achievements
- Notifications
- User profiles
- Historical insights

The project is intentionally built incrementally while following production design principles.

---

# Tech Stack

- Java 21
- Spring Boot 3.5.x
- Maven
- PostgreSQL
- Apache Kafka (KRaft Mode)
- Docker Compose
- Spring Data JPA
- Spring Validation
- Spring Actuator
- Lombok

Future additions:

- Spring Security + JWT
- Redis
- Prometheus
- Grafana
- OpenAPI / Swagger
- Testcontainers
- GitHub Actions
- Azure OpenAI / OpenAI
- Kubernetes

---

# Architecture

Current architecture:

REST API

↓

Controller

↓

Service

↓

Repository (Postgres)

↓

Kafka Producer

↓

Kafka Topic

↓

Kafka Consumer

↓

Business Service

The application is currently a **modular monolith**.

Kafka is used for asynchronous processing within the same application.

The current modules include workout functionality, Kafka integration, and analytics. The domain is modeled around a hierarchy:

```text
Workout
  └── WorkoutExercise
        └── WorkoutSet
```

A workout owns multiple exercises, and each exercise owns multiple sets.

---

# Current Package Structure

com.pawaneet.fitai

```text
analytics/
common/
config/
exception/

kafka/
    config/

workout/
    controller/
    dto/
    entity/
    mapper/
    producer/
    consumer/
    repository/
    service/
    event/
```

---

# Domain Model

## Workout

A workout contains:

- `id`
- `startedAt`
- `endedAt`
- `status`
- `notes`
- collection of `WorkoutExercise`

Workout-level `durationInSeconds` is **not stored as an entity field**. Duration is derived from `startedAt` and `endedAt` when required.

---

## WorkoutExercise

A `WorkoutExercise` belongs to one `Workout`.

Fields include:

- `id`
- `workout`
- `exerciseName`
- `orderIndex`
- collection of `WorkoutSet`

Database constraint:

- `(workout_id, order_index)` must be unique.

`orderIndex` is assigned by the backend automatically based on the current maximum order for that workout.

The client therefore does not need to provide `orderIndex` when adding an exercise.

---

## WorkoutSet

A `WorkoutSet` belongs to one `WorkoutExercise`.

Current set-level data includes:

- `setNumber`
- `weight`
- `reps`
- `rir`
- optional `durationSeconds`
- optional `notes`

`setNumber` is assigned automatically per exercise.

`durationSeconds` is intentionally optional. A more advanced optional time-tracking model may be introduced in the future if needed.

---

# Features Completed

## Workout

- Start Workout API
- End Workout API
- Get Workout API
- List Workouts API
- Add Workout Exercise API
- Add Workout Set API
- Workout entity
- WorkoutExercise entity
- WorkoutSet entity
- Workout repository
- WorkoutExercise repository
- WorkoutSet repository
- Workout mapper
- WorkoutExercise mapper
- WorkoutSet mapper
- Workout service
- WorkoutExercise service
- WorkoutSet service
- Workout controller
- WorkoutExercise controller
- WorkoutSet controller
- Workout duration calculation
- Automatic exercise ordering
- Automatic set numbering
- Validation for workout/exercise/set requests
- Prevent adding exercises/sets to completed workouts where applicable

---

## Kafka

Kafka Producers:

- `WorkoutEventProducer`
- `ExerciseEventProducer`

Kafka Consumers:

- `WorkoutStartedConsumer`
- `WorkoutEndedConsumer`
- `ExerciseAddedConsumer`

Kafka Topics:

- `workout-started`
- `workout-ended`
- `exercise-added`

Kafka configuration uses `NewTopic` beans instead of relying on broker auto-topic creation.

Kafka publishing is encapsulated inside producer classes. Business services should not interact directly with `KafkaTemplate`.

Kafka event payloads are represented by dedicated event types rather than exposing JPA entities directly as Kafka contracts.

Current event examples include:

- `WorkoutStartedEvent`
- `WorkoutEndedEvent`
- `ExerciseAddedEvent`

Kafka messages use the workout ID as the message key for workout/exercise events where appropriate.

---

# Event Flow

## Start Workout

```text
POST /api/workouts

↓

WorkoutController

↓

WorkoutService

↓

Save Workout

↓

WorkoutEventProducer

↓

Publish WorkoutStartedEvent

↓

Kafka topic: workout-started

↓

WorkoutStartedConsumer

↓

AnalyticsService
```

---

## End Workout

```text
PATCH /api/workouts/{workoutId}/end

↓

WorkoutController

↓

WorkoutService

↓

Load Workout

↓

Mark Workout COMPLETED

↓

Set endedAt

↓

Calculate duration

↓

WorkoutEventProducer

↓

Publish WorkoutEndedEvent

↓

Kafka topic: workout-ended

↓

WorkoutEndedConsumer

↓

AnalyticsService
```

---

## Add Exercise

```text
POST /api/workouts/{workoutId}/exercises

↓

WorkoutExerciseController

↓

WorkoutExerciseService

↓

Load Workout

↓

Validate workout state

↓

Determine next orderIndex

↓

Save WorkoutExercise

↓

ExerciseEventProducer

↓

Publish ExerciseAddedEvent

↓

Kafka topic: exercise-added

↓

ExerciseAddedConsumer

↓

AnalyticsService
```

---

## Add Set

```text
POST /api/workouts/{workoutId}/exercises/{exerciseId}/sets

↓

WorkoutSetController

↓

WorkoutSetService

↓

Load workout/exercise

↓

Validate workout state

↓

Determine next setNumber

↓

Save WorkoutSet

↓

Return WorkoutSetResponse
```

Set events can be introduced later when there is a clear consumer/use case for them.

---

# Analytics

`AnalyticsService` currently receives and logs:

- `WorkoutStartedEvent`
- `WorkoutEndedEvent`
- `ExerciseAddedEvent`

The current implementation is intentionally simple. Analytics will eventually become a real business capability rather than only logging consumed events.

---

# Current APIs

## Start Workout

POST

```text
/api/workouts
```

Request:

```json
{
  "notes": "Push Day"
}
```

Response:

```json
{
  "id": "...",
  "startedAt": "...",
  "endedAt": null,
  "durationSeconds": null,
  "status": "IN_PROGRESS",
  "notes": "Push Day"
}
```

`durationSeconds` in the REST response is derived data; it is not stored on the `Workout` entity.

---

## End Workout

PATCH

```text
/api/workouts/{workoutId}/end
```

Response:

```json
{
  "id": "...",
  "startedAt": "...",
  "endedAt": "...",
  "durationSeconds": 3600,
  "status": "COMPLETED",
  "notes": "Push Day"
}
```

---

## Get Workout

GET

```text
/api/workouts/{workoutId}
```

Response:

```json
{
  "id": "...",
  "startedAt": "...",
  "endedAt": "...",
  "durationSeconds": 3600,
  "status": "COMPLETED",
  "notes": "Push Day"
}
```

---

## List Workouts

GET

```text
/api/workouts
```

Response:

```json
[
  {
    "id": "...",
    "startedAt": "...",
    "endedAt": null,
    "durationSeconds": null,
    "status": "IN_PROGRESS",
    "notes": "Push Day"
  }
]
```

Results are ordered by `startedAt` descending.

---

## Add Workout Exercise

POST

```text
/api/workouts/{workoutId}/exercises
```

Request:

```json
{
  "exerciseName": "Bench Press"
}
```

Response:

```json
{
  "id": "...",
  "exerciseName": "Bench Press",
  "orderIndex": 1
}
```

The backend automatically determines `orderIndex`.

For a second exercise:

```json
{
  "exerciseName": "Incline Bench Press"
}
```

the response contains `orderIndex: 2`.

---

## Add Workout Set

POST

```text
/api/workouts/{workoutId}/exercises/{exerciseId}/sets
```

Request:

```json
{
  "weight": 80,
  "reps": 8,
  "rir": 2,
  "durationSeconds": null,
  "notes": "Felt easy"
}
```

Response:

```json
{
  "id": "...",
  "setNumber": 1,
  "weight": 80,
  "reps": 8,
  "rir": 2,
  "durationSeconds": null,
  "notes": "Felt easy"
}
```

Sets are ordered per exercise using an automatically incremented `setNumber`.

---

# Design Patterns Used

## Layered Architecture

Controller

↓

Service

↓

Repository

---

## DTO Pattern

Entities are never exposed through REST APIs.

DTOs are used for request/response contracts.

---

## Repository Pattern

Persistence logic stays inside repositories.

---

## Mapper Pattern

Converts Entity ↔ DTO.

---

## Producer Pattern

Kafka publishing is encapsulated in producer classes such as `WorkoutEventProducer` and `ExerciseEventProducer`.

Business services do not interact with `KafkaTemplate` directly.

---

## Event Handler Pattern

Kafka listeners contain minimal logic.

They immediately delegate to services.

---

## Publish–Subscribe Pattern

Events are published once.

Multiple consumer groups can independently react.

Examples planned:

- analytics-group
- recommendation-group
- notification-group
- achievement-group

---

# Development Principles

This project intentionally prioritizes:

- Clean Architecture
- Separation of Concerns
- SOLID Principles
- Production-ready design
- Extensibility
- Readability

Over:

- Quick shortcuts
- Large service classes
- Tight coupling

---

# Coding Conventions

- Constructor injection only.
- Never use field injection.
- Use records for immutable request/response DTOs where appropriate.
- Business logic belongs in services.
- Controllers should remain thin.
- Kafka listeners should remain thin.
- Mapper handles object conversion.
- Repository handles persistence only.
- Kafka event contracts should be explicit and independent of JPA entities.
- Avoid exposing persistence models as API or event contracts.

---

# Learning Convention

Whenever introducing a new concept, explain:

1. Why the pattern exists.
2. Which problem it solves.
3. Where it is used in production.
4. Alternatives.
5. Trade-offs.

Do not explain basic Java or Spring concepts unless explicitly asked.

Assume intermediate knowledge of Spring Boot and Java.

---

# Current Infrastructure

Docker Compose services:

- PostgreSQL
- Apache Kafka (KRaft)

Spring Boot connects locally to:

Postgres

```text
localhost:5432
```

Kafka

```text
localhost:9092
```

---

# Current Validation / Business Rules

- Workout must exist before exercises can be added.
- Exercises cannot be added to a completed workout.
- Sets cannot be added where the associated workout is completed.
- Exercise name is required.
- Exercise name has a maximum length of 255 characters.
- Exercise ordering is controlled by the backend.
- Set numbering is controlled by the backend.
- `durationSeconds` at the set level is optional.

---

# Roadmap

## Phase 1

- ✅ Start Workout
- ✅ Kafka Producer
- ✅ Kafka Consumer
- ✅ Analytics Consumer

---

## Phase 2

- ✅ End Workout
- ✅ WorkoutEndedEvent
- ✅ Workout duration
- ✅ Analytics update

---

## Phase 3 (Current)

- ✅ Exercise logging
- ✅ Sets
- ✅ Reps
- ✅ Weight
- ✅ RIR
- ⬜ Personal records

---

## Phase 4

- AI workout summaries
- AI recommendations
- Long-term workout memory

---

## Phase 5

- Authentication
- User accounts
- Multiple users

---

## Phase 6

- Observability
- Metrics
- Logging
- Distributed tracing

---

## Phase 7

- CI/CD
- Deployment
- Kubernetes
- Production readiness

---

# Long-Term Goal

The objective is to build a backend project comparable in quality to systems discussed during Senior Software Engineer interviews at companies such as Google, Amazon, Uber, LinkedIn, Microsoft, and BNY.

Every feature should teach a production concept rather than merely make the application work.
