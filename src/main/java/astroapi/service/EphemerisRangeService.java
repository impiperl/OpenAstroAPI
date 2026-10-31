package astroapi.service;

import astroapi.dto.EphemerisRangeRequest;
import astroapi.dto.EphemerisRangeResponse;
import astroapi.dto.EphemerisSampleDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.Bodies;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.SwissEphemerisHelper;
import astroapi.utils.ZodiacSign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Die Ephemeride eines Zeitraums: jeder Körper an festen Stützstellen.
 *
 * <p>Zweck ist der Jahresvorlauf. Aus einer Tagesephemeride lassen sich beim Aufrufer sowohl die
 * Hausdurchgänge als auch die Transite zu einem Radix ableiten — ohne dass das Radix die AstroAPI
 * je erreicht und ohne einen Scan pro Person. Die Ereigniserkennung
 * ({@link AstroEventRangeService}) bleibt davon unberührt: die liefert exakte Durchgänge, diese
 * hier liefert Positionen.
 *
 * <p>Deshalb auch kein Detektor und kein Crossing-Scan: pro Stützstelle genau ein
 * {@code swe_calc_ut} je Körper. Ein Jahr mit Tagesraster und 19 Körpern sind rund 7.000 Aufrufe
 * und damit Sekunden, nicht Minuten.
 */
@Service
public class EphemerisRangeService {

    private static final Logger log = LoggerFactory.getLogger(EphemerisRangeService.class);

    /**
     * Obergrenze des Zeitraums. Ein Jahresvorlauf braucht 366 Tage; der Puffer erlaubt den
     * üblichen Überhang von ein paar Tagen an beiden Rändern, damit beim Aufrufer interpoliert
     * werden kann, ohne dass die Ränder des Jahres unbestimmt bleiben.
     */
    public static final int MAX_RANGE_DAYS = 400;

    /**
     * Obergrenze der Antwortgröße. Erreicht sie der Aufruf, ist die Anfrage unvernünftig
     * geschnitten — besser ein klarer 400er als eine Antwort, die niemand mehr verarbeitet.
     */
    private static final int MAX_SAMPLES = 60_000;

    private static final int MIN_STEP_HOURS = 1;
    private static final int MAX_STEP_HOURS = 24 * 7;

    /** Zeitbudget für einen kompletten Lauf. */
    private static final long CALC_TIMEOUT_SECONDS = 60;

    private final SwissEphemerisHelper swissEphemerisHelper;

    @Value("${astro.bodies.extended:true}")
    private boolean extendedBodies = true;

    @Autowired
    public EphemerisRangeService(SwissEphemerisHelper swissEphemerisHelper) {
        this.swissEphemerisHelper = swissEphemerisHelper;
    }

    public EphemerisRangeResponse calculate(EphemerisRangeRequest request) {
        int stepHours = request.getStepHours() != null ? request.getStepHours() : 24;
        validate(request, stepHours);

        List<Planet> bodies = resolveBodies(request.getBodies());
        List<OffsetDateTime> stamps = stampsOf(request.getFrom(), request.getTo(), stepHours);

        long expected = (long) stamps.size() * bodies.size();
        if (expected > MAX_SAMPLES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "request would produce " + expected + " samples, limit is " + MAX_SAMPLES);
        }

        long startedAt = System.currentTimeMillis();
        int[] missing = new int[1];

        List<EphemerisSampleDTO> samples = swissEphemerisHelper.withEphemeris(port -> {
            List<EphemerisSampleDTO> collected = new ArrayList<>((int) expected);
            for (OffsetDateTime stamp : stamps) {
                double jd = JulianTime.toJulianDay(stamp);
                for (Planet body : bodies) {
                    double[] state = port.lonSpeed(jd, body.getSweConstValue());
                    if (Double.isNaN(state[0])) {
                        // Eine fehlende Stützstelle wird nicht als 0° abgelegt — das wäre ein
                        // Körper auf dem Widderpunkt und damit ein falscher Transit.
                        missing[0]++;
                        continue;
                    }
                    double longitude = AngleMath.norm360(state[0]);
                    collected.add(new EphemerisSampleDTO(stamp, body.getName(), round(longitude),
                            round(state[1]), ZodiacSign.fromDegree(longitude).getName(),
                            round(AngleMath.degreeInSign(longitude))));
                }
            }
            return collected;
        }, CALC_TIMEOUT_SECONDS);

        log.info("Ephemeris range {} → {} step {}h: {} sample(s) for {} bodies in {} ms{}",
                request.getFrom(), request.getTo(), stepHours, samples.size(), bodies.size(),
                System.currentTimeMillis() - startedAt,
                missing[0] == 0 ? "" : " — INCOMPLETE, " + missing[0] + " missing");

        EphemerisRangeResponse response = new EphemerisRangeResponse();
        response.setFrom(request.getFrom());
        response.setTo(request.getTo());
        response.setStepHours(stepHours);
        response.setCalculatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        response.setPartial(missing[0] > 0);
        response.setMissingSamples(missing[0]);
        response.setSamples(samples);
        return response;
    }

    /**
     * Die Stützstellen von {@code from} an, {@code to} eingeschlossen.
     *
     * <p>Gerechnet wird additiv auf dem Startzeitpunkt, nicht fortlaufend auf dem Vorgänger: so
     * liegt bei 24 h Schritt jede Stelle auf derselben Uhrzeit, auch über eine Zeitumstellung
     * hinweg — der Offset von {@code from} gilt für alle.
     */
    private List<OffsetDateTime> stampsOf(OffsetDateTime from, OffsetDateTime to, int stepHours) {
        List<OffsetDateTime> stamps = new ArrayList<>();
        for (long index = 0; ; index++) {
            OffsetDateTime stamp = from.plusHours(index * stepHours);
            if (stamp.isAfter(to)) break;
            stamps.add(stamp);
        }
        return stamps;
    }

    /**
     * Namen auf Körper abbilden.
     *
     * <p>Ein unbekannter Name ist ein 400 und kein stilles Überspringen: der Aufrufer legt das
     * Ergebnis als vollständiges Jahr ab, und eine fehlende Reihe fiele dort erst auf, wenn die
     * Deutung sie braucht.
     */
    private List<Planet> resolveBodies(List<String> requested) {
        List<Planet> available = Bodies.forChart(extendedBodies);
        if (requested == null || requested.isEmpty()) return available;

        Map<String, Planet> byName = new LinkedHashMap<>();
        for (Planet body : available) {
            byName.put(body.getName().toLowerCase(Locale.ROOT), body);
        }

        List<Planet> resolved = new ArrayList<>();
        for (String name : requested) {
            Planet body = name != null ? byName.get(name.trim().toLowerCase(Locale.ROOT)) : null;
            if (body == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown body: " + name);
            }
            if (!resolved.contains(body)) resolved.add(body);
        }
        return resolved;
    }

    private void validate(EphemerisRangeRequest request, int stepHours) {
        if (!request.getTo().isAfter(request.getFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be after from");
        }
        long days = Duration.between(request.getFrom(), request.getTo()).toDays();
        if (days > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        if (stepHours < MIN_STEP_HOURS || stepHours > MAX_STEP_HOURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "stepHours must be between " + MIN_STEP_HOURS + " and " + MAX_STEP_HOURS);
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
