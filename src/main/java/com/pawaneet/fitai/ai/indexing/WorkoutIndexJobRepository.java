package com.pawaneet.fitai.ai.indexing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutIndexJobRepository extends JpaRepository<WorkoutIndexJob, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from WorkoutIndexJob j where j.workoutId = :id")
    Optional<WorkoutIndexJob> findLockedById(UUID id);

    List<WorkoutIndexJob> findTop20ByStatusNotAndNextDispatchAtBeforeOrderByNextDispatchAtAsc(String status, Instant now);

    @Query("""
            select j.workoutId from WorkoutIndexJob j, com.pawaneet.fitai.workout.entity.Workout w
            where j.workoutId = w.id and j.userId = :userId and j.status = 'READY'
            and w.endedAt <= :completedAt order by w.endedAt desc
            """)
    List<UUID> findReadyWorkoutIds(String userId, Instant completedAt,
                                  org.springframework.data.domain.Pageable pageable);
}
