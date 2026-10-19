package com.pawaneet.fitai.ai.indexing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawaneet.fitai.analytics.service.WorkoutMetricsService;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.LinkedHashMap;

@Component
@RequiredArgsConstructor
public class WorkoutDocumentBuilder {
    private final WorkoutMetricsService metrics;
    private final ObjectMapper objectMapper;

    public String build(Workout workout, List<WorkoutExercise> exercises) {
        var document = new LinkedHashMap<String, Object>();
        document.put("userId", CurrentWorkoutUser.ID);
        document.put("workoutId", workout.getId());
        document.put("startedAt", workout.getStartedAt().toString());
        document.put("completedAt", workout.getEndedAt().toString());
        document.put("durationSeconds", java.time.Duration.between(workout.getStartedAt(), workout.getEndedAt()).getSeconds());
        document.put("notes", workout.getNotes());
        document.put("metrics", metrics.calculate(exercises));
        document.put("exercises", exercises.stream().map(exercise -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("exerciseName", exercise.getExerciseName());
            item.put("orderIndex", exercise.getOrderIndex());
            item.put("sets", exercise.getSets().stream().map(set -> {
                var values = new LinkedHashMap<String, Object>();
                values.put("setNumber", set.getSetNumber()); values.put("weight", set.getWeight());
                values.put("reps", set.getReps()); values.put("rir", set.getRir());
                values.put("durationSeconds", set.getDurationSeconds()); values.put("notes", set.getNotes());
                return values;
            }).toList());
            return item;
        }).toList());
        try {
            return "Completed workout record. Metrics are computed by the backend. Weight units are unspecified.\n"
                    + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        } catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize workout document", e); }
    }
}
