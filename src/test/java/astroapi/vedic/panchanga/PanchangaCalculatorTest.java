package astroapi.vedic.panchanga;

import astroapi.vedic.Nakshatra;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Die fünf Glieder, geprüft ohne Ephemeride.
 *
 * <p>Alles hier rechnet auf eingesetzten Längengraden. Das ist Absicht: Die Tabellen und die
 * Verteilung der Karanas sind der Teil, an dem sich ein Panchanga-Rechner verzählt, und der
 * lässt sich ohne Swiss Ephemeris vollständig festnageln.
 */
class PanchangaCalculatorTest {

    /** Ein Montag — für alle Tests, in denen der Vara nicht die Frage ist. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    // ── Tithi ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Neumond beginnt die erste Tithi der zunehmenden Hälfte")
    void newMoonStartsTheFirstTithi() {
        Panchanga p = PanchangaCalculator.at(100.0, 100.0, MONDAY);
        assertEquals(1, p.tithiNumber());
        assertEquals(Tithi.PRATIPADA, p.tithi());
        assertEquals(Paksha.SHUKLA, p.paksha());
        assertEquals(0.0, p.tithiFraction(), 1e-9);
    }

    @Test
    @DisplayName("Vollmond ist Purnima und der Wechsel in die abnehmende Hälfte")
    void fullMoonIsPurnima() {
        // Genau 180° Unterschied: die fünfzehnte Tithi ist gerade vorbei, die sechzehnte beginnt.
        Panchanga p = PanchangaCalculator.at(10.0, 190.0, MONDAY);
        assertEquals(16, p.tithiNumber());
        assertEquals(Paksha.KRISHNA, p.paksha());

        // Kurz davor: noch Purnima.
        Panchanga before = PanchangaCalculator.at(10.0, 189.9, MONDAY);
        assertEquals(15, before.tithiNumber());
        assertEquals(Paksha.SHUKLA, before.paksha());
        assertEquals("Purnima", before.tithiDisplayName());
        assertEquals("purnima", before.tithiKey());
    }

    @Test
    @DisplayName("Die dreißigste Tithi heißt Amavasya, nicht Purnima")
    void theLastTithiIsAmavasya() {
        Panchanga p = PanchangaCalculator.at(10.0, 10.0 - 0.1, MONDAY);
        assertEquals(30, p.tithiNumber());
        assertEquals(Paksha.KRISHNA, p.paksha());
        assertEquals("Amavasya", p.tithiDisplayName());
        assertEquals("amavasya", p.tithiKey());
    }

    @Test
    @DisplayName("Jede der 30 Tithis trägt den Namen ihrer Hälfte")
    void allThirtyTithisAreNamedConsistently() {
        for (int index = 0; index < 30; index++) {
            double elongation = index * PanchangaCalculator.TITHI_SPAN + 1.0;
            Panchanga p = PanchangaCalculator.at(0.0, elongation, MONDAY);
            assertEquals(index + 1, p.tithiNumber(), "Tithi-Nummer bei Index " + index);
            assertEquals(index % 15 + 1, p.tithi().getNumberInPaksha(), "Nummer in der Hälfte");
            assertEquals(index < 15 ? Paksha.SHUKLA : Paksha.KRISHNA, p.paksha());
        }
    }

    // ── Karana ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Das erste Karana ist Kimstughna, danach beginnt der bewegliche Kreis")
    void theFirstKaranaIsFixed() {
        assertEquals(Karana.KIMSTUGHNA, Karana.ofAbsoluteIndex(0));
        assertEquals(Karana.BAVA, Karana.ofAbsoluteIndex(1));
        assertEquals(Karana.VISHTI, Karana.ofAbsoluteIndex(7));
        assertEquals(Karana.BAVA, Karana.ofAbsoluteIndex(8));
    }

    @Test
    @DisplayName("Die drei letzten Karanas sind fest, nicht der achte Umlauf")
    void theLastThreeKaranasAreFixed() {
        assertEquals(Karana.VISHTI, Karana.ofAbsoluteIndex(56), "der achte Umlauf endet mit Vishti");
        assertEquals(Karana.SHAKUNI, Karana.ofAbsoluteIndex(57));
        assertEquals(Karana.CHATUSHPADA, Karana.ofAbsoluteIndex(58));
        assertEquals(Karana.NAGA, Karana.ofAbsoluteIndex(59));
    }

    /**
     * Die eigentliche Probe auf das Schema: 1 + 56 + 3 = 60. Vishti muss genau achtmal vorkommen,
     * Kimstughna, Shakuni, Chatushpada und Naga je einmal.
     */
    @Test
    @DisplayName("Über einen ganzen Monat stimmen die Häufigkeiten der elf Karanas")
    void theKaranaDistributionAddsUp() {
        java.util.Map<Karana, Integer> counts = new java.util.EnumMap<>(Karana.class);
        for (int index = 0; index < 60; index++) {
            counts.merge(Karana.ofAbsoluteIndex(index), 1, Integer::sum);
        }
        assertEquals(8, counts.get(Karana.VISHTI), "Vishti kommt achtmal vor");
        for (Karana movable : new Karana[]{Karana.BAVA, Karana.BALAVA, Karana.KAULAVA,
                Karana.TAITILA, Karana.GARA, Karana.VANIJA}) {
            assertEquals(8, counts.get(movable), movable + " kommt achtmal vor");
        }
        for (Karana fixed : new Karana[]{Karana.KIMSTUGHNA, Karana.SHAKUNI,
                Karana.CHATUSHPADA, Karana.NAGA}) {
            assertEquals(1, counts.get(fixed), fixed + " kommt genau einmal vor");
        }
        assertEquals(60, counts.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    @DisplayName("Ein Karana ist eine halbe Tithi")
    void aKaranaIsHalfATithi() {
        // Mitten in der ersten Tithi: erstes Karana. Zweite Hälfte: zweites Karana.
        assertEquals(1, PanchangaCalculator.karanaIndex(0.0, 3.0) + 1);
        assertEquals(2, PanchangaCalculator.karanaIndex(0.0, 9.0) + 1);
        assertEquals(1, PanchangaCalculator.tithiIndex(0.0, 3.0) + 1);
        assertEquals(1, PanchangaCalculator.tithiIndex(0.0, 9.0) + 1);
    }

    // ── Yoga ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Das Yoga zählt die Summe, nicht die Differenz")
    void theYogaUsesTheSum() {
        // Summe 0° → erstes Yoga. Die Differenz ist hier 100° und damit eine ganz andere Tithi.
        Panchanga p = PanchangaCalculator.at(310.0, 50.0, MONDAY);
        assertEquals(1, p.yogaNumber());
        assertEquals(NityaYoga.VISHKAMBHA, p.yoga());
        // Differenz: norm(50 − 310) = 100°, geteilt durch 12 ist Abschnitt 8 — die neunte Tithi.
        assertEquals(9, p.tithiNumber(), "die Differenz liefert etwas völlig anderes");
    }

    /**
     * Der Fehler, den diese Klasse verhindern soll: Yoga aus tropischen Längen.
     *
     * <p>Eine Verschiebung des Nullpunkts um das Ayanamsha δ verschiebt die Summe um 2δ. Bei
     * heutigen rund 24° sind das 48° — fast vier Yoga-Abschnitte. Die Tithi bleibt dabei
     * unberührt, das Ergebnis sieht also halb richtig aus.
     */
    @Test
    @DisplayName("Ein verschobener Nullpunkt lässt Tithi gleich und Yoga springen")
    void shiftingTheZeroPointMovesOnlyTheYoga() {
        double ayanamsha = 24.0;
        double sunTropical = 200.0;
        double moonTropical = 95.0;

        Panchanga sidereal = PanchangaCalculator.at(
                sunTropical - ayanamsha, moonTropical - ayanamsha, MONDAY);
        Panchanga tropicalByMistake = PanchangaCalculator.at(sunTropical, moonTropical, MONDAY);

        assertEquals(sidereal.tithiNumber(), tropicalByMistake.tithiNumber(),
                "die Differenz ist unabhängig vom Nullpunkt");
        assertEquals(sidereal.karanaNumber(), tropicalByMistake.karanaNumber());
        assertNotEquals(sidereal.yogaNumber(), tropicalByMistake.yogaNumber(),
                "die Summe ist es nicht — genau deshalb muss das Yoga siderisch gerechnet werden");
    }

    /**
     * Das neutrale Panchanga wertet nicht.
     *
     * <p>Zuerst stand hier eine Einstufung „ungünstig" für Vyatipata und Vaidhriti. Das ist für
     * eine Hochzeit zu kurz gegriffen — dort zählen je nach Schule bis zu neun Yogas, teils nur
     * deren Visha-Ghati — und für jeden anderen Anlass schlicht unzuständig. Eine Wertung im
     * Panchanga wäre eine Hochzeitsregel, die auch alle anderen Aufrufer trifft. Sie gehört in
     * ein versioniertes Regelwerk, das den Anlass kennt.
     */
    @Test
    @DisplayName("Das Panchanga nennt das Yoga und bewertet es nicht")
    void thePanchangaDoesNotJudgeTheYoga() {
        for (java.lang.reflect.Method m : NityaYoga.class.getDeclaredMethods()) {
            assertFalse(m.getName().toLowerCase(java.util.Locale.ROOT).contains("auspicious"),
                    "Wertung ist zurück: " + m.getName());
            assertFalse(m.getName().toLowerCase(java.util.Locale.ROOT).contains("favourable"),
                    "Wertung ist zurück: " + m.getName());
        }
    }

    @Test
    @DisplayName("Alle 27 Yogas sind lückenlos durchnummeriert")
    void allYogasAreNumbered() {
        assertEquals(27, NityaYoga.values().length);
        for (int i = 0; i < 27; i++) {
            assertEquals(i + 1, NityaYoga.values()[i].getNumber());
            assertEquals(NityaYoga.values()[i], NityaYoga.ofAbsoluteIndex(i));
        }
    }

    // ── Nakshatra und Pada ──────────────────────────────────────────────────

    @Test
    @DisplayName("Nakshatra und Pada kommen aus derselben Division wie überall sonst")
    void nakshatraAndPadaMatchTheSharedDivision() {
        // 20° siderisch: zweites Nakshatra (Bharani beginnt bei 13°20′), erstes Pada.
        Panchanga p = PanchangaCalculator.at(0.0, 20.0, MONDAY);
        assertEquals(Nakshatra.ofLongitude(20.0), p.nakshatra());
        assertEquals(Nakshatra.BHARANI, p.nakshatra());
        assertEquals(3, p.pada());
    }

    @Test
    @DisplayName("An der Nakshatra-Grenze bleiben Nakshatra und Pada einig")
    void nakshatraAndPadaAgreeAtTheBoundary() {
        // Genau die Grenze Ashwini/Bharani; hier lag im Juli der Fehler zwischen beiden Werten.
        double boundary = astroapi.vedic.VedicMath.NAKSHATRA_SPAN;
        Panchanga at = PanchangaCalculator.at(0.0, boundary, MONDAY);
        assertEquals(Nakshatra.BHARANI, at.nakshatra());
        assertEquals(1, at.pada(), "ein neues Nakshatra beginnt mit Pada 1");

        Panchanga just_before = PanchangaCalculator.at(0.0, Math.nextDown(boundary), MONDAY);
        assertEquals(Nakshatra.ASHWINI, just_before.nakshatra());
        assertEquals(4, just_before.pada(), "und endet mit Pada 4");
    }

    // ── Vara ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Der Vara folgt dem Tag des Sonnenaufgangs")
    void theVaraFollowsTheSunriseDate() {
        assertEquals(Vara.SOMAVARA, PanchangaCalculator.at(0.0, 0.0, MONDAY).vara());
        assertEquals(Vara.RAVIVARA, Vara.ofSunriseDate(LocalDate.of(2026, 10, 4)));
        assertEquals(Vara.SHANIVARA, Vara.ofSunriseDate(LocalDate.of(2026, 10, 3)));
    }

    @Test
    @DisplayName("Jeder Vara hat seinen Herrscher")
    void everyVaraHasItsRuler() {
        assertEquals(astroapi.vedic.VedicBody.SUN, Vara.RAVIVARA.getRuler());
        assertEquals(astroapi.vedic.VedicBody.MOON, Vara.SOMAVARA.getRuler());
        assertEquals(astroapi.vedic.VedicBody.SATURN, Vara.SHANIVARA.getRuler());
        assertEquals(7, Vara.values().length);
    }

    // ── Robustheit ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ein ganzer Umlauf liefert nie einen Index außerhalb der Tabellen")
    void afullCircleNeverLeavesTheTables() {
        for (int tenth = 0; tenth < 3600; tenth++) {
            double moon = tenth / 10.0;
            Panchanga p = PanchangaCalculator.at(0.0, moon, MONDAY);
            assertTrue(p.tithiNumber() >= 1 && p.tithiNumber() <= 30, "Tithi bei " + moon);
            assertTrue(p.karanaNumber() >= 1 && p.karanaNumber() <= 60, "Karana bei " + moon);
            assertTrue(p.yogaNumber() >= 1 && p.yogaNumber() <= 27, "Yoga bei " + moon);
            assertTrue(p.pada() >= 1 && p.pada() <= 4, "Pada bei " + moon);
            assertTrue(p.tithiFraction() >= 0 && p.tithiFraction() < 1);
            assertTrue(p.yogaFraction() >= 0 && p.yogaFraction() < 1);
        }
    }

    @Test
    @DisplayName("Negative und übergroße Indizes werden abgelehnt statt stillschweigend gedeutet")
    void outOfRangeIndicesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Tithi.ofAbsoluteIndex(30));
        assertThrows(IllegalArgumentException.class, () -> Tithi.ofAbsoluteIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> Karana.ofAbsoluteIndex(60));
        assertThrows(IllegalArgumentException.class, () -> NityaYoga.ofAbsoluteIndex(27));
    }
}
