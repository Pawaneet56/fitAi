package com.pawaneet.fitai.ai.workout;

import com.google.genai.types.Schema;
import com.pawaneet.fitai.ai.dto.AiPrompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WorkoutSummaryPromptBuilder {

    public AiPrompt build(WorkoutSummaryContext context) {

        String systemPrompt = """
                You are a fitness workout analysis assistant.

                Analyze the provided completed workout data and generate a concise,
                factual workout summary.

                Rules:
                - Use only information present in the workout data.
                - Do not invent missing workout data.
                - If information is unavailable, explicitly mention that in the
                  observation or suggestion.
                - Keep observations specific to the provided workout.
                - Suggestions should be practical and relevant to the workout.
                """;

        String userPrompt = """
                Generate a summary for the following completed workout:

                %s
                """.formatted(context);

        return new AiPrompt(
                systemPrompt,
                userPrompt,
                workoutSummarySchema()
        );
    }

    private Schema workoutSummarySchema() {

        return Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "summary",
                        Schema.builder()
                                .type("STRING")
                                .build(),

                        "observations",
                        Schema.builder()
                                .type("ARRAY")
                                .items(
                                        Schema.builder()
                                                .type("STRING")
                                                .build()
                                )
                                .build(),

                        "suggestions",
                        Schema.builder()
                                .type("ARRAY")
                                .items(
                                        Schema.builder()
                                                .type("STRING")
                                                .build()
                                )
                                .build()
                ))
                .required(List.of(
                        "summary",
                        "observations",
                        "suggestions"
                ))
                .build();
    }
}