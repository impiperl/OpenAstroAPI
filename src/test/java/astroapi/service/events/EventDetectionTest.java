package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.EphemerisPort;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.SwissEphemerisHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft die Detektoren gegen die echte Ephemeride.
 *
 * <p>Bewusst <b>keine</b> auswendig hingeschriebenen Referenzdaten: geprüft wird jeweils die
 * definierende Eigenschaft am gefundenen Zeitpunkt. Ein Vollmond muss dort stehen, wo Sonne und
 * Mond 180° auseinander sind; eine Station dort, wo die Geschwindigkeit null ist; ein Ingress dort,
 * wo die Länge auf einer Zeichengrenze liegt. Das prüft die Rechnung selbst statt einer Tabelle,
 * die man falsch abtippen kann.
 */
class EventDetectionTest {

    private static SwissEphemerisHelper swe;

    /** Ein Monat mit Tagundnachtgleiche und einer vollständigen Lunation. */
    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = OffsetDateTime.of(2026, 3, 31, 23, 59, 0, 0, ZoneOffset.UTC);

    @BeforeAll
    static void setUp() {
        swe = new SwissEphemerisHelper();
    }

    @AfterAll
    static void tearDown() {
        swe.shutdown();
    }

    /** Führt einen Detektor über den Standardzeitraum aus. */
    private static List<AstroEventRawDTO> run(EventDetector detector) {
        return run(detector, FROM, TO);
    }

    private static List<AstroEventRawDTO> run(EventDetector detector, OffsetDateTime from, OffsetDateTime to) {
        return swe.withEphemeris(port -> detector.detect(
                new EventScanContext(port, JulianTime.toJulianDay(from), JulianTime.toJulianDay(to), null)), 120);
    }

    /** Wertet eine Größe an einem Ereigniszeitpunkt aus. */
    private static double at(AstroEventRawDTO event, BiFunction<EphemerisPort, Double, Double> f) {
        double jd = JulianTime.toJulianDay(event.getPeaksAt() != null ? event.getPeaksAt() : event.getStartsAt());
        return swe.withEphemeris(port -> f.apply(port, jd), 30);
    }

    // ── Mondphasen ────────────────────────────────────────────────────────────

    @Test
    void moonPhases_occurWhereTheElongationMatches() {
        List<AstroEventRawDTO> events = run(new MoonPhaseDetector());

        assertTrue(events.size() >= 4,
                "eine Lunation dauert 29,5 Tage — in 31 Tagen müssen alle vier Hauptphasen vorkommen, waren: "
                        + events.size());

        for (AstroEventRawDTO event : events) {
            double expected;
            switch (event.getKey()) {
                case "moon_phase_new":           expected = 0.0;   break;
                case "moon_phase_first_quarter": expected = 90.0;  break;
                case "moon_phase_full":          expected = 180.0; break;
                case "moon_phase_last_quarter":  expected = 270.0; break;
                default: throw new AssertionError("unbekannter Key: " + event.getKey());
            }

            double elongation = at(event, (port, jd) -> AngleMath.norm360(
                    port.lon(jd, Planet.MOON.getSweConstValue()) - port.lon(jd, Planet.SUN.getSweConstValue())));

            assertEquals(0.0, AngleMath.signedDelta(elongation, expected), 0.01,
                    event.getKey() + " bei " + event.getPeaksAt() + ": Elongation war " + elongation);
            assertTrue(event.isExact());
        }
    }

    @Test
    void moonPhases_appearInChronologicalOrderWithoutDuplicates() {
        List<AstroEventRawDTO> events = run(new MoonPhaseDetector());
        events.sort(java.util.Comparator.comparing(AstroEventRawDTO::getPeaksAt));

        for (int i = 1; i < events.size(); i++) {
            long minutes = java.time.Duration.between(
                    events.get(i - 1).getPeaksAt(), events.get(i).getPeaksAt()).toMinutes();
            // Zwischen zwei Hauptphasen liegen gut sieben Tage; alles darunter wäre ein Doppelfund.
            assertTrue(minutes > 6 * 24 * 60,
                    "zu dicht beieinander: " + events.get(i - 1).getKey() + " und " + events.get(i).getKey());
        }
    }

    // ── Sonnenpunkte ──────────────────────────────────────────────────────────

    @Test
    void marchEquinox_isFoundWhereTheSunCrossesZeroDegrees() {
        List<AstroEventRawDTO> events = run(new SolarPointDetector());

        assertEquals(1, events.size(), "im März gibt es genau einen Sonnenpunkt");
        AstroEventRawDTO equinox = events.get(0);
        assertEquals("equinox_march", equinox.getKey());

        double sunLongitude = at(equinox, (port, jd) -> port.lon(jd, Planet.SUN.getSweConstValue()));
        assertEquals(0.0, AngleMath.signedDelta(sunLongitude, 0.0), 0.01);

        // Die Tagundnachtgleiche liegt immer um den 20. März.
        assertEquals(3, equinox.getPeaksAt().getMonthValue());
        assertTrue(equinox.getPeaksAt().getDayOfMonth() >= 19 && equinox.getPeaksAt().getDayOfMonth() <= 21,
                "unerwarteter Tag: " + equinox.getPeaksAt());
    }

    // ── Ingress ───────────────────────────────────────────────────────────────

    @Test
    void ingresses_happenExactlyOnASignBoundary() {
        List<AstroEventRawDTO> events = run(new IngressDetector());
        assertFalse(events.isEmpty(), "der Mond allein wechselt in einem Monat rund zwölfmal das Zeichen");

        for (AstroEventRawDTO event : events) {
            String bodyName = event.getPlanets().get(0);
            Planet body = java.util.Arrays.stream(Planet.values())
                    .filter(p -> p.getName().equals(bodyName)).findFirst().orElseThrow();

            double longitude = at(event, (port, jd) -> port.lon(jd, body.getSweConstValue()));
            double degreeInSign = AngleMath.degreeInSign(longitude);
            double distanceToBoundary = Math.min(degreeInSign, 30.0 - degreeInSign);

            assertEquals(0.0, distanceToBoundary, 0.01,
                    event.getKey() + " bei " + event.getPeaksAt() + ": Länge war " + longitude);
        }
    }

    @Test
    void ingresses_doNotRepeatTheSolarPoints() {
        // Der Eintritt der Sonne in den Widder IST die Tagundnachtgleiche — er darf nicht doppelt kommen.
        List<AstroEventRawDTO> events = run(new IngressDetector());
        assertTrue(events.stream().noneMatch(e -> "sun_ingress_aries".equals(e.getKey())),
                "Sonne→Widder gehört dem SolarPointDetector");
    }

    // ── Stationen ─────────────────────────────────────────────────────────────

    @Test
    void stations_happenWhereTheSpeedIsZero() {
        // Ein Vierteljahr, damit sicher Stationen enthalten sind.
        List<AstroEventRawDTO> events = run(new StationDetector(),
                OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC));

        assertFalse(events.isEmpty(), "in einem Vierteljahr wendet mindestens ein Planet");

        for (AstroEventRawDTO event : events) {
            String bodyName = event.getPlanets().get(0);
            Planet body = java.util.Arrays.stream(Planet.values())
                    .filter(p -> p.getName().equals(bodyName)).findFirst().orElseThrow();

            double speed = at(event, (port, jd) -> port.lonSpeed(jd, body.getSweConstValue())[1]);
            assertEquals(0.0, speed, 0.001,
                    event.getKey() + " bei " + event.getPeaksAt() + ": Geschwindigkeit war " + speed);

            // Die Richtungsangabe muss zum tatsächlichen Vorher/Nachher passen.
            double jd = JulianTime.toJulianDay(event.getPeaksAt());
            double before = swe.withEphemeris(p -> p.lonSpeed(jd - 1.0, body.getSweConstValue())[1], 30);
            boolean turnsRetrograde = "RETROGRADE".equals(event.getPayload().get("direction"));
            assertEquals(turnsRetrograde, before > 0,
                    event.getKey() + ": Richtung passt nicht zur Geschwindigkeit davor (" + before + ")");
        }
    }

    // ── Finsternisse ──────────────────────────────────────────────────────────

    /**
     * Regressionsschutz für einen teuren Fallstrick: mit einer expliziten Typmaske suchte
     * {@code swe_sol_eclipse_when_glob} nicht die nächste Finsternis, sondern lief 33 Sekunden lang
     * bis an die obere Ephemeridengrenze und meldete dann einen Fehler — nach außen sichtbar als
     * „es gibt keine Finsternis". Genau das prüft dieser Test.
     */
    @Test
    void solarEclipse_isFoundInAWindowThatContainsOne() {
        List<AstroEventRawDTO> events = run(new EclipseDetector(),
                OffsetDateTime.of(2027, 2, 3, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2027, 2, 9, 0, 0, 0, 0, ZoneOffset.UTC));

        assertTrue(events.stream().anyMatch(e -> e.getKey().startsWith("eclipse_solar_")),
                "Anfang Februar 2027 liegt eine Sonnenfinsternis — gefunden wurde: " + keys(events));
    }

    @Test
    void lunarEclipse_isFoundInAWindowThatContainsOne() {
        List<AstroEventRawDTO> events = run(new EclipseDetector(),
                OffsetDateTime.of(2026, 8, 26, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 30, 0, 0, 0, 0, ZoneOffset.UTC));

        assertTrue(events.stream().anyMatch(e -> e.getKey().startsWith("eclipse_lunar_")),
                "Ende August 2026 liegt eine Mondfinsternis — gefunden wurde: " + keys(events));
    }

    @Test
    void eclipses_carryATypeAndAreNotReportedTwice() {
        List<AstroEventRawDTO> events = run(new EclipseDetector(),
                OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC));

        java.util.Set<OffsetDateTime> seen = new java.util.HashSet<>();
        for (AstroEventRawDTO event : events) {
            assertNotEquals("UNKNOWN", event.getPayload().get("eclipseType"),
                    "Finsternistyp konnte nicht bestimmt werden: " + event.getKey());
            assertTrue(seen.add(event.getPeaksAt()),
                    "derselbe Zeitpunkt wurde doppelt gemeldet: " + event.getPeaksAt());
        }
        assertTrue(events.size() >= 4, "ein Jahr enthält mindestens vier Finsternisse, waren: " + events.size());
    }

    private static String keys(List<AstroEventRawDTO> events) {
        return events.stream().map(AstroEventRawDTO::getKey).collect(java.util.stream.Collectors.joining(", "));
    }

    // ── Aspekte ───────────────────────────────────────────────────────────────

    @Test
    void aspects_areExactAtTheReportedTime() {
        List<AstroEventRawDTO> events = run(new AspectDetector());
        assertFalse(events.isEmpty());

        for (AstroEventRawDTO event : events) {
            double angle = ((Number) event.getPayload().get("angle")).doubleValue();
            Planet a = planet(event.getPlanets().get(0));
            Planet b = planet(event.getPlanets().get(1));

            double separation = at(event, (port, jd) -> AngleMath.separation(
                    port.lon(jd, a.getSweConstValue()), port.lon(jd, b.getSweConstValue())));

            assertEquals(angle, separation, 0.01,
                    event.getKey() + " bei " + event.getPeaksAt() + ": Abstand war " + separation);
        }
    }

    @Test
    void aspects_excludeTheMoon() {
        List<AstroEventRawDTO> events = run(new AspectDetector());
        assertTrue(events.stream().noneMatch(e -> e.getPlanets().contains(Planet.MOON.getName())),
                "Mondaspekte gehören in den persönlichen Feed, nicht in die kollektive Lage");
    }

    // ── Zeitraum und Determinismus ────────────────────────────────────────────

    @Test
    void sameRangeProducesIdenticalResults() {
        List<AstroEventRawDTO> first = run(new MoonPhaseDetector());
        List<AstroEventRawDTO> second = run(new MoonPhaseDetector());

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).getKey(), second.get(i).getKey());
            assertEquals(first.get(i).getPeaksAt(), second.get(i).getPeaksAt());
        }
    }

    @Test
    void identicalInstantsInDifferentOffsetsProduceTheSameEvents() {
        List<AstroEventRawDTO> utc = run(new MoonPhaseDetector(), FROM, TO);
        List<AstroEventRawDTO> vienna = run(new MoonPhaseDetector(),
                FROM.withOffsetSameInstant(ZoneOffset.ofHours(2)),
                TO.withOffsetSameInstant(ZoneOffset.ofHours(2)));

        assertEquals(utc.size(), vienna.size(), "der Zeitraum ist derselbe, nur anders geschrieben");
        for (int i = 0; i < utc.size(); i++) {
            assertEquals(utc.get(i).getKey(), vienna.get(i).getKey());
            assertEquals(utc.get(i).getPeaksAt().toInstant(), vienna.get(i).getPeaksAt().toInstant());
        }
    }

    @Test
    void eventsOutsideTheWindowAreNotReturned() {
        List<AstroEventRawDTO> full = run(new MoonPhaseDetector());
        assertTrue(full.size() >= 4);

        // Ein Detektor liefert nach Ereignisart gruppiert, nicht chronologisch — die zeitliche
        // Sortierung macht erst der AstroEventRangeService.
        full.sort(java.util.Comparator.comparing(AstroEventRawDTO::getPeaksAt));

        // Fenster so beschneiden, dass das früheste Ereignis knapp herausfällt.
        OffsetDateTime firstEvent = full.get(0).getPeaksAt();
        List<AstroEventRawDTO> clipped = run(new MoonPhaseDetector(), firstEvent.plusMinutes(1), TO);

        assertEquals(full.size() - 1, clipped.size());
        assertTrue(clipped.stream().noneMatch(e -> e.getPeaksAt().equals(firstEvent)));
    }

    private static Planet planet(String name) {
        return java.util.Arrays.stream(Planet.values())
                .filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }
}
