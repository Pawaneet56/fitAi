package com.pawaneet.fitai.ai.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkoutIndexConsumer {
    private final WorkoutIndexingService indexing;

    @KafkaListener(topics = WorkoutIndexEventProducer.TOPIC, groupId = "fitai-workout-indexing-group")
    public void consume(WorkoutIndexRequestedEvent event) {
        if (!CurrentWorkoutUser.ID.equals(event.userId())) {
            throw new IllegalArgumentException("Only user1 is supported by the current single-user application");
        }
        indexing.process(event.workoutId());
    }
}
