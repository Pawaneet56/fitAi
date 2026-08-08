package com.pawaneet.fitai.workout.mapper;

import com.pawaneet.fitai.workout.dto.WorkoutExerciseResponse;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class WorkoutExerciseMapper {

    private final WorkoutSetMapper workoutSetMapper;

    public WorkoutExerciseResponse toResponse(WorkoutExercise workoutExercise) {
        return new WorkoutExerciseResponse(
                workoutExercise.getId(),
                workoutExercise.getExerciseName(),
                workoutExercise.getOrderIndex(),
                workoutExercise.getSets()
                        .stream()
                        .sorted(Comparator.comparing(WorkoutSet::getSetNumber))
                        .map(workoutSetMapper::toResponse)
                        .toList()
        );
    }
}
