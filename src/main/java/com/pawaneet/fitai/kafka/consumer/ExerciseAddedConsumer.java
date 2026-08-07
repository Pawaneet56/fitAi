package com.pawaneet.fitai.kafka.consumer;

import com.pawaneet.fitai.analytics.service.AnalyticsService;
import com.pawaneet.fitai.workout.event.ExerciseAddedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExerciseAddedConsumer {

    private final AnalyticsService analyticsService;

    public ExerciseAddedConsumer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(topics = "${app.kafka.topics.exercise-added}", groupId = "analytics-group")
    public void handleExerciseAdded(ExerciseAddedEvent event) {
        log.info("Received ExerciseAddedEvent: {}", event);
        analyticsService.handleExerciseAdded(event);
    }
}
