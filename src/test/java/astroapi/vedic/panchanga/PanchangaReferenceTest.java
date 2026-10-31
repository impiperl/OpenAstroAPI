package astroapi.vedic.panchanga;

import astroapi.service.TimeZoneService;
import astroapi.vedic.Ayanamsa;
import astroapi.vedic.VedicEphemerisService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feste Werte aus einer <b>fremden</b> Quelle.
 *
 * <h2>Warum es diese Klasse geben muss</h2>
 * Die übrigen Panchanga-Tests prüfen innere Stimmigkeit: dass nach einer Wechselzeit das nächste
 * Glied gilt, dass Karana und Tithi zusammenpassen, dass nichts aus den Tabellen fällt. Das ist
 * nützlich und war trotzdem nicht genug — eine <b>falsche Sonnenaufgangs-Konvention blieb damit
 * grün</b>, weil sie in sich vollkommen widerspruchsfrei war. Gegen einen falschen Nullpunkt hilft
 * nur ein Wert, der nicht aus dem eigenen Code stammt.
 *
 * <h2>Die Quelle</h2>
 * United States Naval Observatory, Astronomical Applications Department
 * ({@code aa.usno.navy.mil}), abgerufen am 04.10.2026. Die Zeiten sind dort auf die Minute
 * gerundet; die Toleranzen hier tragen das mit.
 */
class PanchangaReferenceTest {

    private static TimeZoneService zones;
    private static VedicEphemerisService vedic;
    private static PanchangaService panchanga;

    private static final double CHENNAI_LAT = 13.0827;
    private static final double CHENNAI_LON = 80.2707;

    /** USNO: Neumond am 10. Oktober 2026 um 15:50 UTC. */
    private static final Instant NEW_MOON_OCT = Instant.parse("2026-10-10T15:50:00Z");

    /** USNO: Vollmond am 26. Oktober 2026 um 04:12 UTC. */
    private static final Instant FULL_MOON_OCT = Instant.parse("2026-10-26T04:12:00Z");

    /** USNO: Neumond am 9. November 2026 um 07:02 UTC. */
    private static final Instant NEW_MOON_NOV = Instant.parse("2026-11-09T07:02:00Z");

    /**
     * USNO: bürgerlicher Sonnenaufgang in Chennai am 4. Oktober 2026 um 00:28 UTC (05:58 Ortszeit).
     *
     * <p><b>Bürgerlich</b> heißt: oberer Sonnenrand, mit Refraktion. Das ist <em>nicht</em> die
     * hinduistische Konvention — und genau deshalb ist dieser Wert hier so wertvoll.
     */
    private static final Instant CHENNAI_CIVIL_SUNRISE = Instant.parse("2026-10-04T00:28:00Z");

    @BeforeAll
    static void setup() {
        zones = new TimeZoneService();
        vedic = new VedicEphemerisService(zones, "ephe/", 2);
        panchanga = new PanchangaService(vedic, zones);
    }

    @AfterAll
    static void tearDown() {
        vedic.shutdown();
    }

    // ── Neumond und Vollmond ────────────────────────────────────────────────

    /**
     * Zum Neumond stehen Sonne und Mond auf derselben Länge: die Elongation ist 0, und damit
     * beginnt die erste Tithi. Das prüft die ganze Kette — Zeitumrechnung, Julianischer Tag,
     * beide Ephemeridenabfragen — gegen einen Wert, der nicht von hier stammt.
     *
     * <p>Toleranz: Der Mond entfernt sich mit rund 12,19° am Tag von der Sonne, also 0,0085° je
     * Minute. Die USNO-Angabe ist minutengenau; 0,05° entsprechen knapp sechs Minuten und sind
     * damit eng genug, dass eine falsche Zeitbasis sofort auffällt.
     */
    @Test
    @DisplayName("Zum Neumond der USNO ist die Elongation null")
    void atTheUsnoNewMoonTheElongationIsZero() {
        for (Instant newMoon : new Instant[]{NEW_MOON_OCT, NEW_MOON_NOV}) {
            double[] sunMoon = sunAndMoonAt(newMoon);
            double elongation = PanchangaCalculator.elongation(sunMoon[0], sunMoon[1]);
            double offset = Math.min(elongation, 360.0 - elongation);
            assertTrue(offset < 0.05,
                    "Elongation zum Neumond " + newMoon + " war " + elongation + "°");
        }
    }

    @Test
    @DisplayName("Zum Vollmond der USNO beträgt die Elongation 180 Grad")
    void atTheUsnoFullMoonTheElongationIsOneEighty() {
        double[] sunMoon = sunAndMoonAt(FULL_MOON_OCT);
        double elongation = PanchangaCalculator.elongation(sunMoon[0], sunMoon[1]);
        assertEquals(180.0, elongation, 0.05, "Elongation zum Vollmond");
    }

    /**
     * Und dieselbe Probe über die Tithi: Unmittelbar nach dem Neumond läuft die erste, unmittelbar
     * nach dem Vollmond die sechzehnte. Zehn Minuten Abstand halten sicheren Abstand zur Grenze,
     * ohne das nächste Glied zu erreichen.
     */
    @Test
    @DisplayName("Kurz nach Neumond läuft Tithi 1, kurz nach Vollmond Tithi 16")
    void theTithiFollowsTheLunation() {
        Panchanga afterNew = panchanga.at(CHENNAI_LAT, CHENNAI_LON,
                NEW_MOON_OCT.plusSeconds(600)).limbs();
        assertEquals(1, afterNew.tithiNumber());
        assertEquals(Paksha.SHUKLA, afterNew.paksha());

        Panchanga afterFull = panchanga.at(CHENNAI_LAT, CHENNAI_LON,
                FULL_MOON_OCT.plusSeconds(600)).limbs();
        assertEquals(16, afterFull.tithiNumber());
        assertEquals(Paksha.KRISHNA, afterFull.paksha());

        // Und davor jeweils das letzte Glied des alten Zyklus.
        assertEquals(30, panchanga.at(CHENNAI_LAT, CHENNAI_LON,
                NEW_MOON_OCT.minusSeconds(600)).limbs().tithiNumber());
        assertEquals(15, panchanga.at(CHENNAI_LAT, CHENNAI_LON,
                FULL_MOON_OCT.minusSeconds(600)).limbs().tithiNumber());
    }

    /**
     * Die Nullstellensuche selbst gegen die fremde Zeit: Der Übergang von Tithi 30 auf 1 ist der
     * Neumond. Gefunden werden darf er nur dort, wo die USNO ihn nennt.
     */
    @Test
    @DisplayName("Die gefundene Wechselzeit trifft den Neumond der USNO")
    void theCrossingSearchFindsTheUsnoNewMoon() {
        PanchangaDay day = panchanga.at(CHENNAI_LAT, CHENNAI_LON, NEW_MOON_OCT.minusSeconds(3600));
        Instant found = day.tithiEnd().toInstant();

        long deltaSeconds = Math.abs(java.time.Duration.between(NEW_MOON_OCT, found).getSeconds());
        assertTrue(deltaSeconds < 180,
                "Neumond laut USNO " + NEW_MOON_OCT + ", gefunden " + found
                        + " (" + deltaSeconds + " s Abstand)");
    }

    // ── Sonnenaufgang: die Konvention ───────────────────────────────────────

    /**
     * Der Prüfstein für den Blocker.
     *
     * <p>Die hinduistische Kalenderrechnung nimmt den <b>Mittelpunkt</b> der Sonnenscheibe
     * <b>ohne</b> Refraktion. Beides lässt die Sonne später aufgehen als die bürgerliche
     * Definition (oberer Rand, mit Refraktion), nach der die USNO rechnet: Der obere Rand steht
     * um den scheinbaren Halbmesser (rund 16′) höher, die Refraktion hebt die Sonne um weitere
     * rund 34′. Zusammen gut 50′ — in Chennai, wo die Sonne steil aufgeht, sind das etwa drei bis
     * vier Minuten.
     *
     * <p>Dieser Test ist der Grund, warum die Klasse existiert. Mit dem ursprünglich verwendeten
     * {@code SE_CALC_RISE} allein läge der Wert <b>auf</b> der USNO-Zeit statt dahinter, und alle
     * übrigen Tests wären trotzdem grün geblieben.
     */
    @Test
    @DisplayName("Der hinduistische Sonnenaufgang liegt nach dem bürgerlichen der USNO")
    void theHinduSunriseLagsBehindTheCivilOne() {
        // Mittags fragen, damit der Sonnenaufgang desselben Tages gefunden wird.
        double noonJd = vedic.julianDay(LocalDateTime.of(2026, 10, 4, 12, 0));
        OptionalDouble sunrise = vedic.lastSunriseBefore(noonJd, CHENNAI_LAT, CHENNAI_LON, Ayanamsa.LAHIRI);
        assertTrue(sunrise.isPresent());

        Instant found = toInstant(sunrise.getAsDouble());
        long lagSeconds = java.time.Duration.between(CHENNAI_CIVIL_SUNRISE, found).getSeconds();

        assertTrue(lagSeconds > 90,
                "Der hinduistische Aufgang muss SPÄTER liegen als der bürgerliche der USNO ("
                        + CHENNAI_CIVIL_SUNRISE + "), gefunden " + found
                        + " — Abstand " + lagSeconds + " s. Zu klein heißt: es wird doch der "
                        + "bürgerliche Aufgang gerechnet.");
        assertTrue(lagSeconds < 480,
                "… aber nicht beliebig viel später: " + lagSeconds + " s deuten auf eine falsche "
                        + "Definition, nicht auf Scheibenmittelpunkt ohne Refraktion.");
    }

    /**
     * Der Sonnenaufgang wird minutengenau mit der USNO verglichen, ohne die Konvention: Beide
     * müssen jedenfalls denselben Morgen meinen. Ein um Stunden verschobener Wert käme von einer
     * falschen Zeitzone oder vertauschten Koordinaten — der Fehler, den die Reihenfolge in
     * {@code geopos} nahelegt.
     */
    @Test
    @DisplayName("Der gefundene Aufgang liegt am selben Morgen wie der der USNO")
    void theSunriseIsOnTheSameMorning() {
        double noonJd = vedic.julianDay(LocalDateTime.of(2026, 10, 4, 12, 0));
        Instant found = toInstant(vedic.lastSunriseBefore(
                noonJd, CHENNAI_LAT, CHENNAI_LON, Ayanamsa.LAHIRI).getAsDouble());

        long deltaMinutes = Math.abs(
                java.time.Duration.between(CHENNAI_CIVIL_SUNRISE, found).toMinutes());
        assertTrue(deltaMinutes < 15,
                "mehr als eine Viertelstunde Unterschied deutet auf vertauschte Koordinaten "
                        + "oder eine falsche Zeitbasis: USNO " + CHENNAI_CIVIL_SUNRISE
                        + ", gefunden " + found);
    }

    /**
     * Wirken wirklich <b>beide</b> Bits?
     *
     * <p>Der Vergleich mit der USNO allein beweist das nicht: Auch „oberer Rand ohne Refraktion"
     * läge im selben Zeitfenster. Erst die vier Modi gegeneinander trennen die Wirkungen.
     *
     * <p>Beide Bits verzögern den Aufgang, und zwar unabhängig voneinander: Der Scheibenmittelpunkt
     * liegt rund 16′ tiefer als der obere Rand, das Weglassen der Refraktion weitere rund 34′.
     * Also muss gelten: bürgerlich &lt; (je eines) &lt; beide — und der Gesamtverzug muss ungefähr
     * der Summe der Einzelverzüge entsprechen.
     */
    @Test
    @DisplayName("Scheibenmittelpunkt und fehlende Refraktion wirken beide, und zwar getrennt")
    void bothRisingBitsTakeEffect() {
        double noonJd = vedic.julianDay(LocalDateTime.of(2026, 10, 4, 12, 0));

        double civil = sunrise(noonJd, swisseph.SweConst.SE_CALC_RISE);
        double discCentreOnly = sunrise(noonJd,
                swisseph.SweConst.SE_CALC_RISE | swisseph.SweConst.SE_BIT_DISC_CENTER);
        double noRefractionOnly = sunrise(noonJd,
                swisseph.SweConst.SE_CALC_RISE | swisseph.SweConst.SE_BIT_NO_REFRACTION);
        double both = sunrise(noonJd, swisseph.SweConst.SE_CALC_RISE
                | swisseph.SweConst.SE_BIT_DISC_CENTER | swisseph.SweConst.SE_BIT_NO_REFRACTION);

        double discLag = (discCentreOnly - civil) * 86400.0;
        double refractionLag = (noRefractionOnly - civil) * 86400.0;
        double totalLag = (both - civil) * 86400.0;

        assertTrue(discLag > 10, "SE_BIT_DISC_CENTER zeigt keine Wirkung: " + discLag + " s");
        assertTrue(refractionLag > 10, "SE_BIT_NO_REFRACTION zeigt keine Wirkung: " + refractionLag + " s");
        assertTrue(totalLag > discLag && totalLag > refractionLag,
                "beide zusammen müssen später liegen als jedes für sich");
        // Die Höhenbeiträge addieren sich nahezu; die Umrechnung in Zeit ist nur schwach nichtlinear.
        assertEquals(discLag + refractionLag, totalLag, 30.0,
                "der Gesamtverzug passt nicht zur Summe der Einzelwirkungen — wirkt eines der Bits nicht?");

        // Und der Betriebsmodus ist genau dieser Gesamtmodus.
        double operational = vedic.lastSunriseBefore(
                noonJd, CHENNAI_LAT, CHENNAI_LON, Ayanamsa.LAHIRI).getAsDouble();
        assertEquals(both, operational, 1.0 / 86400.0,
                "der Betriebsmodus ist nicht derselbe wie der hier geprüfte");
    }

    private static double sunrise(double jd, int mode) {
        return vedic.sunriseForTest(jd, CHENNAI_LAT, CHENNAI_LON, mode).orElseThrow();
    }

    // ── Vertrag ─────────────────────────────────────────────────────────────

    /**
     * Polarnacht ist kein Serverfehler.
     *
     * <p>Longyearbyen Ende Dezember: Die Sonne geht wochenlang nicht auf. Ohne Sonnenaufgang gibt
     * es keinen vedischen Tag — das ist ein erwartbarer Zustand, und die eigene Ausnahme sorgt
     * dafür, dass daraus ein 422 mit Code statt eines 500 wird.
     */
    @Test
    @DisplayName("Polarnacht ergibt eine eigene Ausnahme, keinen internen Fehler")
    void polarNightRaisesItsOwnException() {
        astroapi.exeptions.NoSunriseException thrown = assertThrows(
                astroapi.exeptions.NoSunriseException.class,
                () -> panchanga.at(78.22, 15.63, LocalDateTime.of(2026, 12, 21, 12, 0)));
        assertEquals(78.22, thrown.getLatitude(), 1e-9);
        assertTrue(thrown.getMessage().toLowerCase(java.util.Locale.ROOT).contains("polar"));
    }

    /**
     * Zwei Wege zum selben Augenblick müssen dasselbe ergeben. Das ist die Probe darauf, dass
     * {@code instant} und {@code dateTime} nicht zwei verschiedene Zeitbasen benutzen.
     */
    @Test
    @DisplayName("Augenblick und Ortszeit führen zum selben Panchanga")
    void bothTimeContractsAgree() {
        // 2026-10-04 12:00 in Chennai ist 06:30 UTC.
        PanchangaDay viaLocal = panchanga.at(CHENNAI_LAT, CHENNAI_LON,
                LocalDateTime.of(2026, 10, 4, 12, 0));
        PanchangaDay viaInstant = panchanga.at(CHENNAI_LAT, CHENNAI_LON,
                Instant.parse("2026-10-04T06:30:00Z"));

        assertEquals(viaLocal.limbs().tithiNumber(), viaInstant.limbs().tithiNumber());
        assertEquals(viaLocal.limbs().vara(), viaInstant.limbs().vara());
        assertEquals(viaLocal.sunrise(), viaInstant.sunrise());
    }

    @Test
    @DisplayName("Die Anfrage lehnt unmögliche Koordinaten und doppelte Zeitangaben ab")
    void therequestGuardsItsOwnContract() {
        astroapi.vedic.dto.PanchangaRequest request = new astroapi.vedic.dto.PanchangaRequest();

        request.setCoordinates(new double[]{13.0, 80.0});
        assertTrue(request.isCoordinatesInRange());
        request.setCoordinates(new double[]{80.0, 200.0});
        assertFalse(request.isCoordinatesInRange(), "Länge über 180 muss auffallen");
        request.setCoordinates(new double[]{95.0, 10.0});
        assertFalse(request.isCoordinatesInRange(), "Breite über 90 muss auffallen");
        request.setCoordinates(new double[]{Double.NaN, 10.0});
        assertFalse(request.isCoordinatesInRange(), "NaN darf nicht in die Ephemeride");

        request.setCoordinates(new double[]{13.0, 80.0});
        assertFalse(request.isExactlyOneMomentGiven(), "ohne Zeitangabe nicht gültig");
        request.setDateTime(LocalDateTime.of(2026, 10, 4, 12, 0));
        assertTrue(request.isExactlyOneMomentGiven());
        request.setInstant(java.time.OffsetDateTime.parse("2026-10-04T12:00:00+05:30"));
        assertFalse(request.isExactlyOneMomentGiven(), "beides zugleich wäre mehrdeutig");
    }

    // ── Hilfen ──────────────────────────────────────────────────────────────

    private static double[] sunAndMoonAt(Instant instant) {
        double jd = vedic.julianDay(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
        return vedic.siderealSunAndMoon(jd, Ayanamsa.LAHIRI);
    }

    private static Instant toInstant(double julianDay) {
        return Instant.ofEpochMilli(Math.round((julianDay - 2440587.5) * 86_400_000.0));
    }
}
