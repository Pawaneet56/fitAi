package com.pawaneet.fitai.workout.consumer;

import com.pawaneet.fitai.analytics.service.AnalyticsService;
import com.pawaneet.fitai.workout.event.WorkoutEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutEndedConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(
            topics = "${app.kafka.topics.workout-ended}",
            groupId = "fitai-workout-group"
    )
    public void consume(WorkoutEndedEvent event) {
        log.info("Workout Ended Event received: {}", event);
        analyticsService.handleWorkoutEnded(event);
    }
}
