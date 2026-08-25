package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.dto.EphiDatenDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.SwissEphemerisHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Persönliche Transite und mehrgliedrige Figuren.
 *
 * <p>Das Radix ist hier bewusst frei gewählt und nicht aus einem echten Geburtsdatum gerechnet:
 * für die Transitmathematik zählt nur, dass die Radixlängen fest stehen. Ein synthetisches Radix
 * macht die Erwartungen nachvollziehbar, ohne dass jemand Geburtsdaten in den Test schreiben muss.
 */
class TransitAndConfigurationTest {

    private static SwissEphemerisHelper swe;

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = OffsetDateTime.of(2026, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeAll
    static void setUp() {
        swe = new SwissEphemerisHelper();
    }

    @AfterAll
    static void tearDown() {
        swe.shutdown();
    }

    private static EphiDatenDTO syntheticNatalChart() {
        EphiDatenDTO chart = new EphiDatenDTO();

        Map<String, Double> planets = new LinkedHashMap<>();
        planets.put("Sun", 15.0);
        planets.put("Moon", 100.0);
        planets.put("Venus", 200.0);
        planets.put("Mars", 310.0);
        chart.setPlanets(planets);

        chart.setAscendant(45.0);
        chart.setMediumCoeli(315.0);

        // Gleich große Häuser ab dem Aszendenten — für den Haustest genügt das.
        List<Double> cusps = new ArrayList<>();
        for (int i = 0; i < 12; i++) cusps.add(AngleMath.norm360(45.0 + i * 30.0));
        chart.setCusps(cusps);

        return chart;
    }

    private static List<AstroEventRawDTO> run(EventDetector detector, EphiDatenDTO natal) {
        return swe.withEphemeris(port -> detector.detect(new EventScanContext(
                port, JulianTime.toJulianDay(FROM), JulianTime.toJulianDay(TO), natal)), 180);
    }

    // ── Transite ──────────────────────────────────────────────────────────────

    @Test
    void withoutNatalChart_noTransitsAreProduced() {
        assertTrue(run(new TransitDetector(), null).isEmpty());
    }

    @Test
    void transits_areExactAgainstTheNatalPointAtTheReportedTime() {
        EphiDatenDTO natal = syntheticNatalChart();
        List<AstroEventRawDTO> events = run(new TransitDetector(), natal);
        assertFalse(events.isEmpty(), "in einem Monat trifft allein der Mond jeden Radixpunkt mehrfach");

        for (AstroEventRawDTO event : events) {
            double angle = ((Number) event.getPayload().get("angle")).doubleValue();
            double natalLongitude = ((Number) event.getPayload().get("natalLongitude")).doubleValue();
            String bodyName = (String) event.getPayload().get("transitingBody");
            Planet body = planet(bodyName);

            double jd = JulianTime.toJulianDay(event.getPeaksAt());
            double separation = swe.withEphemeris(
                    port -> AngleMath.separation(port.lon(jd, body.getSweConstValue()), natalLongitude), 30);

            assertEquals(angle, separation, 0.01,
                    event.getKey() + " bei " + event.getPeaksAt() + ": Abstand war " + separation);
        }
    }

    @Test
    void transits_carryTheAffectedNatalHouse() {
        List<AstroEventRawDTO> events = run(new TransitDetector(), syntheticNatalChart());

        for (AstroEventRawDTO event : events) {
            assertNotNull(event.getHouse(), "bei vorhandenen Häusern muss das Transithaus gesetzt sein");
            assertTrue(event.getHouse() >= 1 && event.getHouse() <= 12,
                    "Haus außerhalb 1–12: " + event.getHouse());
        }
    }

    @Test
    void transits_includeTheMoon() {
        List<AstroEventRawDTO> events = run(new TransitDetector(), syntheticNatalChart());
        assertTrue(events.stream().anyMatch(e -> Planet.MOON.getName().equals(e.getPayload().get("transitingBody"))),
                "der Mondtransit ist die tragende Tagesinformation im persönlichen Feed");
    }

    @Test
    void natalHouses_coverTheWholeCircleWithoutGaps() {
        NatalChart natal = new NatalChart(syntheticNatalChart());
        for (double longitude = 0; longitude < 360; longitude += 0.5) {
            Integer house = natal.houseOf(longitude);
            assertNotNull(house, "keine Zuordnung für " + longitude + "°");
            assertTrue(house >= 1 && house <= 12);
        }
    }

    // ── Figuren ───────────────────────────────────────────────────────────────

    @Test
    void configurations_spanAPeriodAndAreNotClaimedExact() {
        List<AstroEventRawDTO> events = run(new ConfigurationDetector(), null);

        for (AstroEventRawDTO event : events) {
            assertFalse(event.isExact(), "eine Figur ist ein Zustand, kein exakter Durchgang");
            assertNotNull(event.getStartsAt());
            assertNotNull(event.getEndsAt());
            assertFalse(event.getEndsAt().isBefore(event.getStartsAt()));
            assertTrue(event.getPlanets().size() >= 3);
        }
    }

    @Test
    void configurations_actuallyHoldAtTheirStartTime() {
        List<AstroEventRawDTO> events = run(new ConfigurationDetector(), null);

        for (AstroEventRawDTO event : events) {
            String figure = (String) event.getPayload().get("figure");
            double jd = JulianTime.toJulianDay(event.getStartsAt());

            List<Double> longitudes = new ArrayList<>();
            for (String name : event.getPlanets()) {
                longitudes.add(swe.withEphemeris(port -> port.lon(jd, planet(name).getSweConstValue()), 30));
            }

            if ("stellium".equals(figure)) {
                int sign = AngleMath.signIndex(longitudes.get(0));
                for (double longitude : longitudes) {
                    assertEquals(sign, AngleMath.signIndex(longitude),
                            "Stellium: alle Körper müssen im selben Zeichen stehen");
                }
            } else if ("grand_trine".equals(figure)) {
                for (int i = 0; i < longitudes.size(); i++) {
                    for (int j = i + 1; j < longitudes.size(); j++) {
                        assertEquals(120.0, AngleMath.separation(longitudes.get(i), longitudes.get(j)), 6.0,
                                "großes Trigon: alle Paare müssen im Trigon stehen");
                    }
                }
            }
            // T-Quadrat, Grand Cross und Yod haben eine Rollenverteilung (Apex), die sich aus der
            // Ereigniszeile allein nicht rekonstruieren lässt — hier genügt die Strukturprüfung oben.
        }
    }

    private static Planet planet(String name) {
        return Arrays.stream(Planet.values())
                .filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }
}
