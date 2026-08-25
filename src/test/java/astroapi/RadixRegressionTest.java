package astroapi;

import astroapi.dto.EphiDatenDTO;
import astroapi.utils.HoroscopeType;
import astroapi.utils.HouseSystem;
import astroapi.utils.SwissEphemerisHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the radix calculation (Swiss Ephemeris + house system).
 *
 * The reference times are the birth's LOCAL wall-clock time already converted to UT using the
 * correct HISTORICAL UTC offset (incl. summer time) — the same conversion {@code AstroService.toUt}
 * performs at runtime. Sun positions are asserted by zodiac sign, which is astronomically well
 * defined and robust across ephemeris backends, so these act as stable reference charts.
 */
class RadixRegressionTest {

    private static SwissEphemerisHelper swe;

    @BeforeAll
    static void setup() {
        swe = new SwissEphemerisHelper();
    }

    private static final String[] SIGNS = {
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
    };

    private static int signIndex(double lon) {
        double l = ((lon % 360) + 360) % 360;
        return (int) Math.floor(l / 30) % 12;
    }

    private static double angularDiff(double a, double b) {
        double d = Math.abs(((a - b) % 360 + 360) % 360);
        return Math.min(d, 360 - d);
    }

    private static final class RefBirth {
        final String name;
        final LocalDateTime ut;
        final double lat;
        final double lon;
        final int sunSign;
        RefBirth(String name, LocalDateTime ut, double lat, double lon, int sunSign) {
            this.name = name; this.ut = ut; this.lat = lat; this.lon = lon; this.sunSign = sunSign;
        }
    }

    // Local birth time → historical offset → UT. Sun's expected zodiac sign in the last column.
    private static final List<RefBirth> BIRTHS = List.of(
        new RefBirth("Graz 1976-01-27 01:50 CET(+1)",   LocalDateTime.of(1976, 1, 27, 0, 50), 47.0708678, 15.4382786, 10), // Aquarius
        new RefBirth("Wien 1990-05-15 12:00 CEST(+2)",  LocalDateTime.of(1990, 5, 15, 10, 0), 48.2082, 16.3738, 1),        // Taurus
        new RefBirth("Wien 1985-11-10 12:00 CET(+1)",   LocalDateTime.of(1985, 11, 10, 11, 0), 48.2082, 16.3738, 7),       // Scorpio
        new RefBirth("NYC 2000-07-04 12:00 EDT(-4)",    LocalDateTime.of(2000, 7, 4, 16, 0), 40.7128, -74.0060, 3),        // Cancer
        new RefBirth("London 1995-08-10 12:00 BST(+1)", LocalDateTime.of(1995, 8, 10, 11, 0), 51.5074, -0.1278, 4)         // Leo
    );

    @Test
    void referenceChartsAreConsistent() {
        for (RefBirth b : BIRTHS) {
            EphiDatenDTO dto = swe.createEphiDatenDTO(b.ut, b.lat, b.lon, HouseSystem.PLACIDUS, HoroscopeType.NATAL);

            Map<String, Double> planets = dto.getPlanets();
            assertNotNull(planets, b.name + ": planets null");
            assertTrue(planets.containsKey("Sun"), b.name + ": Sun missing");
            assertTrue(planets.containsKey("Moon"), b.name + ": Moon missing");

            // Sun must be in the expected zodiac sign.
            assertEquals(SIGNS[b.sunSign], SIGNS[signIndex(planets.get("Sun"))],
                    b.name + ": Sun sign wrong (lon=" + planets.get("Sun") + ")");

            // Every planet longitude within [0, 360).
            for (Map.Entry<String, Double> e : planets.entrySet()) {
                double v = e.getValue();
                assertTrue(v >= 0 && v < 360, b.name + ": " + e.getKey() + " out of range: " + v);
            }

            // Structural invariants of the house frame.
            List<Double> cusps = dto.getCusps();
            assertEquals(12, cusps.size(), b.name + ": expected 12 house cusps");

            double asc = dto.getAscendant();
            double mc = dto.getMediumCoeli();
            assertTrue(asc >= 0 && asc < 360, b.name + ": ASC out of range: " + asc);
            assertTrue(mc >= 0 && mc < 360, b.name + ": MC out of range: " + mc);

            // Quadrant system: 1st cusp == Ascendant, 10th cusp == MC.
            assertTrue(angularDiff(asc, cusps.get(0)) < 0.1, b.name + ": ASC != cusp[0]");
            assertTrue(angularDiff(mc, cusps.get(9)) < 0.1, b.name + ": MC != cusp[9]");
        }
    }

    @Test
    void supportedUiHouseSystemsProduceTwelveCusps() {
        for (char code : new char[]{'P','W','A','K','O','R','C','B','T'}) {
            EphiDatenDTO dto = swe.createEphiDatenDTO(LocalDateTime.of(2026, 8, 25, 12, 0),
                    48.2082, 16.3738, HouseSystem.fromCode(code), HoroscopeType.NATAL);
            assertEquals(12, dto.getCusps().size(), "house system " + code);
            assertEquals(String.valueOf(code), dto.getRequestedHouseSystem());
        }
    }

    @Test
    void placidusAtPolarLatitudeFallsBackExplicitlyToPorphyry() {
        EphiDatenDTO dto = swe.createEphiDatenDTO(LocalDateTime.of(2026, 8, 25, 12, 0),
                80.0, 20.0, HouseSystem.PLACIDUS, HoroscopeType.NATAL);
        assertEquals("P", dto.getRequestedHouseSystem());
        assertEquals("O", dto.getEffectiveHouseSystem());
        assertTrue(dto.isHouseSystemFallback());
        assertEquals(12, dto.getCusps().size());
    }

    /**
     * Guards the timezone contract (AstroService.toUt): java.time must apply the correct HISTORICAL
     * UTC offset incl. summer time when interpreting a birth's local wall-clock time. If the JVM tz
     * data or the conversion regressed, ascendant/houses would silently shift.
     */
    @Test
    void historicalDaylightSavingOffsetsAreApplied() {
        ZoneId vienna = ZoneId.of("Europe/Vienna");

        // Summer 1990 → CEST (+2): 12:00 local == 10:00 UTC.
        assertEquals(LocalDateTime.of(1990, 5, 15, 10, 0),
                ZonedDateTime.of(LocalDateTime.of(1990, 5, 15, 12, 0), vienna)
                        .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());

        // Winter 1976 → CET (+1): 12:00 local == 11:00 UTC.
        assertEquals(LocalDateTime.of(1976, 1, 15, 11, 0),
                ZonedDateTime.of(LocalDateTime.of(1976, 1, 15, 12, 0), vienna)
                        .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }
}
