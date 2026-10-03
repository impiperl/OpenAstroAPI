package astroapi.vedic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Navamsa D9.
 *
 * <p>Der Kern: die durchlaufende Formel und die klassische Fallunterscheidung nach Bewegungsart
 * müssen überall dasselbe liefern. Stimmen sie auf allen 108 Navamsas überein, ist die Formel
 * belegt und nicht nur plausibel — und ein späterer Umbau fällt sofort auf.
 */
class NavamsaTest {

    @Test
    @DisplayName("Formel und klassische Regel stimmen auf allen 108 Navamsas überein")
    void formulaMatchesClassicalRule() {
        for (int rashiIndex = 0; rashiIndex < 12; rashiIndex++) {
            Rashi rashi = Rashi.byIndex(rashiIndex);
            for (int division = 0; division < 9; division++) {
                // Mitte des Abschnitts, damit kein Grenzfall die Aussage verwässert.
                double degreeInSign = (division + 0.5) * VedicMath.PADA_SPAN;
                double longitude = rashiIndex * 30.0 + degreeInSign;

                Rashi byFormula = Navamsa.signOf(longitude);
                Rashi byRule = Navamsa.signIndexFromRule(rashi, degreeInSign);
                assertEquals(byRule, byFormula,
                        rashi + " Abschnitt " + (division + 1) + " (bei " + longitude + "°)");
            }
        }
    }

    @Test
    @DisplayName("Beweglich beginnt beim Zeichen selbst")
    void movableStartsAtOwnSign() {
        assertEquals(Rashi.MESHA, Navamsa.signOf(0.0));                 // 0° Widder
        assertEquals(Rashi.KARKA, Navamsa.signOf(3 * 30.0));            // 0° Krebs
        assertEquals(Rashi.TULA, Navamsa.signOf(6 * 30.0));             // 0° Waage
        assertEquals(Rashi.MAKARA, Navamsa.signOf(9 * 30.0));           // 0° Steinbock
    }

    @Test
    @DisplayName("Fest beginnt beim 9. Zeichen")
    void fixedStartsAtNinthSign() {
        // Stier → 9. Zeichen von Stier = Steinbock
        assertEquals(Rashi.MAKARA, Navamsa.signOf(1 * 30.0));
        // Löwe → Widder (9. Zeichen von Löwe inklusiv gezählt: Löwe…Widder)
        assertEquals(Rashi.MESHA, Navamsa.signOf(4 * 30.0));
        // Skorpion → Krebs
        assertEquals(Rashi.KARKA, Navamsa.signOf(7 * 30.0));
        // Wassermann → Waage
        assertEquals(Rashi.TULA, Navamsa.signOf(10 * 30.0));
    }

    @Test
    @DisplayName("Zweigestaltig beginnt beim 5. Zeichen")
    void dualStartsAtFifthSign() {
        // Zwillinge → 5. Zeichen von Zwillinge = Waage
        assertEquals(Rashi.TULA, Navamsa.signOf(2 * 30.0));
        // Jungfrau → Steinbock
        assertEquals(Rashi.MAKARA, Navamsa.signOf(5 * 30.0));
        // Schütze → Widder
        assertEquals(Rashi.MESHA, Navamsa.signOf(8 * 30.0));
        // Fische → Krebs
        assertEquals(Rashi.KARKA, Navamsa.signOf(11 * 30.0));
    }

    @Test
    @DisplayName("Innerhalb eines Zeichens laufen die neun Navamsas lückenlos weiter")
    void nineConsecutiveDivisions() {
        // Widder: Widder … Schütze
        Rashi[] expected = {
            Rashi.MESHA, Rashi.VRISHABHA, Rashi.MITHUNA, Rashi.KARKA, Rashi.SIMHA,
            Rashi.KANYA, Rashi.TULA, Rashi.VRISHCHIKA, Rashi.DHANU,
        };
        for (int i = 0; i < 9; i++) {
            double lon = (i + 0.5) * VedicMath.PADA_SPAN;
            assertEquals(expected[i], Navamsa.signOf(lon), "Abschnitt " + (i + 1));
            assertEquals(i + 1, Navamsa.divisionOf(lon), "Abschnittsnummer bei " + lon);
        }
    }

    @Test
    @DisplayName("Das letzte Navamsa eines zweigestaltigen Zeichens endet beim Zeichen selbst")
    void lastNavamsaOfDualSign() {
        // Fische, 9. Navamsa: beginnt bei Krebs, also Krebs+8 = Fische.
        double lon = 11 * 30.0 + 8.5 * VedicMath.PADA_SPAN;
        assertEquals(Rashi.MEENA, Navamsa.signOf(lon));
        assertEquals(9, Navamsa.divisionOf(lon));
    }

    @Test
    @DisplayName("Grenzen der Navamsas: davor, darauf, danach")
    void navamsaBoundaries() {
        double eps = 1e-7;
        for (int n = 0; n < 108; n++) {
            double boundary = n * VedicMath.PADA_SPAN;
            Rashi atBoundary = Navamsa.signOf(boundary);
            Rashi after = Navamsa.signOf(boundary + eps);
            assertEquals(atBoundary, after, "Navamsa-Grenze " + n);
            assertEquals(Rashi.byIndex(n % 12), atBoundary, "Navamsa " + n);
            if (n > 0) {
                assertEquals(Rashi.byIndex((n - 1) % 12), Navamsa.signOf(boundary - eps),
                        "vor Navamsa-Grenze " + n);
            }
        }
    }

    @Test
    @DisplayName("0° und 360° ergeben dasselbe Navamsa")
    void wrapAround() {
        assertEquals(Navamsa.signOf(0.0), Navamsa.signOf(360.0));
        assertEquals(Rashi.MEENA, Navamsa.signOf(360.0 - 1e-7));
        assertEquals(Rashi.MEENA, Navamsa.signOf(-1e-7));
    }

    @Test
    @DisplayName("Die vier Startzeichen sind immer die beweglichen Zeichen")
    void startSignsAreTheMovableOnes() {
        // Egal welche Bewegungsart: eine Navamsa-Reihe beginnt immer bei Widder, Krebs, Waage oder
        // Steinbock. Das ist die Gegenprobe zur Fallunterscheidung — fällt sie, ist eine der drei
        // Startregeln verrutscht.
        java.util.Set<Rashi> movable = java.util.EnumSet.of(
                Rashi.MESHA, Rashi.KARKA, Rashi.TULA, Rashi.MAKARA);
        for (int i = 0; i < 12; i++) {
            Rashi start = Navamsa.signOf(i * 30.0);
            assertTrue(movable.contains(start),
                    "Navamsa-Reihe von " + Rashi.byIndex(i) + " beginnt bei " + start);
        }
    }

    @Test
    @DisplayName("Ein Navamsa ist so breit wie ein Pada — dieselbe Neunteilung")
    void navamsaEqualsPadaWidth() {
        assertEquals(VedicMath.PADA_SPAN, 30.0 / 9.0, 1e-12);
        assertEquals(VedicMath.NAKSHATRA_SPAN / 4.0, 30.0 / 9.0, 1e-12);
    }
}
