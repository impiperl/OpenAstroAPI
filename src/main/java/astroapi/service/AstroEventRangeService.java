package astroapi.service;

import astroapi.dto.AstroEventRawDTO;
import astroapi.dto.EventRangeRequest;
import astroapi.dto.EventRangeResponse;
import astroapi.service.events.AspectDetector;
import astroapi.service.events.ConfigurationDetector;
import astroapi.service.events.EclipseDetector;
import astroapi.service.events.EventDetector;
import astroapi.service.events.EventScanContext;
import astroapi.service.events.IngressDetector;
import astroapi.service.events.MoonPhaseDetector;
import astroapi.service.events.SolarPointDetector;
import astroapi.service.events.StationDetector;
import astroapi.service.events.TransitDetector;
import astroapi.utils.JulianTime;
import astroapi.utils.ScanCancelledException;
import astroapi.utils.SwissEphemerisHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Erkennt alle Ereignisse eines Zeitraums.
 *
 * <p>Liefert ausschließlich Fachdaten: Keys, Zeiten, beteiligte Körper. Keine Texte, keine
 * Bewertung, kein GPT — das Zusammenführen, Übersetzen und Ranken passiert im gAIster-Aggregator.
 */
@Service
public class AstroEventRangeService {

    private static final Logger log = LoggerFactory.getLogger(AstroEventRangeService.class);

    /**
     * Obergrenze des abfragbaren Zeitraums. gAIster begrenzt selbst schon auf 31 Tage; diese
     * Schranke schützt den Ephemeriden-Pool davor, dass ein einzelner Aufruf ihn minutenlang belegt.
     */
    public static final int MAX_RANGE_DAYS = 62;

    /**
     * Obergrenze für die Antwortgröße. Wird sie erreicht, ist etwas an der Anfrage unvernünftig —
     * besser abgeschnitten und protokolliert als eine Antwort, die niemand verarbeiten kann.
     */
    private static final int MAX_EVENTS = 5000;

    /** Zeitbudget für einen kompletten Scan. */
    private static final long SCAN_TIMEOUT_SECONDS = 60;

    static final List<EventDetector> DEFAULT_DETECTORS = List.of(
            new MoonPhaseDetector(),
            new EclipseDetector(),
            new StationDetector(),
            new IngressDetector(),
            new SolarPointDetector(),
            new AspectDetector(),
            new ConfigurationDetector(),
            new TransitDetector()
    );

    private final SwissEphemerisHelper swissEphemerisHelper;
    private final List<EventDetector> detectors;

    @Autowired
    public AstroEventRangeService(SwissEphemerisHelper swissEphemerisHelper) {
        this(swissEphemerisHelper, DEFAULT_DETECTORS);
    }

    /** Für Tests: erlaubt, das Verhalten bei einem fehlschlagenden Detektor zu prüfen. */
    AstroEventRangeService(SwissEphemerisHelper swissEphemerisHelper, List<EventDetector> detectors) {
        this.swissEphemerisHelper = swissEphemerisHelper;
        this.detectors = detectors;
    }

    public EventRangeResponse detect(EventRangeRequest request) {
        validate(request);

        double jdFrom = JulianTime.toJulianDay(request.getFrom());
        double jdTo = JulianTime.toJulianDay(request.getTo());
        boolean personal = request.getNatalChart() != null;

        List<String> failedDetectors = new ArrayList<>();

        long startedAt = System.currentTimeMillis();
        List<AstroEventRawDTO> events = swissEphemerisHelper.withEphemeris(port -> {
            EventScanContext context = new EventScanContext(port, jdFrom, jdTo, request.getNatalChart());
            List<AstroEventRawDTO> collected = new ArrayList<>();

            for (EventDetector detector : detectors) {
                try {
                    collected.addAll(detector.detect(context));
                } catch (ScanCancelledException e) {
                    // Ein Abbruch betrifft den ganzen Scan, nicht einen Ereignistyp: weiterzumachen
                    // hieße, nach dem Timeout weiterzurechnen — genau das, was der Abbruch verhindern
                    // soll.
                    throw e;
                } catch (RuntimeException e) {
                    // Ein defekter Detektor darf nicht den ganzen Feed verhindern — die Antwort wird
                    // aber als unvollständig gekennzeichnet, damit der Aufrufer entscheiden kann.
                    log.error("Event detector '{}' failed for range {} → {}: {}",
                            detector.name(), request.getFrom(), request.getTo(), e.getMessage(), e);
                    failedDetectors.add(detector.name());
                }
            }
            return collected;
        }, SCAN_TIMEOUT_SECONDS);

        events.sort(Comparator.comparing(AstroEventRawDTO::getStartsAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AstroEventRawDTO::getKey, Comparator.nullsLast(Comparator.naturalOrder())));

        if (events.size() > MAX_EVENTS) {
            log.warn("Event range {} → {} produced {} events, truncating to {}",
                    request.getFrom(), request.getTo(), events.size(), MAX_EVENTS);
            events = new ArrayList<>(events.subList(0, MAX_EVENTS));
        }

        log.info("Event range {} → {} ({}personal): {} events in {} ms{}",
                request.getFrom(), request.getTo(), personal ? "" : "not ", events.size(),
                System.currentTimeMillis() - startedAt,
                failedDetectors.isEmpty() ? "" : " — INCOMPLETE, failed: " + failedDetectors);

        EventRangeResponse response = new EventRangeResponse();
        response.setFrom(request.getFrom());
        response.setTo(request.getTo());
        response.setCalculatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        response.setPersonal(personal);
        response.setPartial(!failedDetectors.isEmpty());
        response.setFailedDetectors(failedDetectors);
        response.setEvents(events);
        return response;
    }

    private void validate(EventRangeRequest request) {
        if (!request.getTo().isAfter(request.getFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be after from");
        }
        long days = Duration.between(request.getFrom(), request.getTo()).toDays();
        if (days > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "range must not exceed " + MAX_RANGE_DAYS + " days");
        }
    }
}
