package com.pawaneet.fitai.workout.service;

import com.pawaneet.fitai.workout.dto.AddWorkoutSetRequest;
import com.pawaneet.fitai.workout.dto.WorkoutSetResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.exception.ConflictException;
import com.pawaneet.fitai.workout.exception.ExerciseNotFoundException;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.mapper.WorkoutSetMapper;
import com.pawaneet.fitai.workout.producer.SetEventProducer;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import com.pawaneet.fitai.workout.repository.WorkoutSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutSetService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final WorkoutSetMapper workoutSetMapper;
    private final SetEventProducer setEventProducer;

    @Transactional
    public WorkoutSetResponse addSet(UUID workoutId, UUID exerciseId, AddWorkoutSetRequest request) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutId));

        if (workout.getStatus() == WorkoutStatus.COMPLETED) {
            throw new ConflictException("Cannot add sets to completed workout with id: " + workoutId);
        }

        WorkoutExercise workoutExercise = workoutExerciseRepository.findByIdAndWorkoutId(exerciseId, workoutId)
                .orElseThrow(() -> new ExerciseNotFoundException(exerciseId));

        int nextSetNumber = workoutSetRepository.findMaxSetNumberByExerciseId(exerciseId) + 1;
        WorkoutSet workoutSet = WorkoutSet.builder()
                .exercise(workoutExercise)
                .setNumber(nextSetNumber)
                .weight(request.weight())
                .reps(request.reps())
                .rir(request.rir())
                .durationSeconds(request.durationSeconds())
                .notes(request.notes())
                .build();

        workoutExercise.addSet(workoutSet);
        WorkoutSet savedWorkoutSet = workoutSetRepository.save(workoutSet);

        setEventProducer.publishSetAdded(savedWorkoutSet);

        return workoutSetMapper.toResponse(savedWorkoutSet);
    }
}
