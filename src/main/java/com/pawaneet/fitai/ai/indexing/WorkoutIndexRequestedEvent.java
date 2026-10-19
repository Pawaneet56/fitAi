package com.pawaneet.fitai.ai.indexing;

import java.util.UUID;

public record WorkoutIndexRequestedEvent(UUID workoutId, String userId) {}
