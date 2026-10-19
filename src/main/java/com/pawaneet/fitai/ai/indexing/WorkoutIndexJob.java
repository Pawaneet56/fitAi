package com.pawaneet.fitai.ai.indexing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_index_jobs")
@Getter
@Setter
@NoArgsConstructor
public class WorkoutIndexJob {
    @Id
    private UUID workoutId;
    @Column(nullable = false)
    private String userId = CurrentWorkoutUser.ID;
    @Column(nullable = false)
    private String status = "PENDING";
    private String operationName;
    private String documentName;
    @Column(nullable = false)
    private Instant nextDispatchAt = Instant.now();
    private Instant indexedAt;
    private int attempts;
    @Column(length = 2000)
    private String lastError;

    public WorkoutIndexJob(UUID workoutId) { this.workoutId = workoutId; }
}
