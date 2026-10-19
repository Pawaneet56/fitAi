package com.pawaneet.fitai.ai.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

/** The database job doubles as an outbox: retry until Google confirms indexing. */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkoutIndexDispatcher {
    private final WorkoutIndexJobRepository jobs;
    private final WorkoutIndexEventProducer producer;

    @Scheduled(fixedDelayString = "${fitai.ai.file-search.dispatch-delay-ms:10000}")
    public void dispatch() {
        for (WorkoutIndexJob job : jobs.findTop20ByStatusNotAndNextDispatchAtBeforeOrderByNextDispatchAtAsc("READY", Instant.now())) {
            try {
                producer.publish(job.getWorkoutId());
            } catch (Exception e) {
                log.warn("Could not dispatch indexing for workout {}: {}", job.getWorkoutId(), e.getMessage());
                if (e instanceof InterruptedException) { Thread.currentThread().interrupt(); return; }
            }
        }
    }
}
