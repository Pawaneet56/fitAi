package com.pawaneet.fitai.analytics.service;

import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkoutMetricsServiceTest {
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final WorkoutExerciseRepository exercises = mock(WorkoutExerciseRepository.class);
    private final WorkoutMetricsService service = new WorkoutMetricsService(workouts, exercises);

    @Test
    void calculatesTotalsAndWeightsRirByRecordedSetsRatherThanExerciseAverages() {
        WorkoutExercise bench = exercise("Bench", set(80, 8, 0), set(82.5, 6, 2), set(70, 10, null));
        WorkoutExercise press = exercise("Press", set(40, 5, 4));
        var metrics = service.calculate(List.of(bench, press));

        assertThat(metrics.totalExercises()).isEqualTo(2);
        assertThat(metrics.totalSets()).isEqualTo(4);
        assertThat(metrics.totalReps()).isEqualTo(29);
        assertThat(metrics.totalVolume()).isEqualTo(2035);
        assertThat(metrics.highestWeight()).isEqualTo(82.5);
        assertThat(metrics.averageRir()).isEqualTo(2);
        var benchMetrics = metrics.exercises().getFirst();
        assertThat(benchMetrics.exerciseId()).isEqualTo(bench.getId());
        assertThat(benchMetrics.totalSets()).isEqualTo(3);
        assertThat(benchMetrics.totalVolume()).isEqualTo(1835);
        assertThat(benchMetrics.averageRir()).isEqualTo(1);
    }

    @Test
    void emptyWorkoutHasZeroTotalsAndUnavailableMaximumAndAverage() {
        var metrics = service.calculate(List.of());
        assertThat(metrics.totalExercises()).isZero();
        assertThat(metrics.totalSets()).isZero();
        assertThat(metrics.totalReps()).isZero();
        assertThat(metrics.totalVolume()).isZero();
        assertThat(metrics.highestWeight()).isNull();
        assertThat(metrics.averageRir()).isNull();
        assertThat(metrics.exercises()).isEmpty();
    }

    @Test
    void distinguishesEmptyExercisesFromRecordedZeroWeightAndMissingRir() {
        var metrics = service.calculate(List.of(exercise("Empty"), exercise("Bodyweight", set(0, 10, null))));
        assertThat(metrics.exercises().getFirst().highestWeight()).isNull();
        assertThat(metrics.exercises().get(1).highestWeight()).isZero();
        assertThat(metrics.highestWeight()).isZero();
        assertThat(metrics.averageRir()).isNull();
        assertThat(metrics.totalVolume()).isZero();
        assertThat(metrics.totalReps()).isEqualTo(10);
    }

    @Test
    void loadsExercisesWithSetsForExistingWorkout() {
        UUID id = UUID.randomUUID();
        when(workouts.existsById(id)).thenReturn(true);
        when(exercises.findByWorkoutIdWithSets(id)).thenReturn(List.of(exercise("Bench", set(10, 2, 1))));
        assertThat(service.getMetrics(id).totalVolume()).isEqualTo(20);
    }

    @Test
    void missingWorkoutDoesNotReturnEmptyMetrics() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.getMetrics(id)).isInstanceOf(WorkoutNotFoundException.class);
        verifyNoInteractions(exercises);
    }

    private WorkoutExercise exercise(String name, WorkoutSet... sets) {
        return WorkoutExercise.builder().id(UUID.randomUUID()).exerciseName(name).sets(List.of(sets)).build();
    }

    private WorkoutSet set(double weight, int reps, Integer rir) {
        return WorkoutSet.builder().weight(weight).reps(reps).rir(rir).build();
    }
}
