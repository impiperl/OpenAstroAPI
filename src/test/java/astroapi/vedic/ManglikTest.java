package astroapi.vedic;

import astroapi.vedic.dto.ManglikReferenceDTO;
import astroapi.vedic.dto.ManglikResultDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicPlanetDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manglik / Kuja Dosha — geprüfte Häuser, die drei Bezugspunkte und jede Aufhebungsregel.
 *
 * <p>Auch hier synthetische Charts: der Befund hängt an Zeichen, nicht an Graden, und so lässt
 * sich jede einzelne Regel gezielt auslösen. Ein Test gegen echte Geburtsdaten würde dieselbe
 * Regel bestenfalls zufällig treffen.
 */
class ManglikTest {

    /**
     * Baut ein Chart, in dem die Zeichen der beteiligten Grahas frei gesetzt werden können. Nur
     * die Felder, die {@link ManglikCalculator} liest, sind belegt.
     */
    private static VedicChartDTO chart(Rashi ascendant, Rashi mars, Rashi moon,
                                       Rashi venus, Rashi jupiter) {
        VedicChartDTO c = new VedicChartDTO();
        c.setLabel("X");
        c.setTimeDependentAvailable(true);
        c.setAscendantRashiIndex(ascendant.getIndex());
        c.setAscendantRashi(ascendant.getKey());
        List<VedicPlanetDTO> planets = new ArrayList<>();
        planets.add(planet(VedicBody.MARS, mars));
        planets.add(planet(VedicBody.MOON, moon));
        planets.add(planet(VedicBody.VENUS, venus));
        planets.add(planet(VedicBody.JUPITER, jupiter));
        c.setPlanets(planets);
        return c;
    }

    private static VedicPlanetDTO planet(VedicBody body, Rashi rashi) {
        VedicPlanetDTO p = new VedicPlanetDTO();
        p.setBody(body.getKey());
        p.setRashi(rashi.getKey());
        p.setRashiIndex(rashi.getIndex());
        p.setSiderealLongitude(rashi.getIndex() * 30.0 + 10.0);
        p.setDegreeInSignFormatted("10°00′00″");
        p.setNavamsaRashi(Navamsa.signOf(rashi.getIndex() * 30.0 + 10.0).getKey());
        p.setNakshatra(VedicEphemerisService.nakshatraPosition(rashi.getIndex() * 30.0 + 10.0));
        return p;
    }

    /**
     * Ein Chart ohne jede Aufhebung: Aszendent Zwillinge, Mars in Fische (10. Haus — nicht
     * geprüft), Jupiter und Venus so weit weg, dass weder Drishti noch Konjunktion greifen.
     */
    private static VedicChartDTO neutralChart() {
        // Jupiter in Zwillinge: Abstand zu Mars (Fische) = 10 → keine Drishti (1/5/7/9).
        return chart(Rashi.MITHUNA, Rashi.MEENA, Rashi.MITHUNA, Rashi.VRISHABHA, Rashi.MITHUNA);
    }

    private static ManglikReferenceDTO ref(ManglikResultDTO r, String name) {
        return r.getReferences().stream().filter(x -> name.equals(x.getReference())).findFirst()
                .orElseThrow(() -> new AssertionError("Bezugspunkt fehlt: " + name));
    }

    @Test
    @DisplayName("Die geprüften Häuser sind 1, 2, 4, 7, 8 und 12 — und stehen in der Antwort")
    void checkedHousesAreDeclared() {
        assertEquals(List.of(1, 2, 4, 7, 8, 12), ManglikCalculator.CHECKED_HOUSES);
        ManglikResultDTO r = ManglikCalculator.calculate(neutralChart());
        assertEquals(List.of(1, 2, 4, 7, 8, 12), r.getCheckedHouses());
        assertEquals(3, r.getReferences().size());
        assertNotNull(ref(r, "lagna"));
        assertNotNull(ref(r, "moon"));
        assertNotNull(ref(r, "venus"));
    }

    @Test
    @DisplayName("Mars in einem nicht geprüften Haus ergibt keinen Befund")
    void noAfflictionOutsideCheckedHouses() {
        ManglikResultDTO r = ManglikCalculator.calculate(neutralChart());
        assertFalse(r.isManglik());
        assertFalse(r.isManglikAfterCancellation());
        assertEquals("none", r.getSeverity());
        assertEquals(10, ref(r, "lagna").getMarsHouse());
        assertFalse(ref(r, "lagna").isAfflicted());
    }

    @Test
    @DisplayName("Jedes geprüfte Haus vom Lagna löst einen Befund aus")
    void everyCheckedHouseFromLagnaTriggers() {
        for (int house : ManglikCalculator.CHECKED_HOUSES) {
            // Aszendent Zwillinge, Mars in das gewünschte Haus setzen.
            Rashi ascendant = Rashi.MITHUNA;
            Rashi mars = Rashi.byIndex(ascendant.getIndex() + house - 1);
            // Mond und Venus auf ein Zeichen, von dem Mars in Haus 10 steht → sie sprechen nicht an.
            Rashi quiet = Rashi.byIndex(mars.getIndex() - 9);
            // Jupiter auf einen Abstand ohne Drishti (10 Zeichen von Mars).
            Rashi jupiter = Rashi.byIndex(mars.getIndex() - 9);
            VedicChartDTO c = chart(ascendant, mars, quiet, quiet, jupiter);
            ManglikResultDTO r = ManglikCalculator.calculate(c);
            assertEquals(house, ref(r, "lagna").getMarsHouse(), "Haus " + house);
            assertTrue(ref(r, "lagna").isAfflicted(), "Haus " + house + " muss ansprechen");
            assertTrue(r.isManglik(), "Haus " + house);
        }
    }

    @Test
    @DisplayName("Jedes nicht geprüfte Haus vom Lagna löst keinen Befund aus")
    void otherHousesDoNotTrigger() {
        for (int house : new int[]{3, 5, 6, 9, 10, 11}) {
            Rashi ascendant = Rashi.MITHUNA;
            Rashi mars = Rashi.byIndex(ascendant.getIndex() + house - 1);
            Rashi quiet = Rashi.byIndex(mars.getIndex() - 9);
            VedicChartDTO c = chart(ascendant, mars, quiet, quiet, quiet);
            ManglikResultDTO r = ManglikCalculator.calculate(c);
            assertFalse(ref(r, "lagna").isAfflicted(), "Haus " + house);
            assertFalse(r.isManglik(), "Haus " + house + " darf nicht ansprechen");
        }
    }

    @Test
    @DisplayName("Mond und Venus sind eigene Bezugspunkte und werden einzeln gemeldet")
    void moonAndVenusAreSeparateReferences() {
        // Lagna Zwillinge, Mars in Fische → Haus 10 vom Lagna, kein Befund.
        // Mond in Löwe → Mars steht im 8. Haus vom Mond → Befund.
        Rashi jupiterNoDrishti = Rashi.MITHUNA; // Abstand 10 zu Fische
        VedicChartDTO c = chart(Rashi.MITHUNA, Rashi.MEENA, Rashi.SIMHA, Rashi.VRISHABHA, jupiterNoDrishti);
        ManglikResultDTO r = ManglikCalculator.calculate(c);
        assertFalse(ref(r, "lagna").isAfflicted());
        assertEquals(8, ref(r, "moon").getMarsHouse());
        assertTrue(ref(r, "moon").isAfflicted());
        assertTrue(r.isManglik());
        // Nur Mond/Venus betroffen → milder Schweregrad.
        assertEquals("mild", r.getSeverity());
    }

    @Test
    @DisplayName("Schweregrad: 7. und 8. Haus vom Lagna gelten als stark, die anderen als mittel")
    void severityGrading() {
        // Mars bleibt in Fische und der Aszendent wandert — nicht umgekehrt. Beim ersten Versuch
        // stand Mars für Haus 8 in Steinbock, also erhöht, und die Aufhebung setzte den Grad auf
        // „none": der Test hätte eine Regel geprüft, die gar nicht zum Zuge kam. Fische trägt
        // keine der Zeichen-Aufhebungen, egal in welchem Haus es landet.
        Rashi mars = Rashi.MEENA;
        Rashi quiet = Rashi.byIndex(mars.getIndex() - 9); // von dort steht Mars im 10. Haus
        for (int house : new int[]{7, 8}) {
            Rashi ascendant = Rashi.byIndex(mars.getIndex() - (house - 1));
            ManglikResultDTO r = ManglikCalculator.calculate(
                    chart(ascendant, mars, quiet, quiet, quiet));
            assertTrue(r.getCancellations().isEmpty(), "Haus " + house + ": " + r.getCancellations());
            assertEquals("strong", r.getSeverity(), "Haus " + house);
        }
        for (int house : new int[]{1, 2, 4, 12}) {
            Rashi ascendant = Rashi.byIndex(mars.getIndex() - (house - 1));
            ManglikResultDTO r = ManglikCalculator.calculate(
                    chart(ascendant, mars, quiet, quiet, quiet));
            assertTrue(r.getCancellations().isEmpty(), "Haus " + house + ": " + r.getCancellations());
            assertEquals("moderate", r.getSeverity(), "Haus " + house);
        }
    }

    @Test
    @DisplayName("Aufhebung: Mars im eigenen Zeichen")
    void cancellationOwnSign() {
        for (Rashi ownSign : new Rashi[]{Rashi.MESHA, Rashi.VRISHCHIKA}) {
            // Aszendent so legen, dass Mars im 7. Haus steht → sicher ein Befund.
            Rashi ascendant = Rashi.byIndex(ownSign.getIndex() - 6);
            Rashi quiet = Rashi.byIndex(ownSign.getIndex() - 9);
            ManglikResultDTO r = ManglikCalculator.calculate(
                    chart(ascendant, ownSign, quiet, quiet, quiet));
            assertTrue(r.isManglik(), ownSign.toString());
            assertTrue(r.getCancellations().contains("mars_own_sign"), ownSign.toString());
            assertFalse(r.isManglikAfterCancellation(), ownSign.toString());
            assertEquals("none", r.getSeverity());
        }
    }

    @Test
    @DisplayName("Aufhebung: Mars erhöht in Steinbock")
    void cancellationExalted() {
        Rashi mars = Rashi.MAKARA;
        Rashi ascendant = Rashi.byIndex(mars.getIndex() - 6); // Mars im 7. Haus
        Rashi quiet = Rashi.byIndex(mars.getIndex() - 9);
        ManglikResultDTO r = ManglikCalculator.calculate(chart(ascendant, mars, quiet, quiet, quiet));
        assertTrue(r.isManglik());
        assertTrue(r.getCancellations().contains("mars_exalted"));
        assertFalse(r.isManglikAfterCancellation());
    }

    @Test
    @DisplayName("Aufhebung: Mars im 1. Haus in Widder, Löwe oder Wassermann")
    void cancellationFirstHouseSign() {
        for (Rashi sign : new Rashi[]{Rashi.MESHA, Rashi.SIMHA, Rashi.KUMBHA}) {
            Rashi quiet = Rashi.byIndex(sign.getIndex() - 9);
            ManglikResultDTO r = ManglikCalculator.calculate(chart(sign, sign, quiet, quiet, quiet));
            assertEquals(1, ref(r, "lagna").getMarsHouse(), sign.toString());
            assertTrue(r.getCancellations().contains("mars_first_house_sign"), sign.toString());
            assertFalse(r.isManglikAfterCancellation(), sign.toString());
        }
        // Gegenprobe: Mars im 1. Haus in Fische hebt nicht auf.
        Rashi quiet = Rashi.MITHUNA;
        ManglikResultDTO r = ManglikCalculator.calculate(
                chart(Rashi.MEENA, Rashi.MEENA, quiet, quiet, quiet));
        assertFalse(r.getCancellations().contains("mars_first_house_sign"));
    }

    @Test
    @DisplayName("Aufhebung: Mars im 2. Haus in Zwillinge oder Jungfrau")
    void cancellationSecondHouseSign() {
        for (Rashi sign : new Rashi[]{Rashi.MITHUNA, Rashi.KANYA}) {
            Rashi ascendant = Rashi.byIndex(sign.getIndex() - 1);
            Rashi quiet = Rashi.byIndex(sign.getIndex() - 9);
            ManglikResultDTO r = ManglikCalculator.calculate(chart(ascendant, sign, quiet, quiet, quiet));
            assertEquals(2, ref(r, "lagna").getMarsHouse(), sign.toString());
            assertTrue(r.getCancellations().contains("mars_second_house_sign"), sign.toString());
        }
    }

    @Test
    @DisplayName("Aufhebung: Mars im 12. Haus in Stier oder Waage")
    void cancellationTwelfthHouseSign() {
        for (Rashi sign : new Rashi[]{Rashi.VRISHABHA, Rashi.TULA}) {
            Rashi ascendant = Rashi.byIndex(sign.getIndex() + 1);
            Rashi quiet = Rashi.byIndex(sign.getIndex() - 9);
            ManglikResultDTO r = ManglikCalculator.calculate(chart(ascendant, sign, quiet, quiet, quiet));
            assertEquals(12, ref(r, "lagna").getMarsHouse(), sign.toString());
            assertTrue(r.getCancellations().contains("mars_twelfth_house_sign"), sign.toString());
        }
    }

    @Test
    @DisplayName("Aufhebung: Jupiters Graha-Drishti auf Mars (Konjunktion, 5., 7., 9. Zeichen)")
    void cancellationJupiterDrishti() {
        Rashi mars = Rashi.MEENA;
        Rashi ascendant = Rashi.byIndex(mars.getIndex() - 6); // Mars im 7. Haus
        Rashi quiet = Rashi.byIndex(mars.getIndex() - 9);
        // Jupiter sieht Mars, wenn Mars 1, 5, 7 oder 9 Zeichen von Jupiter entfernt steht.
        for (int distance : new int[]{1, 5, 7, 9}) {
            Rashi jupiter = Rashi.byIndex(mars.getIndex() - (distance - 1));
            ManglikResultDTO r = ManglikCalculator.calculate(
                    chart(ascendant, mars, quiet, quiet, jupiter));
            assertTrue(r.getCancellations().contains("jupiter_aspects_mars"),
                    "Abstand " + distance + " (Jupiter in " + jupiter + ")");
        }
        // Gegenprobe: Abstände ohne Drishti
        for (int distance : new int[]{2, 3, 4, 6, 8, 10, 11, 12}) {
            Rashi jupiter = Rashi.byIndex(mars.getIndex() - (distance - 1));
            ManglikResultDTO r = ManglikCalculator.calculate(
                    chart(ascendant, mars, quiet, quiet, jupiter));
            assertFalse(r.getCancellations().contains("jupiter_aspects_mars"),
                    "Abstand " + distance + " darf nicht aufheben");
        }
    }

    @Test
    @DisplayName("Aufhebung: Venus im selben Zeichen wie Mars")
    void cancellationVenusConjunction() {
        Rashi mars = Rashi.MEENA;
        Rashi ascendant = Rashi.byIndex(mars.getIndex() - 6);
        Rashi jupiterQuiet = Rashi.byIndex(mars.getIndex() - 9);
        ManglikResultDTO r = ManglikCalculator.calculate(
                chart(ascendant, mars, jupiterQuiet, mars, jupiterQuiet));
        assertTrue(r.getCancellations().contains("venus_conjunct_mars"));
        assertFalse(r.isManglikAfterCancellation());
    }

    @Test
    @DisplayName("Alle bekannten Aufhebungsregeln werden gemeldet, auch die, die nicht greifen")
    void knownCancellationsAreAlwaysListed() {
        ManglikResultDTO r = ManglikCalculator.calculate(neutralChart());
        assertEquals(ManglikCalculator.KNOWN_CANCELLATIONS, r.getCancellationsChecked());
        assertTrue(r.getCancellations().isEmpty());
        assertEquals(7, ManglikCalculator.KNOWN_CANCELLATIONS.size());
    }

    @Test
    @DisplayName("Paarbezogene Aufhebung greift nur, wenn beide einen Befund tragen")
    void mutualCancellation() {
        Rashi mars = Rashi.MEENA;
        Rashi ascendant = Rashi.byIndex(mars.getIndex() - 6);
        Rashi quiet = Rashi.byIndex(mars.getIndex() - 9);
        ManglikResultDTO afflicted = ManglikCalculator.calculate(
                chart(ascendant, mars, quiet, quiet, quiet));
        ManglikResultDTO clean = ManglikCalculator.calculate(neutralChart());

        assertTrue(afflicted.isManglik());
        assertFalse(clean.isManglik());
        assertTrue(ManglikCalculator.mutualCancellation(afflicted, afflicted));
        assertFalse(ManglikCalculator.mutualCancellation(afflicted, clean));
        assertFalse(ManglikCalculator.mutualCancellation(clean, clean));
    }

    @Test
    @DisplayName("Die Mars-Stellung wird immer mitgeliefert, nicht nur ein Ja/Nein")
    void marsPositionIsAlwaysReported() {
        ManglikResultDTO r = ManglikCalculator.calculate(neutralChart());
        assertEquals("pisces", r.getMarsRashi());
        assertTrue(r.getMarsSiderealLongitude() > 0);
        assertNotNull(r.getMarsDegreeInSignFormatted());
        assertNotNull(r.getMarsNavamsaRashi());
        assertNotNull(r.getMarsNakshatra());
        assertNotNull(r.getMarsNakshatra().getKey());
    }

    @Test
    @DisplayName("Ohne Lagna bleiben nur Mond und Venus — und die hausgebundenen Aufhebungen greifen nicht")
    void withoutLagnaOnlyMoonAndVenus() {
        // Mars im 2. Haus in Zwillingen: MIT Lagna greift mars_second_house_sign, OHNE nicht.
        Rashi mars = Rashi.MITHUNA;
        Rashi ascendant = Rashi.byIndex(mars.getIndex() - 1);
        Rashi quiet = Rashi.byIndex(mars.getIndex() - 9);

        ManglikResultDTO withLagna = ManglikCalculator.calculate(
                chart(ascendant, mars, quiet, quiet, quiet));
        assertTrue(withLagna.isLagnaAvailable());
        assertTrue(withLagna.getCancellations().contains("mars_second_house_sign"));

        VedicChartDTO noTime = chart(ascendant, mars, quiet, quiet, quiet);
        noTime.setTimeDependentAvailable(false);
        noTime.setAscendantRashiIndex(null);
        noTime.setAscendantRashi(null);
        ManglikResultDTO withoutLagna = ManglikCalculator.calculate(noTime);

        assertFalse(withoutLagna.isLagnaAvailable());
        assertEquals(2, withoutLagna.getReferences().size());
        assertEquals("moon", ref(withoutLagna, "moon").getReference());
        assertEquals("venus", ref(withoutLagna, "venus").getReference());
        assertTrue(withoutLagna.getReferences().stream()
                .noneMatch(r -> "lagna".equals(r.getReference())));
        assertFalse(withoutLagna.getCancellations().contains("mars_second_house_sign"),
                "Eine hausgebundene Aufhebung ohne Haus wäre geraten");
        assertNotEquals("strong", withoutLagna.getSeverity());
        assertNotEquals("moderate", withoutLagna.getSeverity());
    }

    @Test
    @DisplayName("Ein Chart ohne Mars ist ein Programmfehler, kein stilles Ergebnis")
    void missingMarsFailsLoudly() {
        VedicChartDTO c = new VedicChartDTO();
        c.setAscendantRashiIndex(0);
        c.setPlanets(List.of());
        assertThrows(IllegalStateException.class, () -> ManglikCalculator.calculate(c));
    }
}
