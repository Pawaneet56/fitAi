package com.pawaneet.fitai.ai.workout;

import com.pawaneet.fitai.ai.dto.AiPrompt;
import org.springframework.stereotype.Component;

@Component
public class WorkoutSummaryPromptBuilder {

    public AiPrompt build(WorkoutSummaryContext context) {

        String systemPrompt = """
                You are FitAI, an intelligent fitness assistant.

                Analyze the user's completed workout and provide a concise,
                useful workout summary.

                Focus on:
                - Overall workout performance
                - Exercise volume and intensity
                - Notable sets
                - Potential observations or areas for improvement

                Do not invent information that is not present in the workout data.
                """;

        String userPrompt = buildWorkoutPrompt(context);

        return new AiPrompt(systemPrompt, userPrompt);
    }

    private String buildWorkoutPrompt(WorkoutSummaryContext context) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("Workout ID: ")
                .append(context.workoutId())
                .append("\n");

        prompt.append("Started At: ")
                .append(context.startedAt())
                .append("\n");

        prompt.append("Ended At: ")
                .append(context.endedAt())
                .append("\n");

        if (context.notes() != null) {
            prompt.append("Workout Notes: ")
                    .append(context.notes())
                    .append("\n");
        }

        prompt.append("\nExercises:\n");

        for (WorkoutSummaryContext.ExerciseContext exercise : context.exercises()) {

            prompt.append("\n")
                    .append(exercise.orderIndex())
                    .append(". ")
                    .append(exercise.exerciseName())
                    .append("\n");

            for (WorkoutSummaryContext.SetContext set : exercise.sets()) {

                prompt.append("   Set ")
                        .append(set.setNumber())
                        .append(": ")
                        .append(set.weight())
                        .append(" kg × ")
                        .append(set.reps())
                        .append(" reps");

                if (set.rir() != null) {
                    prompt.append(", RIR ")
                            .append(set.rir());
                }

                if (set.durationSeconds() != null) {
                    prompt.append(", duration ")
                            .append(set.durationSeconds())
                            .append(" seconds");
                }

                if (set.notes() != null) {
                    prompt.append(", notes: ")
                            .append(set.notes());
                }

                prompt.append("\n");
            }
        }

        return prompt.toString();
    }
}