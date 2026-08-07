package com.pawaneet.fitai.workout.controller;

import com.pawaneet.fitai.workout.dto.*;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.service.WorkoutExerciseService;
import com.pawaneet.fitai.workout.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;
    private final WorkoutExerciseService workoutExerciseService;

    @PostMapping
    public ResponseEntity<WorkoutResponse> startWorkout(@RequestBody StartWorkoutRequest request) {
        WorkoutResponse response = workoutService.startWorkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{workoutId}")
    public ResponseEntity<WorkoutResponse> getWorkout(@PathVariable UUID workoutId) {
        WorkoutResponse response = workoutService.getWorkout(workoutId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkoutResponse>> getWorkouts() {
        List<WorkoutResponse> response = workoutService.getWorkouts();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{workoutId}/end")
    public ResponseEntity<WorkoutResponse> endWorkout(@PathVariable UUID workoutId) {
        WorkoutResponse response = workoutService.endWorkout(workoutId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{workoutId}/exercises")
    public ResponseEntity<WorkoutExerciseResponse> addExerciseToWorkout(
            @PathVariable UUID workoutId,
            @Valid @RequestBody AddWorkoutExerciseRequest request
    ) {
        WorkoutExerciseResponse exercise = workoutExerciseService.addExercise(workoutId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(exercise);
    }
}
