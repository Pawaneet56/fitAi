package com.pawaneet.fitai.workout.consumer;

import com.pawaneet.fitai.analytics.service.AnalyticsService;
import com.pawaneet.fitai.workout.event.WorkoutStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutStartedConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(
            topics = "${app.kafka.topics.workout-started}",
            groupId = "fitai-workout-group"
    )
    public void consume(WorkoutStartedEvent event) {
        log.info("Workout Started Event received: {}", event);
        analyticsService.handleWorkoutStarted(event);
    }
}