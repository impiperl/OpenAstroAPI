package astroapi.vedic;

import astroapi.vedic.dto.VedicMarriageRequest;
import astroapi.vedic.dto.VedicMarriageResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Die vedische Strecke der AstroAPI — eigener Pfad, eigene DTOs, eigener Instanzpool.
 *
 * <p>Absichtlich <b>nicht</b> in {@code SEController} und nicht als zusätzlicher Parameter an
 * {@code /api/partnerHoroscope}: die westlichen Endpunkte haben Caches, deren Schlüssel keine
 * Ayanamsha kennen, und Aufrufer, die eine unveränderte Antwortform erwarten. Ein Flag dort wäre
 * der kürzere Weg zu einem Regressionsfehler in der bestehenden Synastrie.
 *
 * <p>Der Zugang ist derselbe wie überall in der AstroAPI: {@code X-API-KEY} über
 * {@code ApiKeyFilter}. Es gibt keinen offenen vedischen Pfad.
 */
@RestController
@RequestMapping("/api/vedic")
public class VedicController {

    private final VedicMarriageService marriageService;

    public VedicController(VedicMarriageService marriageService) {
        this.marriageService = marriageService;
    }

    /**
     * Vedische Partner- und Eheanalyse für zwei Geburtsdaten.
     *
     * <p>Rein rechnend: nichts wird gespeichert, keine KI ist beteiligt, und die Antwort trägt den
     * versionierten Berechnungsvertrag, damit ein Aufrufer sie zuordnen und zwischenspeichern kann.
     */
    @PostMapping("/marriage-compatibility")
    public VedicMarriageResponseDTO marriageCompatibility(@Valid @RequestBody VedicMarriageRequest request) {
        return marriageService.analyse(request);
    }

    /**
     * Was dieser Dienst kann, in Zahlen und Schlüsseln — für Oberflächen, die Auswahllisten
     * aufbauen, und für die Fehlersuche, wenn ein gespeichertes Ergebnis aus einer anderen
     * Regelversion stammt.
     */
    @org.springframework.web.bind.annotation.GetMapping("/meta")
    public ResponseEntity<Map<String, Object>> meta() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("calculationSystem", VedicRuleSet.CALCULATION_SYSTEM);
        body.put("calculationVersion", VedicRuleSet.CALCULATION_VERSION);
        body.put("ashtakootaRuleSet", VedicRuleSet.ASHTAKOOTA_RULE_SET);
        body.put("manglikRuleSet", VedicRuleSet.MANGLIK_RULE_SET);
        body.put("ashtakootaMaxPoints", VedicRuleSet.ASHTAKOOTA_MAX_POINTS);
        body.put("kootaOrder", AshtakootaCalculator.KOOTA_ORDER);
        body.put("roleDependentKootas", AshtakootaCalculator.ROLE_DEPENDENT_KOOTAS);
        body.put("manglikCheckedHouses", ManglikCalculator.CHECKED_HOUSES);
        body.put("manglikCancellations", ManglikCalculator.KNOWN_CANCELLATIONS);
        body.put("defaultAyanamsa", Ayanamsa.LAHIRI.getKey());
        body.put("defaultNodeType", NodeType.MEAN.getKey());
        body.put("ketuRule", "rahu_plus_180");
        body.put("houseSystem", "whole_sign");
        java.util.List<String> ayanamsas = new java.util.ArrayList<>();
        for (Ayanamsa a : Ayanamsa.values()) ayanamsas.add(a.getKey());
        body.put("ayanamsas", ayanamsas);
        return ResponseEntity.ok(body);
    }
}
