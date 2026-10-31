package astroapi.vedic.panchanga;

import astroapi.service.TimeZoneService;
import astroapi.vedic.Ayanamsa;
import astroapi.vedic.VedicEphemerisService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Das Panchanga gegen die echte Ephemeride.
 *
 * <p>Geprüft wird nicht, ob eine bestimmte Tithi herauskommt — das wäre eine Zahl aus dem Lauf
 * selbst und würde nur bestätigen, dass sich nichts geändert hat. Geprüft werden die
 * Eigenschaften, die stimmen <b>müssen</b>, egal welcher Tag: dass der Sonnenaufgang vor dem
 * gefragten Zeitpunkt und höchstens einen Tag davor liegt, dass jede Wechselzeit in der Zukunft
 * liegt und dass unmittelbar danach tatsächlich das nächste Glied gilt.
 *
 * <p>Letzteres ist der eigentliche Beweis: Eine Wechselzeit, nach der dasselbe Glied weiterläuft,
 * ist keine.
 */
class PanchangaServiceIntegrationTest {

    private static TimeZoneService zones;
    private static VedicEphemerisService vedic;
    private static PanchangaService panchanga;

    /** Hamburg — gemäßigte Breite, klarer Sonnenaufgang das ganze Jahr. */
    private static final double HAMBURG_LAT = 53.5511;
    private static final double HAMBURG_LON = 9.9937;

    /** Chennai — andere Zeitzone, andere Halbkugel der Tageslänge. */
    private static final double CHENNAI_LAT = 13.0827;
    private static final double CHENNAI_LON = 80.2707;

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

    @Test
    @DisplayName("Der Sonnenaufgang liegt vor dem Zeitpunkt und höchstens einen Tag davor")
    void sunriseOpensTheDay() {
        LocalDateTime noon = LocalDateTime.of(2026, 10, 4, 12, 0);
        PanchangaDay day = panchanga.at(HAMBURG_LAT, HAMBURG_LON, noon);

        ZonedDateTime sunrise = day.sunrise();
        assertNotNull(sunrise);
        assertTrue(sunrise.toLocalDateTime().isBefore(noon), "Sonnenaufgang muss davor liegen: " + sunrise);
        assertTrue(Duration.between(sunrise.toLocalDateTime(), noon).toHours() < 24,
                "und höchstens einen Tag davor: " + sunrise);
        // Hamburg Anfang Oktober: Sonnenaufgang am frühen Morgen, nicht nachts und nicht mittags.
        int hour = sunrise.getHour();
        assertTrue(hour >= 5 && hour <= 9, "unplausible Aufgangsstunde: " + hour);
    }

    /**
     * Der Fall, für den die Sonnenaufgangs-Grenze überhaupt gebaut ist: Wer nachts fragt, steht
     * noch im Tag von gestern. Ein Rechner, der auf das Kalenderdatum zurückfällt, liegt hier um
     * einen Wochentag daneben — und mit ihm alles, was am Vara hängt.
     */
    @Test
    @DisplayName("Vor dem Sonnenaufgang gilt noch der Vara des Vortages")
    void beforeSunriseTheDayBelongsToYesterday() {
        PanchangaDay night = panchanga.at(HAMBURG_LAT, HAMBURG_LON,
                LocalDateTime.of(2026, 10, 4, 3, 0));
        PanchangaDay noon = panchanga.at(HAMBURG_LAT, HAMBURG_LON,
                LocalDateTime.of(2026, 10, 4, 12, 0));

        assertEquals(night.sunrise().toLocalDate().plusDays(1), noon.sunrise().toLocalDate(),
                "nachts gehört der Zeitpunkt zum Sonnenaufgang des Vortages");
        assertNotEquals(night.limbs().vara(), noon.limbs().vara(),
                "und damit zu einem anderen Vara");
    }

    @Test
    @DisplayName("Alle fünf Glieder liegen im gültigen Bereich")
    void allLimbsAreInRange() {
        for (double[] place : new double[][]{{HAMBURG_LAT, HAMBURG_LON}, {CHENNAI_LAT, CHENNAI_LON}}) {
            PanchangaDay day = panchanga.at(place[0], place[1], LocalDateTime.of(2026, 10, 4, 12, 0));
            Panchanga limbs = day.limbs();
            assertTrue(limbs.tithiNumber() >= 1 && limbs.tithiNumber() <= 30);
            assertTrue(limbs.karanaNumber() >= 1 && limbs.karanaNumber() <= 60);
            assertTrue(limbs.yogaNumber() >= 1 && limbs.yogaNumber() <= 27);
            assertTrue(limbs.pada() >= 1 && limbs.pada() <= 4);
            assertNotNull(limbs.nakshatra());
            assertNotNull(limbs.vara());
            assertNotNull(limbs.tithiDisplayName());
        }
    }

    /**
     * Tithi und Karana hängen an der Differenz Mond − Sonne und sind damit vom Ort unabhängig;
     * nur der Vara und die Zeiten unterscheiden sich. Derselbe Augenblick, zwei Orte.
     */
    @Test
    @DisplayName("Zum selben Augenblick hat die Tithi überall denselben Wert")
    void theTithiIsTheSameEverywhereAtTheSameInstant() {
        // 12:00 in Hamburg ist 15:30 in Chennai — derselbe Augenblick.
        PanchangaDay hamburg = panchanga.at(HAMBURG_LAT, HAMBURG_LON,
                LocalDateTime.of(2026, 10, 4, 12, 0));
        PanchangaDay chennai = panchanga.at(CHENNAI_LAT, CHENNAI_LON,
                LocalDateTime.of(2026, 10, 4, 15, 30));

        assertEquals(hamburg.limbs().tithiNumber(), chennai.limbs().tithiNumber());
        assertEquals(hamburg.limbs().yogaNumber(), chennai.limbs().yogaNumber());
        assertEquals(hamburg.limbs().nakshatra(), chennai.limbs().nakshatra());
    }

    @Test
    @DisplayName("Jede Wechselzeit liegt in der Zukunft und innerhalb von zwei Tagen")
    void everyTransitionLiesAhead() {
        LocalDateTime moment = LocalDateTime.of(2026, 10, 4, 12, 0);
        PanchangaDay day = panchanga.at(HAMBURG_LAT, HAMBURG_LON, moment);

        for (ZonedDateTime end : new ZonedDateTime[]{
                day.tithiEnd(), day.karanaEnd(), day.yogaEnd(), day.nakshatraEnd()}) {
            assertNotNull(end, "in gemäßigter Breite muss jede Wechselzeit gefunden werden");
            assertTrue(end.toLocalDateTime().isAfter(moment), "Wechsel liegt in der Vergangenheit: " + end);
            assertTrue(Duration.between(moment, end.toLocalDateTime()).toHours() <= 48,
                    "kein Glied dauert zwei Tage: " + end);
        }
    }

    /**
     * Der eigentliche Beweis der Wechselzeit: unmittelbar davor gilt noch das alte Glied,
     * unmittelbar danach das nächste. Eine Zeit, nach der dasselbe weiterläuft, ist keine.
     */
    @Test
    @DisplayName("Nach der Wechselzeit gilt wirklich die nächste Tithi")
    void theTithiReallyChangesAtItsEnd() {
        LocalDateTime moment = LocalDateTime.of(2026, 10, 4, 12, 0);
        PanchangaDay day = panchanga.at(HAMBURG_LAT, HAMBURG_LON, moment);
        int before = day.limbs().tithiNumber();

        LocalDateTime justBefore = day.tithiEnd().toLocalDateTime().minusMinutes(2);
        LocalDateTime justAfter = day.tithiEnd().toLocalDateTime().plusMinutes(2);

        assertEquals(before, panchanga.at(HAMBURG_LAT, HAMBURG_LON, justBefore).limbs().tithiNumber(),
                "zwei Minuten vorher gilt noch die alte Tithi");
        assertEquals(before % 30 + 1,
                panchanga.at(HAMBURG_LAT, HAMBURG_LON, justAfter).limbs().tithiNumber(),
                "zwei Minuten danach die nächste");
    }

    @Test
    @DisplayName("Nach der Wechselzeit steht der Mond wirklich im nächsten Nakshatra")
    void theNakshatraReallyChangesAtItsEnd() {
        LocalDateTime moment = LocalDateTime.of(2026, 10, 4, 12, 0);
        PanchangaDay day = panchanga.at(HAMBURG_LAT, HAMBURG_LON, moment);
        astroapi.vedic.Nakshatra before = day.limbs().nakshatra();

        LocalDateTime justAfter = day.nakshatraEnd().toLocalDateTime().plusMinutes(2);
        astroapi.vedic.Nakshatra after =
                panchanga.at(HAMBURG_LAT, HAMBURG_LON, justAfter).limbs().nakshatra();

        assertNotEquals(before, after);
        assertEquals(before.getIndex() % 27 + 1, after.getIndex(),
                "und zwar im unmittelbar folgenden, nicht irgendeinem");
    }

    /**
     * Das Karana ist die halbe Tithi — sein Ende fällt entweder mit dem Tithi-Ende zusammen oder
     * liegt davor. Nie danach. Das prüft, dass beide dieselbe Winkelgröße beobachten.
     */
    @Test
    @DisplayName("Das Karana endet nie nach seiner Tithi")
    void theKaranaNeverOutlastsItsTithi() {
        PanchangaDay day = panchanga.at(CHENNAI_LAT, CHENNAI_LON, LocalDateTime.of(2026, 10, 4, 10, 0));
        assertFalse(day.karanaEnd().isAfter(day.tithiEnd()),
                "Karana " + day.karanaEnd() + " nach Tithi " + day.tithiEnd());
    }

    @Test
    @DisplayName("Die Suche nach dem Sonnenaufgang liefert einen Zeitpunkt vor dem Startpunkt")
    void theSunriseSearchLooksBackwards() {
        double jd = vedic.julianDay(LocalDateTime.of(2026, 10, 4, 10, 0));
        OptionalDouble sunrise = vedic.lastSunriseBefore(jd, HAMBURG_LAT, HAMBURG_LON, Ayanamsa.LAHIRI);

        assertTrue(sunrise.isPresent());
        assertTrue(sunrise.getAsDouble() < jd, "der gefundene Aufgang muss davor liegen");
        assertTrue(jd - sunrise.getAsDouble() < 1.0, "und höchstens einen Tag davor");
    }
    // ── Antwortform ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Die Antwort trägt den Berechnungsvertrag und alle fünf Glieder")
    void theResponseCarriesEverythingAnInterfaceNeeds() {
        astroapi.vedic.dto.PanchangaResponseDTO dto = panchanga.describe(
                HAMBURG_LAT, HAMBURG_LON, LocalDateTime.of(2026, 10, 4, 12, 0));

        assertEquals(astroapi.vedic.VedicRuleSet.CALCULATION_SYSTEM, dto.getCalculationSystem());
        assertEquals(astroapi.vedic.VedicRuleSet.CALCULATION_VERSION, dto.getCalculationVersion());
        assertEquals(Ayanamsa.LAHIRI.getKey(), dto.getAyanamsa());

        for (astroapi.vedic.dto.PanchangaLimbDTO limb : new astroapi.vedic.dto.PanchangaLimbDTO[]{
                dto.getTithi(), dto.getKarana(), dto.getYoga(), dto.getNakshatra(), dto.getVara()}) {
            assertNotNull(limb);
            // Alle Glieder zählen ab 1 — auch der Vara, obwohl sein Enum intern ab 0 zählt.
            assertTrue(limb.getNumber() >= 1, "jedes Glied zählt ab 1");
            assertNotNull(limb.getKey(), "und einen Schlüssel zum Übersetzen");
            assertNotNull(limb.getName(), "und einen Namen als Rückfallebene");
        }
        assertNotNull(dto.getPaksha());
        assertNotNull(dto.getVaraRuler());
        assertTrue(dto.getPada() >= 1 && dto.getPada() <= 4);
    }

    /**
     * Alle Zeiten in Ortszeit mit Offset. Eine Oberfläche soll sie anzeigen können, ohne die
     * Zeitzone noch einmal selbst zu bestimmen — die kennt dieser Dienst bereits.
     */
    @Test
    @DisplayName("Alle Zeiten tragen ihre Zone")
    void allTimesCarryTheirOffset() {
        astroapi.vedic.dto.PanchangaResponseDTO dto = panchanga.describe(
                CHENNAI_LAT, CHENNAI_LON, LocalDateTime.of(2026, 10, 4, 12, 0));

        for (String time : new String[]{dto.getMoment(), dto.getVedicDayStart(),
                dto.getTithi().getEndsAt(), dto.getNakshatra().getEndsAt()}) {
            assertNotNull(time);
            assertDoesNotThrow(() -> java.time.OffsetDateTime.parse(time), "nicht ISO mit Offset: " + time);
            assertTrue(time.contains("+05:30"), "Chennai-Offset fehlt: " + time);
        }
    }

    @Test
    @DisplayName("Der Vara kommt ohne Endzeit — die steht im Sonnenaufgang des Folgetages")
    void theVaraHasNoEndTime() {
        astroapi.vedic.dto.PanchangaResponseDTO dto = panchanga.describe(
                HAMBURG_LAT, HAMBURG_LON, LocalDateTime.of(2026, 10, 4, 12, 0));
        assertNull(dto.getVara().getEndsAt());
    }

    /**
     * Vishti bleibt als <b>sachliche</b> Kennzeichnung — es ist ein Karana-Name, keine Wertung.
     * Die Einstufung „ungünstig" ist dagegen aus dem Panchanga verschwunden: Welche Yogas für
     * einen Anlass sprechen, entscheidet ein versioniertes Muhurta-Regelwerk, nicht der Kalender.
     */
    @Test
    @DisplayName("Vishti ist abgeleitet, und das Yoga trägt keine Wertung mehr")
    void vishtiIsDerivedAndTheYogaCarriesNoVerdict() {
        for (int day = 1; day <= 28; day++) {
            astroapi.vedic.dto.PanchangaResponseDTO dto = panchanga.describe(
                    HAMBURG_LAT, HAMBURG_LON, LocalDateTime.of(2026, 10, day, 12, 0));
            assertEquals("vishti".equals(dto.getKarana().getKey()), dto.isKaranaVishti(),
                    "Vishti-Merkmal passt nicht zum Karana am " + day + ".");
        }
        for (java.lang.reflect.Method m : astroapi.vedic.dto.PanchangaResponseDTO.class.getDeclaredMethods()) {
            assertFalse(m.getName().toLowerCase(java.util.Locale.ROOT).contains("auspicious"),
                    "Die Antwort wertet wieder: " + m.getName());
        }
    }
}
