package com.pawaneet.fitai.workout.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "workout_exercises",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_workout_exercise_order",
                columnNames = {"workout_id", "order_index"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(nullable = false, length = 255)
    private String exerciseName;

    @Column(nullable = false)
    private Integer orderIndex;

    @OneToMany(
            mappedBy = "exercise",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @BatchSize(size = 20)
    @Builder.Default
    private List<WorkoutSet> sets = new ArrayList<>();

    public void addSet(WorkoutSet workoutSet) {
        sets.add(workoutSet);
        workoutSet.setExercise(this);
    }
}
