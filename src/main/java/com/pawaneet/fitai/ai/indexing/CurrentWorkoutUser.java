package com.pawaneet.fitai.ai.indexing;

/** Temporary single-user identity. Replace with authenticated ownership before multi-user use. */
public final class CurrentWorkoutUser {
    public static final String ID = "user1";
    public static String documentDate(java.time.Instant instant) {
        return new java.time.format.DateTimeFormatterBuilder().appendInstant(9).toFormatter().format(instant);
    }
    private CurrentWorkoutUser() {}
}
