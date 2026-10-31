package astroapi.vedic.dto;

import java.util.List;

/**
 * Die Antwort von {@code POST /api/vedic/kundli}: das vedische Profil einer Person.
 *
 * <p>Wie bei der Partneranalyse steht der versionierte Vertrag oben, und {@code contractKey} fasst
 * ihn zusammen. gAIster baut daraus seine Persistenzschlüssel; steigt eine Regelversion, wird neu
 * gerechnet statt ein Ergebnis nach alten Regeln weiterzugeben.
 *
 * <p>{@code manglik} und {@code kaalSarpa} werden <b>immer</b> gerechnet. Ob sie erscheinen,
 * entscheidet die Oberfläche nach der Einstellung der Nutzerin — die Rechnung ändert das nicht.
 */
public class VedicKundliResponseDTO {

    private String calculationSystem;
    private String calculationVersion;
    private String dignityRuleSet;
    private String manglikRuleSet;
    private String kaalSarpaRuleSet;
    private String ayanamsa;
    private String nodeType;
    /** Erklärt, wie Ketu entsteht: {@code rahu_plus_180}. */
    private String ketuRule;
    private String contractKey;

    private VedicChartDTO chart;
    private List<GrahaDignityDTO> dignities;
    private StrongestGrahaDTO strongestGraha;
    private ManglikResultDTO manglik;
    private KaalSarpaDTO kaalSarpa;
    private List<VedicWarningDTO> warnings;

    public String getCalculationSystem() { return calculationSystem; }
    public void setCalculationSystem(String v) { this.calculationSystem = v; }
    public String getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(String v) { this.calculationVersion = v; }
    public String getDignityRuleSet() { return dignityRuleSet; }
    public void setDignityRuleSet(String v) { this.dignityRuleSet = v; }
    public String getManglikRuleSet() { return manglikRuleSet; }
    public void setManglikRuleSet(String v) { this.manglikRuleSet = v; }
    public String getKaalSarpaRuleSet() { return kaalSarpaRuleSet; }
    public void setKaalSarpaRuleSet(String v) { this.kaalSarpaRuleSet = v; }
    public String getAyanamsa() { return ayanamsa; }
    public void setAyanamsa(String ayanamsa) { this.ayanamsa = ayanamsa; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public String getKetuRule() { return ketuRule; }
    public void setKetuRule(String ketuRule) { this.ketuRule = ketuRule; }
    public String getContractKey() { return contractKey; }
    public void setContractKey(String contractKey) { this.contractKey = contractKey; }
    public VedicChartDTO getChart() { return chart; }
    public void setChart(VedicChartDTO chart) { this.chart = chart; }
    public List<GrahaDignityDTO> getDignities() { return dignities; }
    public void setDignities(List<GrahaDignityDTO> dignities) { this.dignities = dignities; }
    public StrongestGrahaDTO getStrongestGraha() { return strongestGraha; }
    public void setStrongestGraha(StrongestGrahaDTO strongestGraha) { this.strongestGraha = strongestGraha; }
    public ManglikResultDTO getManglik() { return manglik; }
    public void setManglik(ManglikResultDTO manglik) { this.manglik = manglik; }
    public KaalSarpaDTO getKaalSarpa() { return kaalSarpa; }
    public void setKaalSarpa(KaalSarpaDTO kaalSarpa) { this.kaalSarpa = kaalSarpa; }
    public List<VedicWarningDTO> getWarnings() { return warnings; }
    public void setWarnings(List<VedicWarningDTO> warnings) { this.warnings = warnings; }
}
