package com.pawaneet.fitai.workout.controller;

import com.pawaneet.fitai.workout.dto.StartWorkoutRequest;
import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}