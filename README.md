# FitAI

AI-powered workout tracking platform.

## Features

- Workout logging
- Kafka event streaming
- AI workout summaries
- Analytics
- Recommendations

## Tech Stack

- Java 21
- Spring Boot
- Kafka
- PostgreSQL
- Angular
- Docker

## Workout metrics

`GET /api/workouts/{workoutId}/metrics` returns current metrics for an existing
workout, including in-progress workouts. Unknown workout IDs return 404.

- Workout and exercise totals: sets, reps, volume, highest weight, average RIR.
- Volume is the sum of `weight × reps`, using the logged weight unit. It does
  not estimate bodyweight load or account for unilateral/dumbbell multipliers.
- Average RIR includes only recorded values and weights each set equally.
- Empty workouts have zero totals. Highest weight and average RIR are `null`
  when there are no relevant recorded values.

Completed-workout AI summaries receive these backend-computed metrics alongside
the raw sets. No metrics are persisted; they reflect the current workout data.

## Architecture

(We'll add diagram later)
