package com.pawaneet.fitai.ai.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/workouts/{workoutId}/index")
@RequiredArgsConstructor
public class WorkoutIndexController {
    private final WorkoutIndexingService indexing;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkoutIndexingService.IndexStatus enqueue(@PathVariable UUID workoutId) { return indexing.enqueue(workoutId); }

    @GetMapping
    public WorkoutIndexingService.IndexStatus status(@PathVariable UUID workoutId) { return indexing.getStatus(workoutId); }
}
