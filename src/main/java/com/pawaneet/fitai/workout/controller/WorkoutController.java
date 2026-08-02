package com.pawaneet.fitai.workout.controller;

import com.pawaneet.fitai.workout.dto.StartWorkoutRequest;
import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    public ResponseEntity<WorkoutResponse> startWorkout(
            @RequestBody StartWorkoutRequest request) {

        WorkoutResponse response = workoutService.startWorkout(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
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
}
