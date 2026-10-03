package astroapi.vedic;

import astroapi.vedic.dto.NakshatraPositionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grenzen. Nur Grenzen.
 *
 * <p>Nakshatra und Pada sind reine Intervallzuordnungen, und genau dort geht so etwas schief: ein
 * {@code <=} statt {@code <}, eine Normalisierung, die 360° durchlässt, ein Index, der durch
 * Fließkomma-Aufrundung auf 27 statt 26 landet. Deshalb wird hier jede der 27 Nakshatra-Grenzen
 * und jede der 108 Pada-Grenzen unmittelbar davor, genau darauf und unmittelbar danach geprüft.
 */
class NakshatraBoundaryTest {

    /** Ein Hauch unter einer Grenze — klein genug, um dieselbe Grenze zu meinen. */
    private static final double EPS = 1e-7;

    @Test
    @DisplayName("0° und 360° sind dasselbe und ergeben Ashwini, Pada 1")
    void zeroAndThreeSixty() {
        for (double lon : new double[]{0.0, 360.0, -360.0, 720.0}) {
            NakshatraPositionDTO p = position(lon);
            assertEquals(1, p.getIndex(), "Länge " + lon);
            assertEquals(1, p.getPada(), "Länge " + lon);
            assertEquals(0.0, p.getDegreeInNakshatra(), 1e-9, "Länge " + lon);
            assertEquals(0.0, p.getSiderealLongitude(), 1e-9, "Länge " + lon);
        }
    }

    @Test
    @DisplayName("Knapp unter 360° ist noch Revati, Pada 4 — nicht ein 28. Nakshatra")
    void justBelowFullCircle() {
        NakshatraPositionDTO p = position(360.0 - EPS);
        assertEquals(27, p.getIndex());
        assertEquals(4, p.getPada());
        assertTrue(p.getRemainingDegrees() > 0 && p.getRemainingDegrees() < 1e-5,
                "Restweg muss winzig, aber positiv sein: " + p.getRemainingDegrees());
    }

    @Test
    @DisplayName("Negative Längen werden korrekt in den Kreis geholt")
    void negativeLongitudes() {
        assertEquals(27, position(-EPS).getIndex());
        assertEquals(27, position(-1.0).getIndex());
        assertEquals(1, position(-360.0 + 1.0).getIndex());
    }

    @Test
    @DisplayName("Jede der 27 Nakshatra-Grenzen: davor, darauf, danach")
    void everyNakshatraBoundary() {
        for (int i = 0; i < 27; i++) {
            double boundary = i * VedicMath.NAKSHATRA_SPAN;
            int expectedAtOrAfter = i + 1;
            int expectedBefore = i == 0 ? 27 : i;

            assertEquals(expectedBefore, position(boundary - EPS).getIndex(),
                    "unmittelbar vor Grenze " + i);
            assertEquals(expectedAtOrAfter, position(boundary).getIndex(),
                    "genau auf Grenze " + i);
            assertEquals(expectedAtOrAfter, position(boundary + EPS).getIndex(),
                    "unmittelbar nach Grenze " + i);

            // Auf der Grenze beginnt immer Pada 1, davor endet immer Pada 4.
            assertEquals(1, position(boundary).getPada(), "Pada auf Grenze " + i);
            assertEquals(4, position(boundary - EPS).getPada(), "Pada vor Grenze " + i);
        }
    }

    @Test
    @DisplayName("Alle 108 Pada-Grenzen liegen richtig")
    void everyPadaBoundary() {
        for (int p = 0; p < 108; p++) {
            double boundary = p * VedicMath.PADA_SPAN;
            int expectedPada = (p % 4) + 1;
            int expectedNakshatra = (p / 4) + 1;

            NakshatraPositionDTO atBoundary = position(boundary);
            assertEquals(expectedPada, atBoundary.getPada(), "Pada auf Grenze " + p);
            assertEquals(expectedNakshatra, atBoundary.getIndex(), "Nakshatra auf Grenze " + p);

            NakshatraPositionDTO after = position(boundary + EPS);
            assertEquals(expectedPada, after.getPada(), "Pada nach Grenze " + p);

            if (p > 0) {
                NakshatraPositionDTO before = position(boundary - EPS);
                int expectedPrevPada = ((p - 1) % 4) + 1;
                assertEquals(expectedPrevPada, before.getPada(), "Pada vor Grenze " + p);
            }
        }
    }

    @Test
    @DisplayName("Restweg und gelaufener Weg ergänzen sich immer zur Nakshatra-Breite")
    void remainingPlusTravelled() {
        for (double lon = 0; lon < 360; lon += 0.37) {
            NakshatraPositionDTO p = position(lon);
            assertEquals(VedicMath.NAKSHATRA_SPAN,
                    p.getDegreeInNakshatra() + p.getRemainingDegrees(), 1e-9, "Länge " + lon);
            assertTrue(p.getRemainingDegreesInPada() > -1e-9
                            && p.getRemainingDegreesInPada() <= VedicMath.PADA_SPAN + 1e-9,
                    "Pada-Restweg außerhalb des Bereichs bei " + lon + ": "
                            + p.getRemainingDegreesInPada());
        }
    }

    @Test
    @DisplayName("Jede Länge ergibt genau ein gültiges Nakshatra und ein gültiges Pada")
    void sweepIsAlwaysValid() {
        for (double lon = 0; lon < 360; lon += 0.013) {
            NakshatraPositionDTO p = position(lon);
            assertTrue(p.getIndex() >= 1 && p.getIndex() <= 27, "Index bei " + lon + ": " + p.getIndex());
            assertTrue(p.getPada() >= 1 && p.getPada() <= 4, "Pada bei " + lon + ": " + p.getPada());
        }
    }

    @Test
    @DisplayName("Normalisierung lässt 360 nie als 360 stehen")
    void normalizeNeverReturnsThreeSixty() {
        assertEquals(0.0, VedicMath.normalizeDegrees(360.0), 0.0);
        assertEquals(0.0, VedicMath.normalizeDegrees(-0.0), 0.0);
        assertEquals(0.0, VedicMath.normalizeDegrees(720.0), 0.0);
        for (double d : new double[]{-1e-13, -1e-9, 359.9999999999, 360.0000000001}) {
            double n = VedicMath.normalizeDegrees(d);
            assertTrue(n >= 0.0 && n < 360.0, "Normalisierung von " + d + " ergab " + n);
        }
    }

    @Test
    @DisplayName("Gradformatierung rundet nicht auf 60 Minuten oder 60 Sekunden")
    void degreeFormatting() {
        assertEquals("0°00′00″", VedicMath.formatDegreeMinuteSecond(0.0));
        assertEquals("13°20′00″", VedicMath.formatDegreeMinuteSecond(VedicMath.NAKSHATRA_SPAN));
        assertEquals("3°20′00″", VedicMath.formatDegreeMinuteSecond(VedicMath.PADA_SPAN));
        // 29,99999° darf nicht als 29°59′60″ erscheinen.
        assertEquals("30°00′00″", VedicMath.formatDegreeMinuteSecond(29.9999999));
        assertEquals("1°00′00″", VedicMath.formatDegreeMinuteSecond(0.99999999));
    }

    @Test
    @DisplayName("Nakshatra-Zählung ist 1-basiert, inklusiv und läuft über 27 hinweg um")
    void nakshatraCounting() {
        assertEquals(1, VedicMath.countNakshatras(1, 1));
        assertEquals(27, VedicMath.countNakshatras(1, 27));
        assertEquals(2, VedicMath.countNakshatras(27, 1));
        assertEquals(1, VedicMath.countNakshatras(14, 14));
        for (int from = 1; from <= 27; from++) {
            for (int to = 1; to <= 27; to++) {
                int c = VedicMath.countNakshatras(from, to);
                assertTrue(c >= 1 && c <= 27, from + "→" + to + " = " + c);
            }
        }
    }

    private static NakshatraPositionDTO position(double longitude) {
        return VedicEphemerisService.nakshatraPosition(longitude);
    }
}
