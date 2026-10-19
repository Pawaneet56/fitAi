package com.pawaneet.fitai.ai.indexing;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkoutIndexConsumerTest {
    @Test
    void rejectsUnrecognizedUserWithoutIndexing() {
        WorkoutIndexingService indexing = mock(WorkoutIndexingService.class);
        var consumer = new WorkoutIndexConsumer(indexing);
        assertThatThrownBy(() -> consumer.consume(new WorkoutIndexRequestedEvent(UUID.randomUUID(), "user2")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(indexing);
    }
}
