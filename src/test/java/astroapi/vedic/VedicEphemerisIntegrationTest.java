package astroapi.vedic;

import astroapi.dto.EphiDatenDTO;
import astroapi.service.TimeZoneService;
import astroapi.utils.HoroscopeType;
import astroapi.utils.HouseSystem;
import astroapi.utils.SwissEphemerisHelper;
import astroapi.vedic.dto.ManglikResultDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicMarriageRequest;
import astroapi.vedic.dto.VedicMarriageResponseDTO;
import astroapi.vedic.dto.VedicPersonRequest;
import astroapi.vedic.dto.VedicPlanetDTO;
import astroapi.vedic.dto.VedicWarningDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import swisseph.SweConst;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Die Berechnung gegen die echte Ephemeride — und die eine Frage, die über diese Funktion
 * entscheidet: laufen tropische und siderische Rechnung ineinander?
 *
 * <p>Die Referenzwerte hier sind <b>nicht geraten</b>. Sie sind gegen die Definition der
 * Lahiri-Ayanamsha geprüft: Swiss Ephemeris legt sie über {@code t0 = JD 2415020.0} (1900) mit
 * {@code ayan_t0 = 22°27′37,70″} fest — das ist die Definition des indischen Rashtriya Panchang.
 * Daraus folgt für den 1. Januar 2000, 0:00 UT der angewandte Wert <b>23°51′11,4″</b>, und genau
 * das ist auch der publizierte Lahiri-Wert für diesen Zeitpunkt. Dass die Bibliothek ihn trifft,
 * prüft {@link #lahiriReferenceValue()}.
 */
class VedicEphemerisIntegrationTest {

    /**
     * Toleranz der Ayanamsha-Prüfung: 0,5 Bogensekunden. Eng genug, damit eine andere
     * Ayanamsha-Variante (die Varianten liegen Bogenminuten auseinander) sofort auffällt, und weit
     * genug, dass Rundung in der letzten Stelle nicht rot wird.
     */
    private static final double AYANAMSA_TOLERANCE_DEGREES = 0.5 / 3600.0;

    /** Toleranz für Positionsvergleiche: 1 Bogensekunde. */
    private static final double POSITION_TOLERANCE_DEGREES = 1.0 / 3600.0;

    private static VedicEphemerisService vedic;
    private static VedicMarriageService marriage;
    private static SwissEphemerisHelper tropical;
    private static TimeZoneService zones;

    @BeforeAll
    static void setup() {
        zones = new TimeZoneService();
        vedic = new VedicEphemerisService(zones, "ephe/", 2);
        marriage = new VedicMarriageService(vedic);
        tropical = new SwissEphemerisHelper();
    }

    @AfterAll
    static void tearDown() {
        vedic.shutdown();
        tropical.shutdown();
    }

    // ── Testdaten ────────────────────────────────────────────────────────────

    /** Wien, 17. Mai 1990, 14:30 Ortszeit. */
    private static VedicPersonRequest vienna() {
        return person("A", LocalDateTime.of(1990, 5, 17, 14, 30), 48.2082, 16.3738, true);
    }

    /** Mumbai, 3. Februar 1988, 6:15 Ortszeit. */
    private static VedicPersonRequest mumbai() {
        return person("B", LocalDateTime.of(1988, 2, 3, 6, 15), 19.0760, 72.8777, true);
    }

    private static VedicPersonRequest person(String label, LocalDateTime dt,
                                             double lat, double lon, boolean timeKnown) {
        VedicPersonRequest p = new VedicPersonRequest();
        p.setLabel(label);
        p.setDateTime(dt);
        p.setCoordinates(new double[]{lat, lon});
        p.setBirthTimeKnown(timeKnown);
        return p;
    }

    private static VedicPlanetDTO body(VedicChartDTO chart, VedicBody b) {
        return chart.getPlanets().stream().filter(p -> b.getKey().equals(p.getBody())).findFirst()
                .orElseThrow();
    }

    private static double angularDiff(double a, double b) {
        double d = Math.abs(((a - b) % 360 + 360) % 360);
        return Math.min(d, 360 - d);
    }

    // ── Lahiri-Referenz ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Lahiri am 1.1.2000 0:00 UT ist 23°51′11,4″ — der publizierte Wert")
    void lahiriReferenceValue() {
        // Greenwich, damit der Ort nichts beiträgt; 1.1.2000 0:00 UT ist dort auch Ortszeit.
        VedicPersonRequest greenwich = person("ref",
                LocalDateTime.of(2000, 1, 1, 0, 0), 51.4779, -0.0015, true);
        VedicChartDTO chart = vedic.chartOf(greenwich, Ayanamsa.LAHIRI, NodeType.MEAN);

        double expected = 23.0 + 51.0 / 60.0 + 11.4 / 3600.0;
        assertEquals(expected, chart.getAyanamsaDegrees(), AYANAMSA_TOLERANCE_DEGREES,
                "Angewandte Ayanamsha: " + chart.getAyanamsaDegreesFormatted());
        assertEquals("23°51′11″", chart.getAyanamsaDegreesFormatted());
    }

    @Test
    @DisplayName("Die Abfragefunktion liefert einen anderen Wert als die Rechnung — beide stehen drin")
    void apiAndAppliedAyanamsaBothReported() {
        VedicChartDTO chart = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        // swe_get_ayanamsa_ut enthält die Rahmenkorrektur nicht, die swe_calc anwendet. Der
        // Unterschied liegt im Bereich von Bogensekunden — klein, aber nicht null. Genau deshalb
        // wird nicht der Abfragewert als „verwendete Ayanamsha" ausgegeben.
        double delta = Math.abs(chart.getAyanamsaDegrees() - chart.getAyanamsaApiDegrees());
        assertTrue(delta > 0, "Die beiden Werte dürfen nicht stillschweigend gleichgesetzt werden");
        assertTrue(delta < 30.0 / 3600.0, "Unterschied unerwartet groß: " + (delta * 3600) + "″");
    }

    @Test
    @DisplayName("Ayanamsha wächst mit der Zeit — und zwar um etwa 50″ im Jahr")
    void ayanamsaGrowth() {
        double y1950 = vedic.chartOf(person("x", LocalDateTime.of(1950, 1, 1, 0, 0), 51.4779, -0.0015, true),
                Ayanamsa.LAHIRI, NodeType.MEAN).getAyanamsaDegrees();
        double y2000 = vedic.chartOf(person("x", LocalDateTime.of(2000, 1, 1, 0, 0), 51.4779, -0.0015, true),
                Ayanamsa.LAHIRI, NodeType.MEAN).getAyanamsaDegrees();
        double perYear = (y2000 - y1950) / 50.0 * 3600.0;
        assertTrue(y2000 > y1950, "Die Ayanamsha muss wachsen");
        assertEquals(50.3, perYear, 1.0, "Jahreszuwachs in Bogensekunden: " + perYear);
    }

    @Test
    @DisplayName("Verschiedene Ayanamshas ergeben verschiedene Positionen")
    void ayanamsaChoiceMatters() {
        VedicChartDTO lahiri = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        VedicChartDTO raman = vedic.chartOf(vienna(), Ayanamsa.RAMAN, NodeType.MEAN);
        double diff = Math.abs(lahiri.getAyanamsaDegrees() - raman.getAyanamsaDegrees());
        assertTrue(diff > 1.0 / 60.0,
                "Lahiri und Raman müssen sich messbar unterscheiden, Differenz: " + (diff * 60) + "′");
        assertNotEquals(lahiri.getAyanamsaDegrees(), raman.getAyanamsaDegrees());
    }

    // ── Siderisch gegen tropisch ─────────────────────────────────────────────

    @Test
    @DisplayName("Siderische Länge ist tropische Länge minus der angewandten Ayanamsha")
    void siderealIsTropicalMinusAyanamsa() {
        VedicChartDTO chart = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        double ayanamsa = chart.getAyanamsaDegrees();
        int[] sweBodies = {SweConst.SE_SUN, SweConst.SE_MOON, SweConst.SE_MARS, SweConst.SE_MERCURY,
                SweConst.SE_JUPITER, SweConst.SE_VENUS, SweConst.SE_SATURN};
        VedicBody[] bodies = {VedicBody.SUN, VedicBody.MOON, VedicBody.MARS, VedicBody.MERCURY,
                VedicBody.JUPITER, VedicBody.VENUS, VedicBody.SATURN};
        for (int i = 0; i < bodies.length; i++) {
            double trop = vedic.tropicalLongitudeForTest(chart.getJulianDayUT(), sweBodies[i]);
            double sid = body(chart, bodies[i]).getSiderealLongitude();
            assertEquals(0.0, angularDiff(VedicMath.normalizeDegrees(trop - ayanamsa), sid),
                    POSITION_TOLERANCE_DEGREES, bodies[i].getKey());
        }
    }

    @Test
    @DisplayName("Der Aszendent liegt im selben Tierkreis wie die Planeten")
    void ascendantUsesTheSameZodiac() {
        VedicChartDTO chart = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        EphiDatenDTO westernChart = tropical.createEphiDatenDTO(
                LocalDateTime.parse(chart.getUtDateTime()),
                chart.getCoordinates()[0], chart.getCoordinates()[1],
                HouseSystem.WHOLE_SIGN_HOUSES, HoroscopeType.NATAL);

        double expected = VedicMath.normalizeDegrees(
                westernChart.getAscendant() - chart.getAyanamsaDegrees());
        // Eine Bogenminute Toleranz: der Aszendent kommt aus swe_houses, die Ayanamsha aus der
        // Sonnendifferenz. Lägen sie in verschiedenen Tierkreisen, wären es gut 24 GRAD.
        assertEquals(0.0, angularDiff(expected, chart.getAscendantLongitude()), 1.0 / 60.0,
                "tropischer ASC " + westernChart.getAscendant()
                        + ", siderischer ASC " + chart.getAscendantLongitude());
    }

    @Test
    @DisplayName("Tropische und siderische Berechnung laufen parallel ohne sich zu verfälschen")
    void parallelTropicalAndSiderealDoNotContaminate() throws Exception {
        // Erst die Sollwerte in Ruhe holen.
        VedicChartDTO siderealReference = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        EphiDatenDTO tropicalReference = tropical.createEphiDatenDTO(
                LocalDateTime.parse(siderealReference.getUtDateTime()),
                48.2082, 16.3738, HouseSystem.PLACIDUS, HoroscopeType.NATAL);
        double sidSunReference = body(siderealReference, VedicBody.SUN).getSiderealLongitude();
        double tropSunReference = tropicalReference.getPlanets().get("Sun");
        // Die beiden müssen sich um die Ayanamsha unterscheiden — sonst prüft der Test nichts.
        assertTrue(angularDiff(sidSunReference, tropSunReference) > 20.0,
                "Siderisch und tropisch müssen sich um gut 24° unterscheiden");

        // Jetzt dasselbe durcheinander, mit Caches aus dem Weg geräumt.
        vedic.clearCache();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                final boolean siderealTurn = i % 2 == 0;
                final int seed = i;
                tasks.add(() -> {
                    if (siderealTurn) {
                        // Leicht wechselnde Zeiten, damit nicht alles aus dem Cache kommt.
                        VedicPersonRequest p = person("A",
                                LocalDateTime.of(1990, 5, 17, 14, 30).plusSeconds(seed),
                                48.2082, 16.3738, true);
                        VedicChartDTO c = vedic.chartOf(p, Ayanamsa.LAHIRI, NodeType.MEAN);
                        double sun = body(c, VedicBody.SUN).getSiderealLongitude();
                        // Die Sonne läuft knapp 1°/Tag; 40 Sekunden sind ein halbes Milligrad.
                        if (angularDiff(sun, sidSunReference) > 0.01) {
                            return "siderisch verfälscht: " + sun + " statt ~" + sidSunReference;
                        }
                        if (angularDiff(c.getAyanamsaDegrees(),
                                siderealReference.getAyanamsaDegrees()) > 0.001) {
                            return "Ayanamsha verfälscht: " + c.getAyanamsaDegrees();
                        }
                    } else {
                        EphiDatenDTO c = tropical.createEphiDatenDTO(
                                LocalDateTime.parse(siderealReference.getUtDateTime()).plusSeconds(seed),
                                48.2082, 16.3738, HouseSystem.PLACIDUS, HoroscopeType.NATAL);
                        double sun = c.getPlanets().get("Sun");
                        if (angularDiff(sun, tropSunReference) > 0.01) {
                            return "tropisch verfälscht: " + sun + " statt ~" + tropSunReference;
                        }
                    }
                    return null;
                });
            }
            List<Future<String>> results = pool.invokeAll(tasks, 120, TimeUnit.SECONDS);
            for (Future<String> f : results) {
                String problem = f.get();
                assertNull(problem, problem);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Eine siderische Berechnung hinterlässt keinen Modus in der tropischen Strecke")
    void siderealLeavesNoTraceInTheTropicalPath() {
        EphiDatenDTO before = tropical.createEphiDatenDTO(
                LocalDateTime.of(1990, 5, 17, 12, 30), 48.2082, 16.3738,
                HouseSystem.PLACIDUS, HoroscopeType.NATAL);
        for (Ayanamsa a : Ayanamsa.values()) {
            vedic.chartOf(vienna(), a, NodeType.MEAN);
            vedic.chartOf(mumbai(), a, NodeType.TRUE);
        }
        EphiDatenDTO after = tropical.createEphiDatenDTO(
                LocalDateTime.of(1990, 5, 17, 12, 30).plusSeconds(1), 48.2082, 16.3738,
                HouseSystem.PLACIDUS, HoroscopeType.NATAL);
        // Eine Sekunde später: die Sonne ist um 0,00001° weiter, nichts weiter.
        assertEquals(0.0, angularDiff(before.getPlanets().get("Sun"), after.getPlanets().get("Sun")),
                0.001, "Die tropische Sonne hat sich nach siderischen Berechnungen verschoben");
        assertEquals(0.0, angularDiff(before.getAscendant(), after.getAscendant()), 0.01,
                "Der tropische Aszendent hat sich verschoben");
    }

    // ── Cache ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Der Cache trennt nach Ayanamsha und Knotentyp")
    void cacheSeparatesByContract() {
        vedic.clearCache();
        VedicChartDTO lahiriMean = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        VedicChartDTO ramanMean = vedic.chartOf(vienna(), Ayanamsa.RAMAN, NodeType.MEAN);
        VedicChartDTO lahiriTrue = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.TRUE);

        assertNotEquals(lahiriMean.getAyanamsaDegrees(), ramanMean.getAyanamsaDegrees(),
                "Zweiter Aufruf mit anderer Ayanamsha darf nicht den ersten Eintrag liefern");
        assertNotEquals(
                body(lahiriMean, VedicBody.RAHU).getSiderealLongitude(),
                body(lahiriTrue, VedicBody.RAHU).getSiderealLongitude(),
                "Mittlerer und wahrer Knoten müssen sich unterscheiden");

        // Zweiter Aufruf derselben Kombination: identische Werte (Cache-Treffer, kein Drift).
        VedicChartDTO again = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        assertEquals(lahiriMean.getAyanamsaDegrees(), again.getAyanamsaDegrees());
        assertEquals(lahiriMean.getAscendantLongitude(), again.getAscendantLongitude());
    }

    @Test
    @DisplayName("Der Cache-Eintrag wird nicht mit dem Label der nächsten Person überschrieben")
    void cacheEntryIsNotMutatedByLabels() {
        vedic.clearCache();
        VedicPersonRequest first = person("Anna", LocalDateTime.of(1990, 5, 17, 14, 30),
                48.2082, 16.3738, true);
        VedicPersonRequest second = person("Ben", LocalDateTime.of(1990, 5, 17, 14, 30),
                48.2082, 16.3738, false);
        VedicChartDTO a = vedic.chartOf(first, Ayanamsa.LAHIRI, NodeType.MEAN);
        VedicChartDTO b = vedic.chartOf(second, Ayanamsa.LAHIRI, NodeType.MEAN);
        assertEquals("Anna", a.getLabel(), "Das erste Chart darf nicht nachträglich umbenannt werden");
        assertEquals("Ben", b.getLabel());
        assertTrue(a.isBirthTimeKnown());
        assertFalse(b.isBirthTimeKnown());
        // Gleiche Geburtsdaten, aber nur eine Anfrage kennt die Uhrzeit — das sind ZWEI
        // Cache-Einträge, weil das Flag das Ergebnis verändert. Bekäme Ben hier Annas Eintrag,
        // stünde bei ihm ein Aszendent, den seine Anfrage nicht hergibt.
        assertNotNull(a.getAscendantLongitude());
        assertNull(b.getAscendantLongitude(),
                "Ohne Geburtszeit darf kein Aszendent aus einem fremden Cache-Eintrag kommen");
        // Der Mond ist in beiden gleich — er hängt nicht an der Achse.
        assertEquals(body(a, VedicBody.MOON).getSiderealLongitude(),
                body(b, VedicBody.MOON).getSiderealLongitude());
    }

    // ── Unbekannte Geburtszeit ───────────────────────────────────────────────

    @Test
    @DisplayName("Ohne Geburtszeit fehlt die zeitabhängige Hälfte ganz — statt auf Mitternacht gerechnet")
    void withoutBirthTimeTheTimeDependentHalfIsAbsent() {
        VedicChartDTO chart = vedic.chartOf(
                person("A", LocalDateTime.of(1990, 5, 17, 12, 0), 48.2082, 16.3738, false),
                Ayanamsa.LAHIRI, NodeType.MEAN);

        assertFalse(chart.isTimeDependentAvailable());
        assertFalse(chart.isBirthTimeKnown());
        assertNull(chart.getAscendantLongitude());
        assertNull(chart.getAscendantDegreeInSign());
        assertNull(chart.getAscendantDegreeInSignFormatted());
        assertNull(chart.getAscendantRashiIndex());
        assertNull(chart.getAscendantRashi());
        assertNull(chart.getAscendantRashiLord());
        assertNull(chart.getAscendantNakshatra());
        assertNull(chart.getAscendantNavamsaRashiIndex());
        assertNull(chart.getAscendantNavamsaRashi());
        assertTrue(chart.getHouses().isEmpty(), "Whole-Sign-Häuser hängen am Aszendenten");
        for (VedicPlanetDTO planet : chart.getPlanets()) {
            assertNull(planet.getHouse(), planet.getBody() + " darf kein Haus haben");
        }
        for (VedicPlanetDTO planet : chart.getNavamsaPlanets()) {
            assertNull(planet.getHouse(), "D9 " + planet.getBody() + " darf kein Haus haben");
        }
    }

    @Test
    @DisplayName("Ohne Geburtszeit bleiben Mond, Nakshatra und alle Zeichen erhalten")
    void withoutBirthTimeTheMoonRemains() {
        VedicChartDTO chart = vedic.chartOf(
                person("A", LocalDateTime.of(1990, 5, 17, 12, 0), 48.2082, 16.3738, false),
                Ayanamsa.LAHIRI, NodeType.MEAN);

        // Das ganze Ashtakoota hängt am Mond. Ihn zu unterdrücken würde die Funktion abschalten,
        // statt sie ehrlich zu machen — die Unsicherheit steht als Warnung daneben.
        assertNotNull(chart.getMoonNakshatra());
        assertNotNull(chart.getMoonRashi());
        assertNotNull(chart.getVarna());
        assertNotNull(chart.getVashya());
        assertEquals(9, chart.getPlanets().size());
        for (VedicPlanetDTO planet : chart.getPlanets()) {
            assertNotNull(planet.getRashi(), planet.getBody());
            assertNotNull(planet.getNavamsaRashi(), planet.getBody());
            assertNotNull(planet.getNakshatra(), planet.getBody());
        }
    }

    @Test
    @DisplayName("Ohne Geburtszeit entfällt der Lagna-Bezug des Manglik und der Grad bleibt mild")
    void withoutBirthTimeManglikHasNoLagnaReference() {
        VedicChartDTO chart = vedic.chartOf(
                person("A", LocalDateTime.of(1990, 5, 17, 12, 0), 48.2082, 16.3738, false),
                Ayanamsa.LAHIRI, NodeType.MEAN);
        ManglikResultDTO manglik = ManglikCalculator.calculate(chart);

        assertFalse(manglik.isLagnaAvailable());
        assertEquals(2, manglik.getReferences().size(), "Nur Mond und Venus bleiben");
        assertTrue(manglik.getReferences().stream()
                .noneMatch(r -> "lagna".equals(r.getReference())));
        assertNotEquals("strong", manglik.getSeverity(),
                "Die starken Grade hängen am Lagna");
        assertNotEquals("moderate", manglik.getSeverity());
        // Die hausgebundenen Aufhebungen dürfen nicht greifen.
        for (String houseBound : List.of("mars_first_house_sign", "mars_second_house_sign",
                "mars_twelfth_house_sign")) {
            assertFalse(manglik.getCancellations().contains(houseBound),
                    houseBound + " braucht das Haus vom Lagna");
        }
    }

    @Test
    @DisplayName("Ohne Geburtszeit fehlen Aszendent, 7. Haus und dessen Herrscher in den Ehefaktoren")
    void withoutBirthTimeAxisFactorsAreAbsent() {
        VedicChartDTO chart = vedic.chartOf(
                person("A", LocalDateTime.of(1990, 5, 17, 12, 0), 48.2082, 16.3738, false),
                Ayanamsa.LAHIRI, NodeType.MEAN);
        var factors = MarriageFactors.build(chart).getFactors();

        List<String> keys = factors.stream().map(f -> f.getKey()).collect(Collectors.toList());
        assertEquals(List.of("venus", "jupiter", "mars", "moon"), keys,
                "Die drei achsengebundenen Faktoren müssen fehlen, nicht geraten sein");
        for (var factor : factors) {
            assertNull(factor.getHouse(), factor.getKey() + " darf kein Haus haben");
            assertNotNull(factor.getRashi(), factor.getKey());
            assertNotNull(factor.getNavamsaRashi(), factor.getKey());
        }
    }

    @Test
    @DisplayName("Mitternacht gilt nicht automatisch als unbekannte Zeit")
    void midnightIsAValidBirthTime() {
        // Wer um 0:00 geboren ist, hat eine Geburtszeit. Die erste Fassung hat Mitternacht als
        // „unbekannt" gelesen und damit genau diesen Menschen den Aszendenten weggenommen.
        VedicChartDTO chart = vedic.chartOf(
                person("A", LocalDateTime.of(1990, 5, 17, 0, 0), 48.2082, 16.3738, true),
                Ayanamsa.LAHIRI, NodeType.MEAN);
        assertTrue(chart.isTimeDependentAvailable());
        assertNotNull(chart.getAscendantRashi());
        assertEquals(12, chart.getHouses().size());
    }

    // ── Rahu und Ketu ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ketu steht exakt gegenüber Rahu, für beide Knotentypen")
    void ketuIsExactlyOppositeRahu() {
        for (NodeType type : NodeType.values()) {
            for (VedicPersonRequest p : List.of(vienna(), mumbai())) {
                VedicChartDTO chart = vedic.chartOf(p, Ayanamsa.LAHIRI, type);
                double rahu = body(chart, VedicBody.RAHU).getSiderealLongitude();
                double ketu = body(chart, VedicBody.KETU).getSiderealLongitude();
                assertEquals(180.0, angularDiff(rahu, ketu), 1e-9,
                        type + ": Rahu " + rahu + ", Ketu " + ketu);
                // Auch die Zeichen müssen gegenüberliegen.
                assertEquals(7, Rashi.countFrom(Rashi.byIndex(body(chart, VedicBody.RAHU).getRashiIndex()),
                        Rashi.byIndex(body(chart, VedicBody.KETU).getRashiIndex())), type.toString());
                assertTrue(body(chart, VedicBody.RAHU).isAlwaysRetrograde());
                assertTrue(body(chart, VedicBody.KETU).isAlwaysRetrograde());
            }
        }
    }

    @Test
    @DisplayName("Kein moderner Planet und kein Asteroid taucht in der vedischen Wertung auf")
    void onlyClassicalBodies() {
        VedicChartDTO chart = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        assertEquals(9, chart.getPlanets().size());
        assertEquals(9, chart.getNavamsaPlanets().size());
        List<String> keys = chart.getPlanets().stream().map(VedicPlanetDTO::getBody)
                .collect(Collectors.toList());
        assertEquals(List.of("sun", "moon", "mars", "mercury", "jupiter", "venus", "saturn",
                "rahu", "ketu"), keys);
        for (String forbidden : List.of("uranus", "neptune", "pluto", "chiron", "lilith",
                "ceres", "pallas", "juno", "vesta", "pholus")) {
            assertFalse(keys.contains(forbidden), forbidden + " gehört nicht in die vedische Wertung");
        }
    }

    // ── Chart-Struktur ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Whole-Sign-Häuser: zwölf Häuser, lückenlos ab dem Aszendentenzeichen")
    void wholeSignHouses() {
        VedicChartDTO chart = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        assertEquals(12, chart.getHouses().size());
        for (int i = 0; i < 12; i++) {
            var house = chart.getHouses().get(i);
            assertEquals(i + 1, house.getHouse());
            assertEquals(Rashi.byIndex(chart.getAscendantRashiIndex() + i).getKey(), house.getRashi());
            assertNotNull(house.getLord());
            assertNotNull(house.getBodies());
        }
        // Jeder Körper steht in genau einem Haus.
        long placed = chart.getHouses().stream().mapToLong(h -> h.getBodies().size()).sum();
        assertEquals(9, placed);
        // Und das Haus am Planeten stimmt mit dem Haus in der Hausliste überein.
        for (VedicPlanetDTO p : chart.getPlanets()) {
            assertTrue(chart.getHouses().get(p.getHouse() - 1).getBodies().contains(p.getBody()),
                    p.getBody() + " behauptet Haus " + p.getHouse());
        }
    }

    @Test
    @DisplayName("D1 und D9 sind getrennte Listen mit demselben Körpersatz")
    void d1AndD9AreSeparate() {
        VedicChartDTO chart = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        for (int i = 0; i < 9; i++) {
            VedicPlanetDTO d1 = chart.getPlanets().get(i);
            VedicPlanetDTO d9 = chart.getNavamsaPlanets().get(i);
            assertEquals(d1.getBody(), d9.getBody());
            // Das D9-Zeichen des D1-Eintrags ist das Zeichen des D9-Eintrags.
            assertEquals(d1.getNavamsaRashi(), d9.getRashi(), d1.getBody());
            assertEquals(Navamsa.signOf(d1.getSiderealLongitude()).getKey(), d9.getRashi(), d1.getBody());
        }
        assertNotNull(chart.getAscendantNavamsaRashi());
        assertEquals(Navamsa.signOf(chart.getAscendantLongitude()).getKey(),
                chart.getAscendantNavamsaRashi());
    }

    @Test
    @DisplayName("Das Mond-Nakshatra des Charts passt zur Mondlänge")
    void moonNakshatraMatchesMoonLongitude() {
        for (VedicPersonRequest p : List.of(vienna(), mumbai())) {
            VedicChartDTO chart = vedic.chartOf(p, Ayanamsa.LAHIRI, NodeType.MEAN);
            double moon = body(chart, VedicBody.MOON).getSiderealLongitude();
            assertEquals(Nakshatra.ofLongitude(moon).getIndex(), chart.getMoonNakshatra().getIndex());
            assertEquals(Rashi.ofLongitude(moon).getIndex(), chart.getMoonRashiIndex());
            assertEquals(Rashi.ofLongitude(moon).getVarna().getKey(), chart.getVarna());
        }
    }

    // ── Gesamtergebnis ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Die Antwort trägt den vollständigen Berechnungsvertrag")
    void responseCarriesTheContract() {
        VedicMarriageRequest request = new VedicMarriageRequest();
        request.setPersonA(vienna());
        request.setPersonB(mumbai());
        VedicMarriageResponseDTO r = marriage.analyse(request);

        assertEquals("vedic-sidereal", r.getCalculationSystem());
        assertEquals(VedicRuleSet.CALCULATION_VERSION, r.getCalculationVersion());
        assertEquals(VedicRuleSet.ASHTAKOOTA_RULE_SET, r.getAshtakootaRuleSet());
        assertEquals(VedicRuleSet.MANGLIK_RULE_SET, r.getManglikRuleSet());
        assertEquals("lahiri", r.getAyanamsa());
        assertEquals("mean", r.getNodeType());
        assertEquals("rahu_plus_180", r.getKetuRule());
        assertEquals("both", r.getTraditionalRole());
        assertNotNull(r.getContractKey());
        assertTrue(r.getContractKey().contains("lahiri"));
        assertTrue(r.getContractKey().contains("mean"));
        assertTrue(r.getContractKey().contains(VedicRuleSet.ASHTAKOOTA_RULE_SET));

        assertNotNull(r.getPersonA());
        assertNotNull(r.getPersonB());
        assertNotNull(r.getManglikA());
        assertNotNull(r.getManglikB());
        assertNotNull(r.getMarriageFactorsA());
        assertNotNull(r.getMarriageFactorsB());
        assertNotNull(r.getWarnings());
    }

    @Test
    @DisplayName("Ohne Rollenvorgabe werden beide Richtungen geliefert")
    void bothOrientationsByDefault() {
        VedicMarriageRequest request = new VedicMarriageRequest();
        request.setPersonA(vienna());
        request.setPersonB(mumbai());
        VedicMarriageResponseDTO r = marriage.analyse(request);
        assertEquals(2, r.getAshtakoota().size());
        assertEquals("a_as_bride", r.getAshtakoota().get(0).getRole());
        assertEquals("b_as_bride", r.getAshtakoota().get(1).getRole());
    }

    @Test
    @DisplayName("Mit Rollenvorgabe wird nur diese Richtung geliefert")
    void singleOrientationOnRequest() {
        VedicMarriageRequest request = new VedicMarriageRequest();
        request.setPersonA(vienna());
        request.setPersonB(mumbai());
        request.setTraditionalRole("b_as_bride");
        VedicMarriageResponseDTO r = marriage.analyse(request);
        assertEquals(1, r.getAshtakoota().size());
        assertEquals("b_as_bride", r.getAshtakoota().get(0).getRole());
        assertEquals("B", r.getAshtakoota().get(0).getBrideLabel());
        assertEquals("A", r.getAshtakoota().get(0).getGroomLabel());
    }

    @Test
    @DisplayName("Fehlende Geburtszeit erzeugt eine Warnung und wird nicht verschwiegen")
    void missingBirthTimeWarns() {
        VedicMarriageRequest request = new VedicMarriageRequest();
        request.setPersonA(person("A", LocalDateTime.of(1990, 5, 17, 12, 0), 48.2082, 16.3738, false));
        request.setPersonB(mumbai());
        VedicMarriageResponseDTO r = marriage.analyse(request);

        List<String> codes = r.getWarnings().stream().map(VedicWarningDTO::getCode)
                .collect(Collectors.toList());
        assertTrue(codes.contains("birth_time_unknown"), codes.toString());
        VedicWarningDTO w = r.getWarnings().stream()
                .filter(x -> "birth_time_unknown".equals(x.getCode())).findFirst().orElseThrow();
        assertEquals("A", w.getLabel());
        assertEquals("caution", w.getLevel());
        assertFalse(r.getPersonA().isBirthTimeKnown());
    }

    @Test
    @DisplayName("Ein grenznaher Mond wird gemeldet, samt der Zeitspanne, die ihn kippt")
    void nearBoundaryMoonWarns() {
        // Ein Datum suchen, an dem der Mond nahe einer Nakshatra-Grenze steht: der Mond läuft
        // 13,2°/Tag, also wird im Lauf eines Tages jede Grenze gestreift.
        LocalDateTime found = null;
        LocalDateTime cursor = LocalDateTime.of(1990, 1, 1, 0, 0);
        for (int i = 0; i < 24 * 60 && found == null; i += 7) {
            VedicPersonRequest p = person("A", cursor.plusMinutes(i), 48.2082, 16.3738, true);
            VedicChartDTO c = vedic.chartOf(p, Ayanamsa.LAHIRI, NodeType.MEAN);
            double distance = Math.min(c.getMoonNakshatra().getRemainingDegrees(),
                    c.getMoonNakshatra().getDegreeInNakshatra());
            if (distance <= 0.05) found = cursor.plusMinutes(i);
        }
        assertNotNull(found, "Innerhalb eines Tages muss der Mond eine Nakshatra-Grenze streifen");

        VedicMarriageRequest request = new VedicMarriageRequest();
        request.setPersonA(person("A", found, 48.2082, 16.3738, true));
        request.setPersonB(mumbai());
        VedicMarriageResponseDTO r = marriage.analyse(request);
        List<String> codes = r.getWarnings().stream().map(VedicWarningDTO::getCode)
                .collect(Collectors.toList());
        assertTrue(codes.contains("moon_near_nakshatra_boundary"), codes + " für " + found);
        VedicWarningDTO w = r.getWarnings().stream()
                .filter(x -> "moon_near_nakshatra_boundary".equals(x.getCode())).findFirst().orElseThrow();
        assertNotNull(w.getArgs().get("degrees"));
        assertNotNull(w.getArgs().get("approxMinutes"));
    }

    @Test
    @DisplayName("Die Manglik-Prüfung läuft gegen echte Charts durch und bleibt widerspruchsfrei")
    void manglikOnRealCharts() {
        for (VedicPersonRequest p : List.of(vienna(), mumbai())) {
            VedicChartDTO chart = vedic.chartOf(p, Ayanamsa.LAHIRI, NodeType.MEAN);
            ManglikResultDTO m = ManglikCalculator.calculate(chart);
            assertEquals(chart.getLabel(), m.getLabel());
            assertEquals(body(chart, VedicBody.MARS).getRashi(), m.getMarsRashi());
            assertEquals(body(chart, VedicBody.MARS).getHouse(),
                    m.getReferences().stream().filter(x -> "lagna".equals(x.getReference()))
                            .findFirst().orElseThrow().getMarsHouse(),
                    "Das Haus des Mars vom Lagna muss dem Haus im Chart entsprechen");
            if (!m.isManglik()) assertEquals("none", m.getSeverity());
            if (m.isManglik() && !m.getCancellations().isEmpty()) {
                assertFalse(m.isManglikAfterCancellation());
            }
        }
    }

    @Test
    @DisplayName("Die Ehefaktoren nennen Aszendent, 7. Haus, dessen Herrscher und die vier Grahas")
    void marriageFactors() {
        VedicChartDTO chart = vedic.chartOf(vienna(), Ayanamsa.LAHIRI, NodeType.MEAN);
        var factors = MarriageFactors.build(chart).getFactors();
        List<String> keys = factors.stream().map(f -> f.getKey()).collect(Collectors.toList());
        assertEquals(List.of("ascendant", "seventh_house", "seventh_lord",
                "venus", "jupiter", "mars", "moon"), keys);

        var seventh = factors.stream().filter(f -> "seventh_house".equals(f.getKey())).findFirst().orElseThrow();
        assertEquals(Rashi.byIndex(chart.getAscendantRashiIndex() + 6).getKey(), seventh.getRashi());
        assertEquals(7, seventh.getHouse());

        var lord = factors.stream().filter(f -> "seventh_lord".equals(f.getKey())).findFirst().orElseThrow();
        assertEquals(Rashi.byIndex(chart.getAscendantRashiIndex() + 6).getLord().getKey(), lord.getBody());
        assertNotNull(lord.getRashi());
        assertTrue(lord.getHouse() >= 1 && lord.getHouse() <= 12);
        assertNotNull(lord.getNavamsaRashi());
    }

    // ── Ungültige Anfragen ───────────────────────────────────────────────────

    @Test
    @DisplayName("Ungültige Koordinaten, fehlende Zeit und unbekannte Schlüssel werden abgewiesen")
    void invalidRequests() {
        assertThrows(IllegalArgumentException.class, () -> vedic.chartOf(
                person("x", LocalDateTime.now(), 95.0, 0.0, true), Ayanamsa.LAHIRI, NodeType.MEAN),
                "Breite über 90°");
        assertThrows(IllegalArgumentException.class, () -> vedic.chartOf(
                person("x", LocalDateTime.now(), 0.0, 200.0, true), Ayanamsa.LAHIRI, NodeType.MEAN),
                "Länge über 180°");
        VedicPersonRequest noCoords = new VedicPersonRequest();
        noCoords.setDateTime(LocalDateTime.now());
        assertThrows(IllegalArgumentException.class,
                () -> vedic.chartOf(noCoords, Ayanamsa.LAHIRI, NodeType.MEAN), "keine Koordinaten");
        VedicPersonRequest noTime = new VedicPersonRequest();
        noTime.setCoordinates(new double[]{48.2, 16.3});
        assertThrows(IllegalArgumentException.class,
                () -> vedic.chartOf(noTime, Ayanamsa.LAHIRI, NodeType.MEAN), "keine Zeit");

        assertThrows(IllegalArgumentException.class, () -> Ayanamsa.fromKey("sidereal-ish"));
        assertThrows(IllegalArgumentException.class, () -> NodeType.fromKey("wobbly"));
        assertThrows(IllegalArgumentException.class, () -> TraditionalRole.fromKey("whoever"));
        // Leere Angaben fallen auf die Vorgaben zurück, statt zu scheitern.
        assertEquals(Ayanamsa.LAHIRI, Ayanamsa.fromKey(null));
        assertEquals(NodeType.MEAN, NodeType.fromKey(""));
        assertEquals(TraditionalRole.BOTH, TraditionalRole.fromKey(null));
    }

    @Test
    @DisplayName("Ein Punkt im offenen Ozean liefert eine nautische Zone, keinen Abbruch")
    void oceanCoordinatesResolveToANauticalZone() {
        // Erwartet war zunächst ein Fehler — die Zeitzonenbibliothek deckt aber auch die offene
        // See ab und gibt dort eine reine Versatzzone zurück. Der Test hält jetzt das fest, was
        // tatsächlich passiert: die Berechnung läuft durch, und die aufgelöste Zone steht im
        // Ergebnis. Wichtig ist nur, dass gAIster und die Oberflächen keinen Sonderfall erfinden.
        VedicChartDTO chart = vedic.chartOf(
                person("x", LocalDateTime.of(1990, 1, 1, 12, 0), -40.0, -140.0, true),
                Ayanamsa.LAHIRI, NodeType.MEAN);
        assertNotNull(chart.getZoneId());
        assertFalse(chart.getZoneId().isBlank());
        assertEquals(9, chart.getPlanets().size());
        assertTrue(chart.getAyanamsaDegrees() > 23 && chart.getAyanamsaDegrees() < 25);
    }
}
