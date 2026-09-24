package astroapi.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sichert die Asteroiden-Erweiterung an zwei Stellen ab, an denen sie still scheitern würde.
 *
 * <p>Erstens die <b>Daten</b>: ein Körper, für den keine Ephemeridendatei im Image liegt, wirft
 * keine Ausnahme — {@code swe_calc_ut} meldet einen Fehler, der Helper protokolliert ihn und legt
 * {@code NaN} in die Karte. Im Chart stünde der Körper dann mit einem leeren Wert, und auffallen
 * würde es erst der Nutzerin. Deshalb wird hier tatsächlich gerechnet.
 *
 * <p>Zweitens die <b>Filter</b>: sie sind der einzige Grund, warum sechs zusätzliche Körper das
 * Bild nicht zumüllen.
 */
class BodiesTest {

    private static SwissEphemerisHelper swe;

    @BeforeAll
    static void setup() {
        swe = new SwissEphemerisHelper();
        // Der Schalter steht im Betrieb auf AUS. Hier geht es aber gerade darum, ob die
        // Ephemeridendateien die zusätzlichen Körper überhaupt hergeben — also eingeschaltet
        // prüfen, sonst prüft der Test nichts.
        ReflectionTestUtils.setField(swe, "extendedBodies", true);
    }

    @Test
    void everyExtendedBodyResolvesAgainstTheShippedEphemerisFiles() {
        double julianDay = swe.calculateJulianDay(LocalDateTime.of(1990, 5, 17, 12, 30));
        Map<String, Double> positions = swe.calculatePlanetPositions(julianDay);

        Set<String> broken = new TreeSet<>();
        for (Planet body : Bodies.EXTENDED) {
            Double longitude = positions.get(body.getName());
            if (longitude == null || Double.isNaN(longitude) || longitude < 0 || longitude >= 360) {
                broken.add(body.getName() + "=" + longitude);
            }
        }

        assertTrue(broken.isEmpty(),
                "Für diese Körper fehlt die Ephemeridendatei in ephe/ — sie landen als NaN im Chart: "
                        + broken);
    }

    /**
     * Gerechnet wird immer mit.
     *
     * <p>Sichtbar sind die Asteroiden deshalb noch lange nicht — das entscheidet gAIster anhand
     * der Einstellung der Nutzerin. Rechnen muss die AstroAPI sie trotzdem: gespeicherte Charts
     * werden beim Öffnen nicht neu berechnet, ein späteres Einschalten käme sonst zu spät.
     */
    @Test
    void extendedBodiesAreAlwaysCalculated() {
        SwissEphemerisHelper fresh = new SwissEphemerisHelper();
        assertEquals(true, ReflectionTestUtils.getField(fresh, "extendedBodies"));
        fresh.shutdown();
    }

    /**
     * Der Feldwert oben sagt nichts über den Betrieb: dort gewinnt {@code application.yml}.
     *
     * <p>Genau das war schon einmal auseinandergelaufen — im Feld stand {@code true}, in der yml
     * {@code false}, und in Produktion wäre nichts passiert. Ein Test, der nur das Java-Feld
     * prüft, merkt davon nichts.
     */
    @Test
    void theConfiguredDefaultMatchesTheFieldDefault() throws Exception {
        String yml = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/application.yml"),
                java.nio.charset.StandardCharsets.UTF_8);

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("extended:\\s*\\$\\{ASTRO_BODIES_EXTENDED:(true|false)}")
                .matcher(yml);

        assertTrue(matcher.find(), "astro.bodies.extended fehlt in application.yml");
        assertEquals("true", matcher.group(1),
                "application.yml schaltet die erweiterten Körper ab — dann greift der "
                        + "Sichtbarkeitsfilter in gAIster ins Leere, weil gar nichts gerechnet wird");
    }

    @Test
    void theClassicSetIsUnchangedWhenTheSwitchIsOff() {
        assertEquals(Bodies.CLASSIC, Bodies.forChart(false));
        assertEquals(13, Bodies.CLASSIC.size());
        for (Planet extended : Bodies.EXTENDED) {
            assertFalse(Bodies.CLASSIC.contains(extended), extended + " gehört nicht zum klassischen Satz");
        }
    }

    @Test
    void chironStaysClassic() {
        // Chiron war immer dabei und steckt in allen bestehenden Texten und Deutungen. Stünde er
        // unter den Filtern, verschwänden bestehende Aspekte aus bereits gezeigten Horoskopen.
        assertFalse(Bodies.isExtended(Planet.CHIRON));
        assertTrue(Bodies.aspectAllowed("Chiron", "Neptune", "Quincunx", 2.0));
    }

    @Test
    void anExtendedBodyOnlyAspectsPersonalPoints() {
        assertTrue(Bodies.aspectAllowed("Ceres", "Sun", "Conjunction", 1.0));
        assertTrue(Bodies.aspectAllowed("Vesta", "Ascendant", "Square", 0.5));
        assertTrue(Bodies.aspectAllowed("Medium Coeli", "Juno", "Trine", 0.9));

        assertFalse(Bodies.aspectAllowed("Ceres", "Neptune", "Trine", 0.2));
        assertFalse(Bodies.aspectAllowed("Pholus", "Jupiter", "Conjunction", 0.1));
    }

    @Test
    void twoExtendedBodiesNeverFormAnAspect() {
        assertFalse(Bodies.aspectAllowed("Ceres", "Vesta", "Conjunction", 0.0));
        assertFalse(Bodies.aspectAllowed("Mean Lilith", "Juno", "Opposition", 0.0));
    }

    @Test
    void onlyMajorAspectsAndOnlyWithATightOrb() {
        assertFalse(Bodies.aspectAllowed("Juno", "Venus", "Quincunx", 0.1));
        assertFalse(Bodies.aspectAllowed("Juno", "Venus", "Semisextile", 0.1));

        assertTrue(Bodies.aspectAllowed("Juno", "Venus", "Sextile", 1.0));
        assertFalse(Bodies.aspectAllowed("Juno", "Venus", "Sextile", 1.01));

        // Die Konjunktion darf etwas weiter sein als die übrigen.
        assertTrue(Bodies.aspectAllowed("Juno", "Venus", "Conjunction", 1.4));
        assertFalse(Bodies.aspectAllowed("Juno", "Venus", "Conjunction", 1.6));
    }

    @Test
    void classicPairsAreUntouchedByThePolicy() {
        assertTrue(Bodies.aspectAllowed("Mars", "Saturn", "Quincunx", 2.0));
        assertTrue(Bodies.aspectAllowed("Sun", "Pluto", "Novile", 0.8));
    }

    @Test
    void slowBodiesGetACoarserScanGrid() {
        assertEquals(2.0, Bodies.scanStepDays(Planet.CERES, 0.5));
        assertEquals(0.5, Bodies.scanStepDays(Planet.MARS, 0.5));
    }
}
