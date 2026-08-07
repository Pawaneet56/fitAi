package com.pawaneet.fitai.workout.producer;

import com.pawaneet.fitai.kafka.config.KafkaTopicProperties;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.event.WorkoutEndedEvent;
import com.pawaneet.fitai.workout.event.WorkoutStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkoutEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public void publishWorkoutStarted(Workout workout) {

        WorkoutStartedEvent event = new WorkoutStartedEvent(
                workout.getId(),
                workout.getStartedAt()
        );

        kafkaTemplate.send(
                topics.workoutStarted(),
                workout.getId().toString(),
                event
        );
    }

    public void publishWorkoutEnded(Workout workout) {

        Long durationSeconds = null;

        if (workout.getEndedAt() != null) {
            durationSeconds = Duration.between(
                    workout.getStartedAt(),
                    workout.getEndedAt()
            ).getSeconds();
        }

        WorkoutEndedEvent event = new WorkoutEndedEvent(
                workout.getId(),
                workout.getStartedAt(),
                workout.getEndedAt(),
                durationSeconds
        );

        kafkaTemplate.send(
                topics.workoutEnded(),
                workout.getId().toString(),
                event
        );
    }
}
