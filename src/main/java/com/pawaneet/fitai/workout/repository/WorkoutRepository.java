package com.pawaneet.fitai.workout.repository;

import com.pawaneet.fitai.workout.entity.Workout;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select w from Workout w where w.id = :id")
    Optional<Workout> lockById(UUID id);

    @EntityGraph(attributePaths = "exercises")
    Optional<Workout> findWithExercisesById(UUID workoutId);

    List<Workout> findAllByOrderByStartedAtDesc();
}
