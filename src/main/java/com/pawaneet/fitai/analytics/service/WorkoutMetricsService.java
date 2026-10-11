package com.pawaneet.fitai.analytics.service;

import com.pawaneet.fitai.analytics.dto.WorkoutMetrics;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutMetricsService {
    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;

    @Transactional(readOnly = true)
    public WorkoutMetrics getMetrics(UUID workoutId) {
        if (!workoutRepository.existsById(workoutId)) {
            throw new WorkoutNotFoundException(workoutId);
        }
        return calculate(workoutExerciseRepository.findByWorkoutIdWithSets(workoutId));
    }

    public WorkoutMetrics calculate(List<WorkoutExercise> exercises) {
        List<WorkoutMetrics.ExerciseMetrics> exerciseMetrics = exercises.stream()
                .map(exercise -> {
                    List<WorkoutSet> sets = exercise.getSets();
                    return new WorkoutMetrics.ExerciseMetrics(
                            exercise.getId(), exercise.getExerciseName(), sets.size(),
                            totalReps(sets), totalVolume(sets), highestWeight(sets), averageRir(sets));
                }).toList();
        List<WorkoutSet> allSets = exercises.stream()
                .flatMap(exercise -> exercise.getSets().stream()).toList();
        return new WorkoutMetrics(exercises.size(), allSets.size(), totalReps(allSets),
                totalVolume(allSets), highestWeight(allSets), averageRir(allSets), exerciseMetrics);
    }

    private long totalReps(List<WorkoutSet> sets) {
        return sets.stream().mapToLong(WorkoutSet::getReps).sum();
    }

    private double totalVolume(List<WorkoutSet> sets) {
        return sets.stream().mapToDouble(set -> set.getWeight() * set.getReps()).sum();
    }

    private Double highestWeight(List<WorkoutSet> sets) {
        return sets.stream().mapToDouble(WorkoutSet::getWeight).max().stream()
                .boxed().findFirst().orElse(null);
    }

    private Double averageRir(List<WorkoutSet> sets) {
        return sets.stream().map(WorkoutSet::getRir).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream().boxed().findFirst().orElse(null);
    }
}
