package astroapi.vedic;

import astroapi.vedic.GrahaDignity.Level;
import astroapi.vedic.dto.GrahaDignityDTO;
import astroapi.vedic.dto.KaalSarpaDTO;
import astroapi.vedic.dto.StrongestGrahaDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicPlanetDTO;
import astroapi.vedic.dto.VedicWarningDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Würden, stärkster Graha und Kaal Sarpa — gegen synthetische Charts.
 *
 * <p>Wie in {@link ManglikTest}: der Befund hängt an Zeichen und Längen, und so lässt sich jede
 * Regel gezielt auslösen. Die Tabellenwerte stehen in BPHS Kap. 3.
 */
class KundliTest {

    // ── Würden ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Erhöhung und Fall der sieben Grahas nach BPHS")
    void exaltationAndDebilitation() {
        assertEquals(Rashi.MESHA, GrahaDignity.exaltationSign(VedicBody.SUN));
        assertEquals(Rashi.VRISHABHA, GrahaDignity.exaltationSign(VedicBody.MOON));
        assertEquals(Rashi.MAKARA, GrahaDignity.exaltationSign(VedicBody.MARS));
        assertEquals(Rashi.KANYA, GrahaDignity.exaltationSign(VedicBody.MERCURY));
        assertEquals(Rashi.KARKA, GrahaDignity.exaltationSign(VedicBody.JUPITER));
        assertEquals(Rashi.MEENA, GrahaDignity.exaltationSign(VedicBody.VENUS));
        assertEquals(Rashi.TULA, GrahaDignity.exaltationSign(VedicBody.SATURN));

        assertEquals(Rashi.TULA, GrahaDignity.debilitationSign(VedicBody.SUN));
        assertEquals(Rashi.VRISHCHIKA, GrahaDignity.debilitationSign(VedicBody.MOON));
        assertEquals(Rashi.KARKA, GrahaDignity.debilitationSign(VedicBody.MARS));
        assertEquals(Rashi.MEENA, GrahaDignity.debilitationSign(VedicBody.MERCURY));
        assertEquals(Rashi.MAKARA, GrahaDignity.debilitationSign(VedicBody.JUPITER));
        assertEquals(Rashi.KANYA, GrahaDignity.debilitationSign(VedicBody.VENUS));
        assertEquals(Rashi.MESHA, GrahaDignity.debilitationSign(VedicBody.SATURN));
    }

    @Test
    @DisplayName("Mond im Stier: bis 3° erhöht, danach Moolatrikona")
    void moonInTaurusSplitsByDegree() {
        assertEquals(Level.EXALTED, GrahaDignity.ofPosition(VedicBody.MOON, Rashi.VRISHABHA, 2.9));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.MOON, Rashi.VRISHABHA, 3.0));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.MOON, Rashi.VRISHABHA, 29.9));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.MOON, Rashi.KARKA, 12));
    }

    @Test
    @DisplayName("Merkur in der Jungfrau: erhöht bis 15°, Moolatrikona bis 20°, danach eigenes Zeichen")
    void mercuryInVirgoHasThreeDignities() {
        assertEquals(Level.EXALTED, GrahaDignity.ofPosition(VedicBody.MERCURY, Rashi.KANYA, 14.9));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.MERCURY, Rashi.KANYA, 15.0));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.MERCURY, Rashi.KANYA, 19.9));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.MERCURY, Rashi.KANYA, 20.0));
    }

    @Test
    @DisplayName("Moolatrikona endet im Zeichen — danach eigenes Zeichen")
    void moolatrikonaRangesEndInsideTheSign() {
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.SUN, Rashi.SIMHA, 19.9));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.SUN, Rashi.SIMHA, 20.0));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.MARS, Rashi.MESHA, 11.9));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.MARS, Rashi.MESHA, 12.0));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.MARS, Rashi.VRISHCHIKA, 5));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.JUPITER, Rashi.DHANU, 9.9));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.JUPITER, Rashi.DHANU, 10.0));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.VENUS, Rashi.TULA, 14.9));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.VENUS, Rashi.TULA, 15.0));
        assertEquals(Level.MOOLATRIKONA, GrahaDignity.ofPosition(VedicBody.SATURN, Rashi.KUMBHA, 19.9));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.SATURN, Rashi.KUMBHA, 20.0));
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofPosition(VedicBody.SATURN, Rashi.MAKARA, 3));
    }

    @Test
    @DisplayName("Das ganze Erhöhungszeichen ist Erhöhung, das ganze Fallzeichen Fall")
    void wholeSignExaltationAndFall() {
        assertEquals(Level.EXALTED, GrahaDignity.ofPosition(VedicBody.SUN, Rashi.MESHA, 29));
        assertEquals(Level.EXALTED, GrahaDignity.ofPosition(VedicBody.MARS, Rashi.MAKARA, 1));
        assertEquals(Level.DEBILITATED, GrahaDignity.ofPosition(VedicBody.SATURN, Rashi.MESHA, 20));
        assertEquals(Level.DEBILITATED, GrahaDignity.ofPosition(VedicBody.MOON, Rashi.VRISHCHIKA, 3));
    }

    @Test
    @DisplayName("Sonst entscheidet die natürliche Freundschaft zum Zeichenherrscher")
    void otherwiseNaturalFriendship() {
        // Sonne im Schützen: Jupiter ist ihr Freund.
        assertEquals(Level.FRIEND_SIGN, GrahaDignity.ofPosition(VedicBody.SUN, Rashi.DHANU, 10));
        // Sonne im Steinbock: Saturn ist ihr Feind.
        assertEquals(Level.ENEMY_SIGN, GrahaDignity.ofPosition(VedicBody.SUN, Rashi.MAKARA, 10));
        // Sonne in den Zwillingen: Merkur ist ihr neutral.
        assertEquals(Level.NEUTRAL_SIGN, GrahaDignity.ofPosition(VedicBody.SUN, Rashi.MITHUNA, 10));
        // Der Mond hat keine Feinde: Steinbock (Saturn) ist neutral.
        assertEquals(Level.NEUTRAL_SIGN, GrahaDignity.ofPosition(VedicBody.MOON, Rashi.MAKARA, 10));
    }

    @Test
    @DisplayName("Im Navamsa zählt nur das Zeichen: Moolatrikona fällt mit dem eigenen Zeichen zusammen")
    void navamsaUsesSignOnly() {
        assertEquals(Level.OWN_SIGN, GrahaDignity.ofSign(VedicBody.SUN, Rashi.SIMHA));
        assertEquals(Level.EXALTED, GrahaDignity.ofSign(VedicBody.MOON, Rashi.VRISHABHA));
        assertEquals(Level.EXALTED, GrahaDignity.ofSign(VedicBody.MERCURY, Rashi.KANYA));
        assertEquals(Level.DEBILITATED, GrahaDignity.ofSign(VedicBody.JUPITER, Rashi.MAKARA));
    }

    @Test
    @DisplayName("Die Knoten werden nicht bewertet")
    void nodesAreNotAssessed() {
        assertThrows(IllegalArgumentException.class,
                () -> GrahaDignity.ofPosition(VedicBody.RAHU, Rashi.MITHUNA, 10));
    }

    // ── Stärkster Graha ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Eindeutige D1-Würde entscheidet")
    void strongestByD1() {
        Map<VedicBody, Double> lon = neutralLongitudes();
        lon.put(VedicBody.SATURN, sign(Rashi.TULA, 20)); // erhöht
        StrongestGrahaDTO s = KundliCalculator.strongest(chart(lon));
        assertEquals("saturn", s.getBody());
        assertEquals("exalted", s.getDignity());
        assertEquals("d1_dignity", s.getDecidedBy());
        assertTrue(s.getTiedOnDignity().isEmpty());
    }

    @Test
    @DisplayName("Gleiche D1-Würde: das Navamsa entscheidet")
    void strongestByD9() {
        Map<VedicBody, Double> lon = neutralLongitudes();
        // Beide erhöht. Jupiter bei 21° Krebs → Navamsa Steinbock (Fall). Mars bei 1° Steinbock
        // → Navamsa Steinbock (erhöht).
        lon.put(VedicBody.JUPITER, sign(Rashi.KARKA, 21));
        lon.put(VedicBody.MARS, sign(Rashi.MAKARA, 1));
        assertEquals(Rashi.MAKARA, Navamsa.signOf(sign(Rashi.KARKA, 21)));
        assertEquals(Rashi.MAKARA, Navamsa.signOf(sign(Rashi.MAKARA, 1)));
        StrongestGrahaDTO s = KundliCalculator.strongest(chart(lon));
        assertEquals("mars", s.getBody());
        assertEquals("d9_dignity", s.getDecidedBy());
    }

    @Test
    @DisplayName("Auch D9 gleich: die natürliche Stärke entscheidet — und die Gleichauf-Liste sagt es")
    void strongestByNaturalStrength() {
        Map<VedicBody, Double> lon = neutralLongitudes();
        // Sonne und Saturn beide erhöht, beide mit Navamsa im selben Rang.
        lon.put(VedicBody.SUN, sign(Rashi.MESHA, 1));     // Navamsa Widder → erhöht
        lon.put(VedicBody.SATURN, sign(Rashi.TULA, 1));   // Navamsa Waage → erhöht
        StrongestGrahaDTO s = KundliCalculator.strongest(chart(lon));
        assertEquals("sun", s.getBody());
        assertEquals("natural_strength", s.getDecidedBy());
        assertEquals(List.of("saturn"), s.getTiedOnDignity());
    }

    @Test
    @DisplayName("Würden-Liste: sieben Grahas, ohne Knoten, mit Vargottama")
    void dignityListCoversTheSevenGrahas() {
        Map<VedicBody, Double> lon = neutralLongitudes();
        lon.put(VedicBody.SUN, sign(Rashi.MESHA, 1)); // Navamsa Widder → vargottama
        List<GrahaDignityDTO> list = KundliCalculator.dignities(chart(lon));
        assertEquals(7, list.size());
        assertTrue(list.stream().noneMatch(d -> d.getBody().equals("rahu") || d.getBody().equals("ketu")));
        GrahaDignityDTO sun = list.stream().filter(d -> d.getBody().equals("sun")).findFirst().orElseThrow();
        assertTrue(sun.isVargottama());
        assertEquals("exalted", sun.getDignity());
    }

    // ── Kaal Sarpa ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Alle Grahas zwischen Rahu und Ketu → Kaal Sarpa, Seite rahu_to_ketu")
    void kaalSarpaRahuToKetu() {
        Map<VedicBody, Double> lon = new EnumMap<>(VedicBody.class);
        double rahu = 100;
        double[] offsets = {10, 30, 50, 80, 110, 150, 170};
        VedicBody[] grahas = VedicBody.classicalGrahas();
        for (int i = 0; i < grahas.length; i++) lon.put(grahas[i], rahu + offsets[i]);
        lon.put(VedicBody.RAHU, rahu);
        lon.put(VedicBody.KETU, rahu + 180);
        KaalSarpaDTO k = KundliCalculator.kaalSarpa(chart(lon));
        assertTrue(k.isPresent());
        assertEquals("rahu_to_ketu", k.getArc());
    }

    @Test
    @DisplayName("Alle Grahas zwischen Ketu und Rahu → Kaal Sarpa, Seite ketu_to_rahu; über 0° hinweg")
    void kaalSarpaKetuToRahuAcrossZero() {
        Map<VedicBody, Double> lon = new EnumMap<>(VedicBody.class);
        double rahu = 20; // Ketu bei 200; die Grahas zwischen 200 und 380 (= 20)
        double[] offsets = {190, 220, 250, 280, 300, 330, 355};
        VedicBody[] grahas = VedicBody.classicalGrahas();
        for (int i = 0; i < grahas.length; i++) lon.put(grahas[i], (rahu + offsets[i]) % 360);
        lon.put(VedicBody.RAHU, rahu);
        lon.put(VedicBody.KETU, rahu + 180);
        KaalSarpaDTO k = KundliCalculator.kaalSarpa(chart(lon));
        assertTrue(k.isPresent());
        assertEquals("ketu_to_rahu", k.getArc());
    }

    @Test
    @DisplayName("Ein Graha auf der anderen Seite bricht die Kette; einer genau auf dem Knoten nicht")
    void kaalSarpaBrokenOrOnNode() {
        Map<VedicBody, Double> lon = new EnumMap<>(VedicBody.class);
        double rahu = 100;
        double[] offsets = {0, 30, 50, 80, 110, 150, 180};
        VedicBody[] grahas = VedicBody.classicalGrahas();
        for (int i = 0; i < grahas.length; i++) lon.put(grahas[i], rahu + offsets[i]);
        lon.put(VedicBody.RAHU, rahu);
        lon.put(VedicBody.KETU, rahu + 180);
        assertTrue(KundliCalculator.kaalSarpa(chart(lon)).isPresent());

        lon.put(VedicBody.SATURN, rahu + 200);
        KaalSarpaDTO broken = KundliCalculator.kaalSarpa(chart(lon));
        assertFalse(broken.isPresent());
        assertNull(broken.getArc());
    }

    // ── Lagna-Grenze ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lagna bis 1° vor oder nach der Zeichengrenze → Warnung; ohne Geburtszeit keine")
    void lagnaBoundaryWarning() {
        VedicChartDTO c = chart(neutralLongitudes());
        c.setTimeDependentAvailable(true);
        c.setAscendantDegreeInSign(29.4);
        List<VedicWarningDTO> w = VedicWarnings.lagnaBoundary(c);
        assertEquals(1, w.size());
        assertEquals("lagna_near_sign_boundary", w.get(0).getCode());
        assertEquals(2, w.get(0).getArgs().get("approxMinutes"));

        c.setAscendantDegreeInSign(15.0);
        assertTrue(VedicWarnings.lagnaBoundary(c).isEmpty());

        c.setAscendantDegreeInSign(0.2);
        c.setTimeDependentAvailable(false);
        assertTrue(VedicWarnings.lagnaBoundary(c).isEmpty());
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────

    private static double sign(Rashi rashi, double degree) {
        return rashi.getIndex() * 30.0 + degree;
    }

    /**
     * Ein Ausgangschart, in dem kein Graha eine besondere Würde hat: jeder steht neutral oder im
     * Zeichen eines Feindes, so dass ein einzeln gesetzter Graha eindeutig herausragt.
     */
    private static Map<VedicBody, Double> neutralLongitudes() {
        Map<VedicBody, Double> lon = new EnumMap<>(VedicBody.class);
        lon.put(VedicBody.SUN, sign(Rashi.MITHUNA, 10));      // Merkur: neutral
        lon.put(VedicBody.MOON, sign(Rashi.MAKARA, 10));      // Saturn: neutral
        lon.put(VedicBody.MARS, sign(Rashi.MITHUNA, 20));     // Merkur: Feind
        lon.put(VedicBody.MERCURY, sign(Rashi.KARKA, 10));    // Mond: Feind
        lon.put(VedicBody.JUPITER, sign(Rashi.MITHUNA, 2));   // Merkur: Feind
        lon.put(VedicBody.VENUS, sign(Rashi.KARKA, 20));      // Mond: Feind
        lon.put(VedicBody.SATURN, sign(Rashi.SIMHA, 10));     // Sonne: Feind
        lon.put(VedicBody.RAHU, sign(Rashi.MESHA, 5));
        lon.put(VedicBody.KETU, sign(Rashi.TULA, 5));
        return lon;
    }

    private static VedicChartDTO chart(Map<VedicBody, Double> longitudes) {
        VedicChartDTO c = new VedicChartDTO();
        c.setLabel("X");
        List<VedicPlanetDTO> planets = new ArrayList<>();
        for (Map.Entry<VedicBody, Double> e : longitudes.entrySet()) {
            double l = ((e.getValue() % 360) + 360) % 360;
            Rashi rashi = Rashi.ofLongitude(l);
            Rashi navamsa = Navamsa.signOf(l);
            VedicPlanetDTO p = new VedicPlanetDTO();
            p.setBody(e.getKey().getKey());
            p.setSiderealLongitude(l);
            p.setRashiIndex(rashi.getIndex());
            p.setRashi(rashi.getKey());
            p.setDegreeInSign(l - rashi.getIndex() * 30.0);
            p.setNavamsaRashiIndex(navamsa.getIndex());
            p.setNavamsaRashi(navamsa.getKey());
            planets.add(p);
        }
        c.setPlanets(planets);
        return c;
    }
}
