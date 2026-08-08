package com.pawaneet.fitai.workout.mapper;

import com.pawaneet.fitai.workout.dto.WorkoutExerciseResponse;
import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.dto.WorkoutSetResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Component
public class WorkoutMapper {

    public WorkoutResponse toResponse(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getStartedAt(),
                workout.getEndedAt(),
                durationSeconds(workout),
                workout.getStatus(),
                workout.getNotes(),
                toExerciseResponses(workout.getExercises())
        );
    }

    private List<WorkoutExerciseResponse> toExerciseResponses(
            List<WorkoutExercise> exercises
    ) {
        return exercises.stream()
                .sorted(Comparator.comparing(WorkoutExercise::getOrderIndex))
                .map(this::toExerciseResponse)
                .toList();
    }

    private WorkoutExerciseResponse toExerciseResponse(
            WorkoutExercise exercise
    ) {
        return new WorkoutExerciseResponse(
                exercise.getId(),
                exercise.getExerciseName(),
                exercise.getOrderIndex(),
                toSetResponses(exercise.getSets())
        );
    }

    private List<WorkoutSetResponse> toSetResponses(
            List<WorkoutSet> sets
    ) {
        return sets.stream()
                .sorted(Comparator.comparing(WorkoutSet::getSetNumber))
                .map(this::toSetResponse)
                .toList();
    }

    private WorkoutSetResponse toSetResponse(WorkoutSet set) {
        return new WorkoutSetResponse(
                set.getId(),
                set.getSetNumber(),
                set.getWeight(),
                set.getReps(),
                set.getRir(),
                set.getDurationSeconds(),
                set.getNotes()
        );
    }

    private Long durationSeconds(Workout workout) {
        if (workout.getEndedAt() == null) {
            return null;
        }

        return Duration.between(
                workout.getStartedAt(),
                workout.getEndedAt()
        ).toSeconds();
    }
}