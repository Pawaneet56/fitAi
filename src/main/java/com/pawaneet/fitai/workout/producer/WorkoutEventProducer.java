package com.pawaneet.fitai.workout.producer;

import com.pawaneet.fitai.kafka.config.KafkaTopicProperties;
import com.pawaneet.fitai.workout.event.WorkoutEndedEvent;
import com.pawaneet.fitai.workout.event.WorkoutStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkoutEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public void publishWorkoutStarted(WorkoutStartedEvent event) {
        kafkaTemplate.send(
                topics.workoutStarted(),
                event.workoutId().toString(),
                event
        );
    }

    public void publishWorkoutEnded(WorkoutEndedEvent event) {
        kafkaTemplate.send(
                topics.workoutEnded(),
                event.workoutId().toString(),
                event
        );
    }
}
