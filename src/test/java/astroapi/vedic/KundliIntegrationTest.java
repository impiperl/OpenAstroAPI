package astroapi.vedic;

import astroapi.service.TimeZoneService;
import astroapi.vedic.dto.GrahaDignityDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicKundliRequest;
import astroapi.vedic.dto.VedicKundliResponseDTO;
import astroapi.vedic.dto.VedicMarriageRequest;
import astroapi.vedic.dto.VedicMarriageResponseDTO;
import astroapi.vedic.dto.VedicPersonRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Die Kundli gegen die echte Ephemeride: Vertrag, Vollständigkeit und die Zusage, dass sie
 * dasselbe Chart rechnet wie die Partneranalyse.
 */
class KundliIntegrationTest {

    private static VedicEphemerisService vedic;
    private static VedicKundliService kundli;
    private static VedicMarriageService marriage;

    @BeforeAll
    static void setup() {
        vedic = new VedicEphemerisService(new TimeZoneService(), "ephe/", 2);
        kundli = new VedicKundliService(vedic);
        marriage = new VedicMarriageService(vedic);
    }

    @AfterAll
    static void tearDown() {
        vedic.shutdown();
    }

    private static VedicPersonRequest person(String label, LocalDateTime dt, double lat, double lon,
                                             boolean timeKnown) {
        VedicPersonRequest p = new VedicPersonRequest();
        p.setLabel(label);
        p.setDateTime(dt);
        p.setCoordinates(new double[]{lat, lon});
        p.setBirthTimeKnown(timeKnown);
        return p;
    }

    private static VedicKundliRequest request(VedicPersonRequest person) {
        VedicKundliRequest r = new VedicKundliRequest();
        r.setPerson(person);
        return r;
    }

    /** Wien, 17. Mai 1990, 14:30 Ortszeit — dieselbe Person wie im Ephemeriden-Test. */
    private static VedicPersonRequest vienna(boolean timeKnown) {
        return person("self", LocalDateTime.of(1990, 5, 17, 14, 30), 48.2082, 16.3738, timeKnown);
    }

    @Test
    @DisplayName("Vertrag: Lahiri und mittlerer Knoten als Vorgabe, alle Regelkennungen gesetzt")
    void contractIsComplete() {
        VedicKundliResponseDTO r = kundli.describe(request(vienna(true)));
        assertEquals("vedic-sidereal", r.getCalculationSystem());
        assertEquals(VedicRuleSet.CALCULATION_VERSION, r.getCalculationVersion());
        assertEquals(VedicRuleSet.DIGNITY_RULE_SET, r.getDignityRuleSet());
        assertEquals(VedicRuleSet.MANGLIK_RULE_SET, r.getManglikRuleSet());
        assertEquals(VedicRuleSet.KAAL_SARPA_RULE_SET, r.getKaalSarpaRuleSet());
        assertEquals("lahiri", r.getAyanamsa());
        assertEquals("mean", r.getNodeType());
        assertEquals("rahu_plus_180", r.getKetuRule());
        assertEquals(VedicRuleSet.kundliContractKey(Ayanamsa.LAHIRI, NodeType.MEAN), r.getContractKey());
        assertNotEquals(VedicRuleSet.contractKey(Ayanamsa.LAHIRI, NodeType.MEAN), r.getContractKey(),
                "Kundli und Partneranalyse hängen an verschiedenen Regelwerken");
    }

    @Test
    @DisplayName("Dasselbe Chart wie in der Partneranalyse — die Kundli ist deren Zerlegung")
    void sameChartAsMarriage() {
        VedicChartDTO fromKundli = kundli.describe(request(vienna(true))).getChart();
        VedicMarriageRequest m = new VedicMarriageRequest();
        m.setPersonA(vienna(true));
        m.setPersonB(person("partner", LocalDateTime.of(1988, 2, 3, 6, 15), 19.0760, 72.8777, true));
        VedicMarriageResponseDTO pair = marriage.analyse(m);
        VedicChartDTO fromPair = pair.getPersonA();

        assertEquals(fromPair.getMoonRashi(), fromKundli.getMoonRashi());
        assertEquals(fromPair.getMoonNakshatra().getKey(), fromKundli.getMoonNakshatra().getKey());
        assertEquals(fromPair.getAscendantRashi(), fromKundli.getAscendantRashi());
        for (int i = 0; i < fromPair.getPlanets().size(); i++) {
            assertEquals(fromPair.getPlanets().get(i).getSiderealLongitude(),
                    fromKundli.getPlanets().get(i).getSiderealLongitude(), 1e-9);
        }
    }

    @Test
    @DisplayName("Würden: sieben Grahas, gültige Schlüssel, stärkster Graha aus diesen sieben")
    void dignitiesAndStrongest() {
        VedicKundliResponseDTO r = kundli.describe(request(vienna(true)));
        Set<String> levels = Set.of("exalted", "moolatrikona", "own_sign", "friend_sign",
                "neutral_sign", "enemy_sign", "debilitated");
        assertEquals(7, r.getDignities().size());
        for (GrahaDignityDTO d : r.getDignities()) {
            assertTrue(levels.contains(d.getDignity()), d.getBody() + ": " + d.getDignity());
            assertTrue(levels.contains(d.getNavamsaDignity()), d.getBody() + ": " + d.getNavamsaDignity());
        }
        List<String> bodies = r.getDignities().stream().map(GrahaDignityDTO::getBody)
                .collect(Collectors.toList());
        assertTrue(bodies.contains(r.getStrongestGraha().getBody()));
        assertTrue(Set.of("d1_dignity", "d9_dignity", "natural_strength")
                .contains(r.getStrongestGraha().getDecidedBy()));
        assertNotNull(r.getManglik());
        assertNotNull(r.getKaalSarpa());
    }

    @Test
    @DisplayName("Ohne Geburtszeit: kein Lagna, keine Lagna-Warnung, aber Würden und Mond bleiben")
    void withoutBirthTime() {
        VedicKundliResponseDTO r = kundli.describe(request(vienna(false)));
        assertFalse(r.getChart().isTimeDependentAvailable());
        assertNull(r.getChart().getAscendantRashi());
        assertTrue(r.getWarnings().stream().anyMatch(w -> "birth_time_unknown".equals(w.getCode())));
        assertTrue(r.getWarnings().stream().noneMatch(w -> "lagna_near_sign_boundary".equals(w.getCode())));
        assertEquals(7, r.getDignities().size());
        assertNotNull(r.getChart().getMoonNakshatra());
        assertFalse(r.getManglik().isLagnaAvailable());
    }

    @Test
    @DisplayName("Ohne Label bekommt die Person ein neutrales")
    void defaultLabel() {
        VedicPersonRequest p = vienna(true);
        p.setLabel(null);
        assertEquals("A", kundli.describe(request(p)).getChart().getLabel());
    }
}
