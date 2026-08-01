package com.pawaneet.fitai.workout.producer;

import com.pawaneet.fitai.kafka.config.KafkaTopicProperties;
import com.pawaneet.fitai.workout.event.WorkoutStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkoutEventProducer {

    private final KafkaTemplate<String, WorkoutStartedEvent> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public void publishWorkoutStarted(WorkoutStartedEvent event) {
        kafkaTemplate.send(
                topics.workoutStarted(),
                event.workoutId().toString(),
                event
        );
    }
}
