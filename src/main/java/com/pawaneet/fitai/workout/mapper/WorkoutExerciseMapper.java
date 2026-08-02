package com.pawaneet.fitai.workout.mapper;

import com.pawaneet.fitai.workout.dto.WorkoutExerciseResponse;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import org.springframework.stereotype.Component;

@Component
public class WorkoutExerciseMapper {

    public WorkoutExerciseResponse toResponse(WorkoutExercise workoutExercise) {
        return new WorkoutExerciseResponse(
                workoutExercise.getId(),
                workoutExercise.getExerciseName(),
                workoutExercise.getOrderIndex()
        );
    }
}
