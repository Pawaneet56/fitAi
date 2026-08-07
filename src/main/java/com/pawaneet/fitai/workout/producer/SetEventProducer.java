package com.pawaneet.fitai.workout.producer;

import com.pawaneet.fitai.kafka.config.KafkaTopicProperties;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import com.pawaneet.fitai.workout.event.SetAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SetEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public void publishSetAdded(WorkoutSet workoutSet){
        SetAddedEvent event = new SetAddedEvent(
                workoutSet.getExercise().getWorkout().getId(),
                workoutSet.getExercise().getId(),
                workoutSet.getId(),
                workoutSet.getSetNumber(),
                workoutSet.getWeight(),
                workoutSet.getReps(),
                workoutSet.getRir(),
                workoutSet.getDurationSeconds(),
                workoutSet.getNotes(),
                workoutSet.getCreatedAt()
        );
        kafkaTemplate.send(topics.setAdded(), workoutSet.getId().toString(), event);
    }

}
