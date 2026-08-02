package com.pawaneet.fitai.workout.mapper;

import com.pawaneet.fitai.workout.dto.WorkoutSetResponse;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import org.springframework.stereotype.Component;

@Component
public class WorkoutSetMapper {

    public WorkoutSetResponse toResponse(WorkoutSet workoutSet) {
        return new WorkoutSetResponse(
                workoutSet.getId(),
                workoutSet.getSetNumber(),
                workoutSet.getWeight(),
                workoutSet.getReps(),
                workoutSet.getRir(),
                workoutSet.getDurationSeconds(),
                workoutSet.getNotes()
        );
    }
}
