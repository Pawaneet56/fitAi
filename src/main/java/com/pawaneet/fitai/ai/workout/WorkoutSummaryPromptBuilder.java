package com.pawaneet.fitai.ai.workout;

import com.pawaneet.fitai.ai.dto.AiPrompt;
import org.springframework.stereotype.Component;

@Component
public class WorkoutSummaryPromptBuilder {

    public AiPrompt build(WorkoutSummaryContext context) {

        String systemPrompt = """
        You are a fitness workout analysis assistant.

        Analyze the provided completed workout data and generate a concise,
        factual workout summary.

        Your response MUST be valid JSON matching exactly this structure:

        {
          "summary": "string",
          "observations": [
            "string"
          ],
          "suggestions": [
            "string"
          ]
        }

        Rules:
        - Return ONLY the JSON object.
        - Do not include Markdown.
        - Do not use ```json code fences.
        - Do not include any text before or after the JSON.
        - Use only information present in the workout data.
        - Do not invent missing workout data.
        - If information is unavailable, explicitly mention that in the
          observation or suggestion.
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