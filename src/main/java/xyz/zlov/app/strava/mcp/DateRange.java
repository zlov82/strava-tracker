package xyz.zlov.app.strava.mcp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/** Turns local dates (yyyy-MM-dd, both ends inclusive) into a half-open [from, to) Instant range. */
record DateRange(Instant from, Instant to) {

    private static final Instant MIN = Instant.EPOCH;
    private static final Instant MAX = Instant.parse("3000-01-01T00:00:00Z");

    static DateRange of(String from, String to, ZoneId zone) {
        LocalDate f = parse("from", from);
        LocalDate t = parse("to", to);
        if (f != null && t != null && t.isBefore(f)) {
            throw new IllegalArgumentException("'to' (" + t + ") is before 'from' (" + f + ")");
        }
        return new DateRange(
                f == null ? MIN : f.atStartOfDay(zone).toInstant(),
                t == null ? MAX : t.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private static LocalDate parse(String name, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("'" + name + "' must be a date like 2026-10-31, got: " + value);
        }
    }
}
