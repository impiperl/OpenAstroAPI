package astroapi.vedic;

import astroapi.vedic.dto.AshtakootaOrientationDTO;
import astroapi.vedic.dto.ManglikResultDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicMarriageRequest;
import astroapi.vedic.dto.VedicMarriageResponseDTO;
import astroapi.vedic.dto.VedicPersonRequest;
import astroapi.vedic.dto.VedicWarningDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Setzt die vedische Partneranalyse zusammen: zwei Charts, Ashtakoota, Manglik, Ehefaktoren,
 * Warnungen — und den versionierten Berechnungsvertrag, an dem App und Radix hängen.
 *
 * <p>Diese Klasse rechnet selbst nichts Astronomisches. Sie entscheidet nur, was in welcher
 * Reihenfolge gerufen wird und was in die Antwort kommt.
 */
@Service
public class VedicMarriageService {

    private static final String LABEL_A_DEFAULT = "A";
    private static final String LABEL_B_DEFAULT = "B";

    private final VedicEphemerisService ephemerisService;

    public VedicMarriageService(VedicEphemerisService ephemerisService) {
        this.ephemerisService = ephemerisService;
    }

    public VedicMarriageResponseDTO analyse(VedicMarriageRequest request) {
        Ayanamsa ayanamsa = Ayanamsa.fromKey(request.getAyanamsa());
        NodeType nodeType = NodeType.fromKey(request.getNodeType());
        TraditionalRole role = TraditionalRole.fromKey(request.getTraditionalRole());

        VedicPersonRequest personA = labelled(request.getPersonA(), LABEL_A_DEFAULT);
        VedicPersonRequest personB = labelled(request.getPersonB(), LABEL_B_DEFAULT);

        VedicChartDTO chartA = ephemerisService.chartOf(personA, ayanamsa, nodeType);
        VedicChartDTO chartB = ephemerisService.chartOf(personB, ayanamsa, nodeType);

        List<AshtakootaOrientationDTO> orientations = new ArrayList<>(2);
        if (role == TraditionalRole.A_AS_BRIDE || role == TraditionalRole.BOTH) {
            orientations.add(AshtakootaCalculator.calculate(chartA, chartB, TraditionalRole.A_AS_BRIDE));
        }
        if (role == TraditionalRole.B_AS_BRIDE || role == TraditionalRole.BOTH) {
            orientations.add(AshtakootaCalculator.calculate(chartB, chartA, TraditionalRole.B_AS_BRIDE));
        }

        ManglikResultDTO manglikA = ManglikCalculator.calculate(chartA);
        ManglikResultDTO manglikB = ManglikCalculator.calculate(chartB);
        boolean mutual = ManglikCalculator.mutualCancellation(manglikA, manglikB);

        VedicMarriageResponseDTO response = new VedicMarriageResponseDTO();
        response.setCalculationSystem(VedicRuleSet.CALCULATION_SYSTEM);
        response.setCalculationVersion(VedicRuleSet.CALCULATION_VERSION);
        response.setAshtakootaRuleSet(VedicRuleSet.ASHTAKOOTA_RULE_SET);
        response.setManglikRuleSet(VedicRuleSet.MANGLIK_RULE_SET);
        response.setAyanamsa(ayanamsa.getKey());
        response.setNodeType(nodeType.getKey());
        response.setKetuRule("rahu_plus_180");
        response.setContractKey(VedicRuleSet.contractKey(ayanamsa, nodeType));
        response.setTraditionalRole(role.getKey());
        response.setPersonA(chartA);
        response.setPersonB(chartB);
        response.setAshtakoota(orientations);
        response.setManglikA(manglikA);
        response.setManglikB(manglikB);
        response.setMutualManglikCancellation(mutual);
        response.setMarriageFactorsA(MarriageFactors.build(chartA));
        response.setMarriageFactorsB(MarriageFactors.build(chartB));
        response.setWarnings(warnings(chartA, chartB, role, orientations));
        return response;
    }

    // ── Warnungen ────────────────────────────────────────────────────────────

    /**
     * Die Vorbehalte, die zu diesem Ergebnis gehören.
     *
     * <p>Sie sind kein Beiwerk. Eine fehlende Geburtszeit nimmt dem Aszendenten, den Häusern und
     * dem Lagna-Bezug des Manglik die Grundlage; ein grenznaher Mond kann Nadi, Gana, Yoni und Tara
     * kippen, also bis zu 21 der 36 Punkte. Beides gehört in jede Oberfläche und in jedes
     * gespeicherte Ergebnis.
     */
    private List<VedicWarningDTO> warnings(VedicChartDTO chartA, VedicChartDTO chartB,
                                           TraditionalRole role,
                                           List<AshtakootaOrientationDTO> orientations) {
        List<VedicWarningDTO> warnings = new ArrayList<>();
        for (VedicChartDTO chart : List.of(chartA, chartB)) {
            warnings.addAll(VedicWarnings.forChart(chart));
        }
        if (role == TraditionalRole.BOTH && orientations.size() == 2) {
            double first = orientations.get(0).getTotalPoints();
            double second = orientations.get(1).getTotalPoints();
            if (first != second) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("totalAsBride", first);
                args.put("totalBAsBride", second);
                warnings.add(new VedicWarningDTO("role_order_changes_total", "info", null, args));
            }
        }
        return warnings;
    }

    private static VedicPersonRequest labelled(VedicPersonRequest person, String fallback) {
        if (person.getLabel() != null && !person.getLabel().isBlank()) return person;
        VedicPersonRequest copy = new VedicPersonRequest();
        copy.setLabel(fallback);
        copy.setDateTime(person.getDateTime());
        copy.setCoordinates(person.getCoordinates());
        copy.setBirthTimeKnown(person.isBirthTimeKnown());
        return copy;
    }
}
