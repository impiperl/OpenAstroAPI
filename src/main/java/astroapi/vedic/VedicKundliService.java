package astroapi.vedic;

import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicKundliRequest;
import astroapi.vedic.dto.VedicKundliResponseDTO;
import astroapi.vedic.dto.VedicPersonRequest;
import astroapi.vedic.dto.VedicWarningDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Setzt die Kundli einer Person zusammen: Chart, Würden, stärkster Graha, Manglik, Kaal Sarpa,
 * Warnungen — und den Vertrag.
 *
 * <p>Wie {@link VedicMarriageService} rechnet diese Klasse selbst nichts Astronomisches. Das Chart
 * kommt aus {@link VedicEphemerisService#chartOf}, derselben Rechnung wie in der Partneranalyse —
 * die Kundli ist deren Zerlegung auf eine Person, keine zweite Rechnung.
 */
@Service
public class VedicKundliService {

    private static final String LABEL_DEFAULT = "A";

    private final VedicEphemerisService ephemerisService;

    public VedicKundliService(VedicEphemerisService ephemerisService) {
        this.ephemerisService = ephemerisService;
    }

    public VedicKundliResponseDTO describe(VedicKundliRequest request) {
        Ayanamsa ayanamsa = Ayanamsa.fromKey(request.getAyanamsa());
        NodeType nodeType = NodeType.fromKey(request.getNodeType());
        VedicChartDTO chart = ephemerisService.chartOf(labelled(request.getPerson()), ayanamsa, nodeType);

        List<VedicWarningDTO> warnings = new ArrayList<>(VedicWarnings.forChart(chart));
        warnings.addAll(VedicWarnings.lagnaBoundary(chart));

        VedicKundliResponseDTO response = new VedicKundliResponseDTO();
        response.setCalculationSystem(VedicRuleSet.CALCULATION_SYSTEM);
        response.setCalculationVersion(VedicRuleSet.CALCULATION_VERSION);
        response.setDignityRuleSet(VedicRuleSet.DIGNITY_RULE_SET);
        response.setManglikRuleSet(VedicRuleSet.MANGLIK_RULE_SET);
        response.setKaalSarpaRuleSet(VedicRuleSet.KAAL_SARPA_RULE_SET);
        response.setAyanamsa(ayanamsa.getKey());
        response.setNodeType(nodeType.getKey());
        response.setKetuRule("rahu_plus_180");
        response.setContractKey(VedicRuleSet.kundliContractKey(ayanamsa, nodeType));
        response.setChart(chart);
        response.setDignities(KundliCalculator.dignities(chart));
        response.setStrongestGraha(KundliCalculator.strongest(chart));
        response.setManglik(ManglikCalculator.calculate(chart));
        response.setKaalSarpa(KundliCalculator.kaalSarpa(chart));
        response.setWarnings(warnings);
        return response;
    }

    private static VedicPersonRequest labelled(VedicPersonRequest person) {
        if (person.getLabel() != null && !person.getLabel().isBlank()) return person;
        VedicPersonRequest copy = new VedicPersonRequest();
        copy.setLabel(LABEL_DEFAULT);
        copy.setDateTime(person.getDateTime());
        copy.setCoordinates(person.getCoordinates());
        copy.setBirthTimeKnown(person.isBirthTimeKnown());
        return copy;
    }
}
