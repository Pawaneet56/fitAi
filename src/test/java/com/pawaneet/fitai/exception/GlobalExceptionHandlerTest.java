package com.pawaneet.fitai.exception;

import com.google.genai.errors.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test
    void exposesQuotaFailureWithoutProviderStackOrInternalDetails() {
        var response = new GlobalExceptionHandler().handleGeminiError(
                new ApiException(429, "RESOURCE_EXHAUSTED", "private provider details"),
                new MockHttpServletRequest("GET", "/api/workouts/test/summary"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().message()).contains("quota exceeded").doesNotContain("private provider details");
    }
}
