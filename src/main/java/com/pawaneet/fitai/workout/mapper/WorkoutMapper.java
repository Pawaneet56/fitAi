package com.pawaneet.fitai.workout.mapper;

import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import org.springframework.stereotype.Component;

@Component
public class WorkoutMapper {

    public WorkoutResponse toResponse(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getStartedAt(),
                workout.getStatus(),
                workout.getNotes()
        );
    }
}