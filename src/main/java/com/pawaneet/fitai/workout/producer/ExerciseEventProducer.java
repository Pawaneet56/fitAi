package com.pawaneet.fitai.workout.producer;

import com.pawaneet.fitai.kafka.config.KafkaTopicProperties;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.event.ExerciseAddedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExerciseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public void publishExerciseAdded(WorkoutExercise exercise) {

        ExerciseAddedEvent event = new ExerciseAddedEvent(
                exercise.getWorkout().getId(),
                exercise.getId(),
                exercise.getExerciseName(),
                exercise.getOrderIndex()
        );

        kafkaTemplate.send(
                topics.exerciseAdded(),
                exercise.getWorkout().getId().toString(),
                event
        );
    }
}