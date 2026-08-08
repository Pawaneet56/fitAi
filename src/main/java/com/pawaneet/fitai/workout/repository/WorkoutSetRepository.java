package com.pawaneet.fitai.workout.repository;

import com.pawaneet.fitai.workout.entity.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {


    Optional<WorkoutSet> findByIdAndExerciseId(
            UUID setId,
            UUID exerciseId
    );

    @Query("""
            select coalesce(max(workoutSet.setNumber), 0)
            from WorkoutSet workoutSet
            where workoutSet.exercise.id = :exerciseId
            """)
    int findMaxSetNumberByExerciseId(@Param("exerciseId") UUID exerciseId);
}
