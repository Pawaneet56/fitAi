package com.pawaneet.fitai.ai.workout;

import com.pawaneet.fitai.ai.dto.AiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutSummaryController {

    private final WorkoutSummaryService workoutSummaryService;

    @GetMapping("/{workoutId}/summary")
    public ResponseEntity<AiResponse> generateSummary(
            @PathVariable UUID workoutId) {

        return ResponseEntity.ok(
                workoutSummaryService.generateSummary(workoutId)
        );
    }
}