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

---

# Current Package Structure

com.pawaneet.fitai

```
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

---

## Kafka

Kafka Producer

- WorkoutEventProducer

Kafka Consumer

- WorkoutStartedConsumer
- WorkoutEndedConsumer

Kafka Topic

- workout-started
- workout-ended

Kafka configuration uses NewTopic beans instead of relying on broker auto-topic creation.

---

## Analytics

AnalyticsService receives WorkoutStartedEvent and currently logs receipt of the event.
AnalyticsService receives WorkoutEndedEvent and currently logs workout completion duration.

---

# Event Flow

POST /api/workouts

↓

WorkoutController

↓

WorkoutService

↓

Save Workout

↓

Publish WorkoutStartedEvent

↓

Kafka Topic

↓

WorkoutStartedConsumer

↓

AnalyticsService

---

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

Calculate duration

↓

Publish WorkoutEndedEvent

↓

Kafka Topic

↓

WorkoutEndedConsumer

↓

AnalyticsService

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

---

## Repository Pattern

Persistence logic stays inside repositories.

---

## Mapper Pattern

Converts Entity ↔ DTO.

---

## Producer Pattern

Kafka publishing is encapsulated in WorkoutEventProducer.

Business services never interact with KafkaTemplate directly.

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

Docker Compose

Services:

- PostgreSQL
- Apache Kafka (KRaft)

Spring Boot connects to:

Postgres

```
localhost:5432
```

Kafka

```
localhost:9092
```

---

# Current APIs

## Start Workout

POST

```
/api/workouts
```

Request

```json
{
  "notes": "Push Day"
}
```

Response

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

---

## End Workout

PATCH

```
/api/workouts/{workoutId}/end
```

Response

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

```
/api/workouts/{workoutId}
```

Response

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

```
/api/workouts
```

Response

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

```
/api/workouts/{workoutId}/exercises
```

Request

```json
{
  "exerciseName": "Bench Press"
}
```

Response

```json
{
  "id": "...",
  "exerciseName": "Bench Press",
  "orderIndex": 1
}
```

Exercises are ordered per workout using an automatically incremented `orderIndex`.

---

## Add Workout Set

POST

```
/api/workouts/{workoutId}/exercises/{exerciseId}/sets
```

Request

```json
{
  "weight": 80,
  "reps": 8,
  "rir": 2,
  "durationSeconds": null,
  "notes": "Felt easy"
}
```

Response

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

# Roadmap

## Phase 1

- ✅ Start Workout
- ✅ Kafka Producer
- ✅ Kafka Consumer
- ✅ Analytics Consumer

---

## Phase 2 (Current)

- ✅ End Workout
- ✅ WorkoutEndedEvent
- ✅ Workout duration
- ✅ Analytics update

---

## Phase 3

- ✅ Exercise logging
- ✅ Sets
- Reps
- Weight
- Personal records

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
