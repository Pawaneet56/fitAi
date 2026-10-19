# Workout indexing and retrieval (single-user version)

Every workout currently belongs to the fixed backend identity `user1`. This is
an explicit single-user implementation; there is no login or per-user access
control. Existing workouts are treated as user1's workouts when queued.

## Completion flow

1. `PATCH /api/workouts/{id}/end` saves the completed workout and a `PENDING`
   `workout_index_jobs` row in the same PostgreSQL transaction.
2. Every 10 seconds the dispatcher picks up due jobs and publishes a
   `WorkoutIndexRequestedEvent` on the `workout-index-requested` Kafka topic.
   The event contains only the workout ID and fixed user ID.
3. A dedicated Kafka consumer loads the completed workout and calculates its
   metrics in Java. It serializes one text document containing the identity,
   timestamps, notes, exercises, sets, and verified metrics, then uploads a named temporary Google file and imports it with metadata into
   the configured Gemini File Search store. This avoids the SDK 1.44 direct-store
   uploader dropping display names and custom metadata.
4. Custom metadata includes `user_id`, `workout_id`, and `workout_date`.
   Dates use UTC with fixed nine-digit fractional seconds. A deterministic
   display name is supplied on upload, but document identity and recovery use
   verified metadata because Google can generate its own file/resource names.
5. The upload operation name is stored in PostgreSQL. Subsequent Kafka events
   resume that operation rather than uploading the workout again.
6. The job becomes `READY` only when Google reports the document is active and
   all user/workout/date tags match the expected values. An
   incorrectly tagged document is replaced before retrying.
   The frontend polls the status and enables summary generation at readiness.

Workouts can still be completed when Kafka or Google is unavailable: the indexing
job is committed locally first. Kafka publication retries until the broker
acknowledges it, and Google processing resumes through repeated events. The
existing analytics workout-ended event remains separate from this indexing outbox.

## Summary flow

`GET /api/workouts/{id}/summary` validates completion and indexing readiness.
It sends instructions, user1's ID, the target workout ID, and its completion
instant to Gemini with File Search enabled. It does **not** load the workout's
sets or send the full workout/history in the generation prompt.

PostgreSQL selects up to 20 ready workout IDs for user1 at or before the target
completion time. The target ID is always included. These IDs form an exact
metadata filter; workout contents are not loaded into the prompt. This bounds
retrieval and excludes later sessions without relying on provider string-date
range comparisons.

The client requires provider evidence that File Search executed (retrieval token
usage or a citation for the target document). The model must also echo an
internal `_source` containing the target workout ID and its exact start timestamp
from the retrieved document. That timestamp is not included in the generation
request. The backend compares it with PostgreSQL and removes `_source` before
returning the response. This handles Gemini omitting citation metadata from JSON
answers while still rejecting missing or mismatched record identity.

The retrieval call requests JSON through instructions; the backend validates
required fields and array element types before returning the stable response
contract. Native response-schema constraints remain available for generic calls
without File Search.

The summary response contract remains `summary`, `observations`, and `suggestions`.
Earlier retrieved workouts can support comparisons, but retrieval is not a
complete historical query. Exact history-wide totals should remain database
calculations. Summaries are still generated on demand and are not persisted.

## Existing workouts and recovery

Older completed workouts have no indexing job until explicitly queued. In the
frontend, select the session and choose **Prepare workout insights**.

```http
POST /api/workouts/{workoutId}/index
GET  /api/workouts/{workoutId}/index
```

POST returns 202 with the current job status and is safe to repeat. GET returns
`NOT_QUEUED`, `PENDING`, `INDEXING`, `FAILED`, or `READY`, along with `userId`,
`indexedAt`, and `failedAttempts`. Unknown workouts return 404; queuing active
workouts or requesting a summary before readiness returns 409.

Failures remain persisted and retry automatically, normally after 30 seconds.
**Retry insight preparation** makes a failed job eligible immediately. A
terminally failed Google document is removed before re-upload; transient request
failures retain existing operation/document IDs. Matching user/workout/date metadata
also allows recovery when an upload succeeded before its local state was saved.
Import operation document IDs are normalized, and a listing lookup with verified
metadata resolves cases where the final document ID differs from the operation's
returned ID.
These safeguards reduce duplicates but cannot guarantee exactly-once remote
uploads across the gap between Google accepting a request and PostgreSQL saving
its response, especially while remote document listings are eventually consistent.

## Configuration and startup

Keep PostgreSQL and Kafka running, then restart the backend to apply the new
entity, listener, scheduled dispatcher, and topic definition. Current development
configuration uses Hibernate `ddl-auto: update` to create `workout_index_jobs`.
Restart or reload the frontend after deploying the backend changes.

Configure `GEMINI_API_KEY` (or the SDK's existing supported credentials) in the
backend environment. `GEMINI_FILE_SEARCH_STORE` overrides the existing configured
store name. The store must exist and be accessible to those credentials. No
credentials belong in frontend configuration.

```yaml
fitai:
  ai:
    file-search:
      store-name: ${GEMINI_FILE_SEARCH_STORE:your-existing-store}
      dispatch-delay-ms: 10000
      retry-delay-seconds: 30
```

`GEMINI_MODEL` optionally selects the model; the default remains `gemini-3.6-flash`.
The model must support File Search. Provider calls have bounded timeouts, and
Gemini quota failures return HTTP 429 with a readable message. Other provider
failures return 502/503 rather than exposing provider stack traces.

Live verification confirmed HTTP logging, edits, validation, exact metrics,
completion restrictions, Kafka consumption, metadata-preserving imports, indexing
readiness, and retrieval of the correct synthetic workout contents. The last
summary acceptance check after the `_source` verification change was blocked by
Google's daily free-tier request quota. The final API/UI summary success still
needs a live recheck once quota is available.
