package com.pawaneet.fitai.analytics.service;

import com.pawaneet.fitai.workout.event.WorkoutEndedEvent;
import com.pawaneet.fitai.workout.event.WorkoutStartedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AnalyticsService {

    public void handleWorkoutStarted(WorkoutStartedEvent event) {

        log.info(
                "Analytics received workout {} started at {}",
                event.workoutId(),
                event.startedAt()
        );
    }

    public void handleWorkoutEnded(WorkoutEndedEvent event) {

        log.info(
                "Analytics received workout {} ended at {} after {} seconds",
                event.workoutId(),
                event.endedAt(),
                event.durationSeconds()
        );
    }
}
