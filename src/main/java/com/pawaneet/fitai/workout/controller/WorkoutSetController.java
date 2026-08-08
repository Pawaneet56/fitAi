package com.pawaneet.fitai.workout.controller;

import com.pawaneet.fitai.workout.dto.AddWorkoutSetRequest;
import com.pawaneet.fitai.workout.dto.UpdateWorkoutSetRequest;
import com.pawaneet.fitai.workout.dto.WorkoutSetResponse;
import com.pawaneet.fitai.workout.service.WorkoutSetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workouts/{workoutId}/exercises/{exerciseId}/sets")
@RequiredArgsConstructor
public class WorkoutSetController {

    private final WorkoutSetService workoutSetService;

    @PostMapping
    public ResponseEntity<WorkoutSetResponse> addSet(
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody AddWorkoutSetRequest request) {

        WorkoutSetResponse response = workoutSetService.addSet(workoutId, exerciseId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(
            "/{setId}"
    )
    public ResponseEntity<WorkoutSetResponse> updateSet(
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId,
            @PathVariable UUID setId,
            @Valid @RequestBody UpdateWorkoutSetRequest request
    ) {
        return ResponseEntity.ok(
                workoutSetService.updateSet(
                        workoutId,
                        exerciseId,
                        setId,
                        request
                )
        );
    }
}
