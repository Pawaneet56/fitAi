package com.pawaneet.fitai.ai.workout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawaneet.fitai.ai.dto.AiResponse;
import com.pawaneet.fitai.ai.dto.WorkoutSummaryResponse;
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
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public WorkoutSummaryResponse generateSummary(UUID workoutId) {

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

        AiResponse aiResponse = aiService.generate(
                promptBuilder.build(context)
        );

        try {
            return objectMapper.readValue(
                    aiResponse.content(),
                    WorkoutSummaryResponse.class
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to parse AI workout summary",
                    e
            );
        }
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