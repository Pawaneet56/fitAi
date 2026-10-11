package com.pawaneet.fitai.analytics.controller;

import com.pawaneet.fitai.analytics.dto.WorkoutMetrics;
import com.pawaneet.fitai.analytics.service.WorkoutMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutMetricsController {
    private final WorkoutMetricsService metricsService;

    @GetMapping("/{workoutId}/metrics")
    public WorkoutMetrics getMetrics(@PathVariable UUID workoutId) {
        return metricsService.getMetrics(workoutId);
    }
}
