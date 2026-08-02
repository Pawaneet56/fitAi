package com.pawaneet.fitai.workout.mapper;

import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WorkoutMapper {

    public WorkoutResponse toResponse(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getStartedAt(),
                workout.getEndedAt(),
                durationSeconds(workout),
                workout.getStatus(),
                workout.getNotes()
        );
    }

    private Long durationSeconds(Workout workout) {
        if (workout.getEndedAt() == null) {
            return null;
        }

        return Duration.between(workout.getStartedAt(), workout.getEndedAt()).toSeconds();
    }
}
