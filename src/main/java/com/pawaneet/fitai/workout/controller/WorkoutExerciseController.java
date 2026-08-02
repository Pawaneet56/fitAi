package com.pawaneet.fitai.workout.controller;

import com.pawaneet.fitai.workout.dto.AddWorkoutExerciseRequest;
import com.pawaneet.fitai.workout.dto.WorkoutExerciseResponse;
import com.pawaneet.fitai.workout.service.WorkoutExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workouts/{workoutId}/exercises")
@RequiredArgsConstructor
public class WorkoutExerciseController {

    private final WorkoutExerciseService workoutExerciseService;

    @PostMapping
    public ResponseEntity<WorkoutExerciseResponse> addExercise(
            @PathVariable UUID workoutId,
            @Valid @RequestBody AddWorkoutExerciseRequest request) {

        WorkoutExerciseResponse response = workoutExerciseService.addExercise(workoutId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
