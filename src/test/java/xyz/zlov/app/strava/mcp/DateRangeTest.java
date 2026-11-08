package xyz.zlov.app.strava.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeTest {

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow"); // UTC+3

    @Test
    void localDatesAreInclusiveAtBothEnds() {
        DateRange r = DateRange.of("2026-10-27", "2026-10-31", MSK);

        assertEquals(Instant.parse("2026-10-26T21:00:00Z"), r.from());
        assertEquals(Instant.parse("2026-10-31T21:00:00Z"), r.to()); // exclusive: start of 1 Nov local
    }

    @Test
    void missingBoundsAreOpen() {
        DateRange r = DateRange.of(null, " ", MSK);

        assertEquals(Instant.EPOCH, r.from());
        assertTrue(r.to().isAfter(Instant.parse("2999-01-01T00:00:00Z")));
    }

    @Test
    void invalidInputIsRejectedWithClearMessage() {
        var e = assertThrows(IllegalArgumentException.class, () -> DateRange.of("31.10.2026", null, MSK));
        assertTrue(e.getMessage().contains("from"));
        assertThrows(IllegalArgumentException.class, () -> DateRange.of("2026-11-02", "2026-11-01", MSK));
    }
}
