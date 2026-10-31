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
    private final astroapi.vedic.panchanga.PanchangaService panchangaService;
    private final VedicKundliService kundliService;

    public VedicController(VedicMarriageService marriageService,
                           astroapi.vedic.panchanga.PanchangaService panchangaService,
                           VedicKundliService kundliService) {
        this.marriageService = marriageService;
        this.panchangaService = panchangaService;
        this.kundliService = kundliService;
    }

    /**
     * Die Kundli einer Person: Chart (D1 und D9), Würden, stärkster Graha, Manglik, Kaal Sarpa.
     *
     * <p>Rein rechnend wie die Partneranalyse: nichts wird gespeichert, keine KI ist beteiligt.
     * Die Doshas sind immer dabei; ob eine Oberfläche sie zeigt, entscheidet sie selbst.
     */
    @PostMapping("/kundli")
    public astroapi.vedic.dto.VedicKundliResponseDTO kundli(
            @Valid @RequestBody astroapi.vedic.dto.VedicKundliRequest request) {
        return kundliService.describe(request);
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
     * Das Panchanga eines Augenblicks an einem Ort — die fünf Glieder des vedischen Tages.
     *
     * <p>Rein rechnend wie die Partneranalyse: nichts wird gespeichert, keine KI ist beteiligt.
     *
     * <p><b>Der Ort ist Pflicht</b>, und zwar nicht aus Bequemlichkeit: Der vedische Tag beginnt
     * mit dem Sonnenaufgang. Dasselbe Datum ergibt in Hamburg und in Chennai verschiedene
     * Panchangas, und wer vor Sonnenaufgang fragt, steht noch im Tag von gestern.
     *
     * <p><b>Was die Antwort ist:</b> eine Momentaufnahme zum gefragten Augenblick — die fünf
     * Glieder, die <i>gerade</i> gelten, samt ihrem jeweiligen Ende. Es ist <b>kein</b>
     * vollständiges Tages-Panchanga von Sonnenaufgang bis Sonnenaufgang: Wechselt ein Glied im
     * Lauf des Tages, taucht das folgende hier nicht auf. Wer den ganzen Tag braucht, fragt
     * mehrfach oder wartet auf den Tagesendpunkt.
     *
     * <p>Kein Sonnenaufgang am Ort (Polartag oder -nacht) ergibt <b>422</b> mit dem Code
     * {@code no_sunrise}, nicht 500: Die Anfrage war gültig, die Antwort ist dort nicht
     * berechenbar.
     */
    @PostMapping("/panchanga")
    public astroapi.vedic.dto.PanchangaResponseDTO panchanga(
            @Valid @RequestBody astroapi.vedic.dto.PanchangaRequest request) {
        double[] coordinates = request.getCoordinates();
        // instant hat Vorrang, weil es eindeutig ist; dateTime bleibt als bequemer Weg bestehen.
        return request.getInstant() != null
                ? panchangaService.describe(coordinates[0], coordinates[1], request.getInstant().toInstant())
                : panchangaService.describe(coordinates[0], coordinates[1], request.getDateTime());
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
        body.put("panchangaRuleSet", VedicRuleSet.PANCHANGA_RULE_SET);
        body.put("dignityRuleSet", VedicRuleSet.DIGNITY_RULE_SET);
        body.put("kaalSarpaRuleSet", VedicRuleSet.KAAL_SARPA_RULE_SET);
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
