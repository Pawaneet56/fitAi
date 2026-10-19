package com.pawaneet.fitai.ai.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class WorkoutIndexEventProducer {
    public static final String TOPIC = "workout-index-requested";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final WorkoutIndexJobRepository jobs;
    @org.springframework.beans.factory.annotation.Value("${fitai.ai.file-search.retry-delay-seconds:30}")
    private long retryDelaySeconds;

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void publish(java.util.UUID id) throws Exception {
        WorkoutIndexJob job = jobs.findLockedById(id).orElseThrow();
        if ("READY".equals(job.getStatus()) || job.getNextDispatchAt().isAfter(java.time.Instant.now())) return;
        job.setNextDispatchAt(java.time.Instant.now().plusSeconds(retryDelaySeconds));
        kafkaTemplate.send(TOPIC, job.getWorkoutId().toString(),
                new WorkoutIndexRequestedEvent(job.getWorkoutId(), job.getUserId())).get(10, TimeUnit.SECONDS);
    }
}
