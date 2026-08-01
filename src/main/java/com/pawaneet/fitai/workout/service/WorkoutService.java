package com.pawaneet.fitai.workout.service;

import com.pawaneet.fitai.workout.dto.StartWorkoutRequest;
import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.event.WorkoutStartedEvent;
import com.pawaneet.fitai.workout.mapper.WorkoutMapper;
import com.pawaneet.fitai.workout.producer.WorkoutEventProducer;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;

    private final WorkoutMapper workoutMapper;

    private final WorkoutEventProducer workoutEventProducer;

    public WorkoutResponse startWorkout(StartWorkoutRequest request) {
        Workout workout = Workout.builder().startedAt(Instant.now()).status(WorkoutStatus.IN_PROGRESS).notes(request.notes()).build();
        Workout savedWorkout = workoutRepository.save(workout);
        workoutEventProducer.publishWorkoutStarted(
                new WorkoutStartedEvent(
                        savedWorkout.getId(),
                        savedWorkout.getStartedAt()
                )
        );

        return workoutMapper.toResponse(savedWorkout);
    }
}
