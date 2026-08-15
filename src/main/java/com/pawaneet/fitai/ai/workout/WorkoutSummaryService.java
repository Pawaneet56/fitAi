package com.pawaneet.fitai.ai.workout;

import com.pawaneet.fitai.ai.dto.AiResponse;
import com.pawaneet.fitai.ai.service.AiService;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.exception.ConflictException;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutSummaryService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final AiService aiService;
    private final WorkoutSummaryPromptBuilder promptBuilder;

    @Transactional(readOnly = true)
    public AiResponse generateSummary(UUID workoutId) {

        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutId));

        if (workout.getStatus() != WorkoutStatus.COMPLETED) {
            throw new ConflictException(
                    "Workout summary can only be generated for completed workouts"
            );
        }
        List<WorkoutExercise> exercises =
                workoutExerciseRepository.findByWorkoutIdWithSets(workoutId);

        WorkoutSummaryContext context = new WorkoutSummaryContext(
                workout.getId(),
                workout.getStartedAt(),
                workout.getEndedAt(),
                workout.getNotes(),
                exercises.stream()
                        .map(this::toExerciseContext)
                        .toList()
        );

        return aiService.generate(
                promptBuilder.build(context)
        );
    }

    private WorkoutSummaryContext.ExerciseContext toExerciseContext(
            WorkoutExercise exercise) {

        List<WorkoutSummaryContext.SetContext> sets =
                exercise.getSets().stream()
                        .map(this::toSetContext)
                        .toList();

        return new WorkoutSummaryContext.ExerciseContext(
                exercise.getExerciseName(),
                exercise.getOrderIndex(),
                sets
        );
    }

    private WorkoutSummaryContext.SetContext toSetContext(
            WorkoutSet set) {

        return new WorkoutSummaryContext.SetContext(
                set.getSetNumber(),
                set.getWeight(),
                set.getReps(),
                set.getRir(),
                set.getDurationSeconds(),
                set.getNotes()
        );
    }
}