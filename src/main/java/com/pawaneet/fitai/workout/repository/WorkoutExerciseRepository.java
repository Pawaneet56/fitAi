package com.pawaneet.fitai.workout.repository;

import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {

    @Query("""
            select coalesce(max(workoutExercise.orderIndex), 0)
            from WorkoutExercise workoutExercise
            where workoutExercise.workout.id = :workoutId
            """)
    int findMaxOrderIndexByWorkoutId(@Param("workoutId") UUID workoutId);

    Optional<WorkoutExercise> findByIdAndWorkoutId(UUID id, UUID workoutId);
}
