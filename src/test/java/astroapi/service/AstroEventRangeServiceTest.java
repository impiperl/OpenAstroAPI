package astroapi.service;

import astroapi.dto.AstroEventRawDTO;
import astroapi.dto.EventRangeRequest;
import astroapi.dto.EventRangeResponse;
import astroapi.service.events.EventDetector;
import astroapi.service.events.EventScanContext;
import astroapi.service.events.MoonPhaseDetector;
import astroapi.utils.ScanCancelledException;
import astroapi.utils.SwissEphemerisHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstroEventRangeServiceTest {

    private static SwissEphemerisHelper swe;

    @BeforeAll
    static void setUp() {
        swe = new SwissEphemerisHelper();
    }

    @AfterAll
    static void tearDown() {
        swe.shutdown();
    }

    private static EventRangeRequest request() {
        EventRangeRequest request = new EventRangeRequest();
        request.setFrom(OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        request.setTo(OffsetDateTime.of(2026, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC));
        return request;
    }

    /** Ein Detektor, der immer scheitert. */
    private static EventDetector brokenDetector() {
        return new EventDetector() {
            @Override public String name() { return "broken"; }
            @Override public List<AstroEventRawDTO> detect(EventScanContext context) {
                throw new IllegalStateException("kaputt");
            }
        };
    }

    // ── Unvollständige Ergebnisse ─────────────────────────────────────────────

    @Test
    void healthyRun_isNotMarkedPartial() {
        AstroEventRangeService service = new AstroEventRangeService(
                swe, Collections.singletonList(new MoonPhaseDetector()));

        EventRangeResponse response = service.detect(request());

        assertFalse(response.isPartial());
        assertTrue(response.getFailedDetectors().isEmpty());
        assertFalse(response.getEvents().isEmpty());
    }

    /**
     * Der wichtige Fall: „keine Finsternis gefunden" und „der Finsternis-Detektor ist abgestürzt"
     * dürfen für den Aufrufer nicht gleich aussehen. Ein Lauf, der das Ergebnis speichert, muss
     * einen solchen Zeitraum als fehlgeschlagen behandeln statt die Lücke festzuschreiben.
     */
    @Test
    void failingDetector_marksTheResponsePartialAndNamesIt() {
        AstroEventRangeService service = new AstroEventRangeService(
                swe, Arrays.asList(new MoonPhaseDetector(), brokenDetector()));

        EventRangeResponse response = service.detect(request());

        assertTrue(response.isPartial(), "ein ausgefallener Detektor muss sichtbar sein");
        assertEquals(Collections.singletonList("broken"), response.getFailedDetectors());
        // Die übrigen Ereignisse kommen trotzdem — für die Anzeige ist das brauchbar.
        assertFalse(response.getEvents().isEmpty());
    }

    /**
     * Ein Abbruch ist kein Detektorfehler: würde er wie einer behandelt, liefe der Scan nach dem
     * Timeout einfach mit dem nächsten Detektor weiter — genau das, was der Abbruch verhindern soll.
     */
    @Test
    void cancellation_isNotTreatedAsADetectorFailure() {
        EventDetector cancelling = new EventDetector() {
            @Override public String name() { return "cancelling"; }
            @Override public List<AstroEventRawDTO> detect(EventScanContext context) {
                throw new ScanCancelledException("Zeitbudget aufgebraucht");
            }
        };
        EventDetector mustNotRun = new EventDetector() {
            @Override public String name() { return "mustNotRun"; }
            @Override public List<AstroEventRawDTO> detect(EventScanContext context) {
                return fail("nach einem Abbruch darf kein weiterer Detektor mehr laufen");
            }
        };

        AstroEventRangeService service = new AstroEventRangeService(swe, Arrays.asList(cancelling, mustNotRun));
        assertThrows(RuntimeException.class, () -> service.detect(request()));
    }

    // ── Grenzen ───────────────────────────────────────────────────────────────

    @Test
    void rangeLongerThanTheLimitIsRejected() {
        EventRangeRequest request = request();
        request.setTo(request.getFrom().plusDays(AstroEventRangeService.MAX_RANGE_DAYS + 1));

        AstroEventRangeService service = new AstroEventRangeService(swe, Collections.emptyList());
        assertThrows(ResponseStatusException.class, () -> service.detect(request));
    }

    @Test
    void reversedRangeIsRejected() {
        EventRangeRequest request = request();
        request.setTo(request.getFrom().minusDays(1));

        AstroEventRangeService service = new AstroEventRangeService(swe, Collections.emptyList());
        assertThrows(ResponseStatusException.class, () -> service.detect(request));
    }

    @Test
    void eventsAreSortedChronologically() {
        AstroEventRangeService service = new AstroEventRangeService(swe, AstroEventRangeService.DEFAULT_DETECTORS);
        List<AstroEventRawDTO> events = service.detect(request()).getEvents();

        for (int i = 1; i < events.size(); i++) {
            assertFalse(events.get(i).getStartsAt().isBefore(events.get(i - 1).getStartsAt()),
                    "unsortiert an Position " + i);
        }
    }
}
