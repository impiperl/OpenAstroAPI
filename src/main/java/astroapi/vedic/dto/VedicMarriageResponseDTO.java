package astroapi.vedic.dto;

import java.util.List;

/**
 * Die Antwort von {@code POST /api/vedic/marriage-compatibility}.
 *
 * <p>Der versionierte Berechnungsvertrag steht oben: {@code calculationSystem},
 * {@code calculationVersion}, {@code ashtakootaRuleSet}, {@code manglikRuleSet}, {@code ayanamsa},
 * {@code nodeType}. gAIster speichert diese Felder mit und baut daraus seine Cache- und
 * Persistenzschlüssel; App und Radix zeigen sie an. Ein Ergebnis ohne diese Angaben wäre nicht
 * nachrechenbar und damit wertlos.
 *
 * <p>{@code contractKey} fasst sie zu einer Zeichenkette zusammen, damit ein Aufrufer nicht sechs
 * Felder von Hand verketten muss und dabei eine andere Reihenfolge wählt als der nächste.
 */
public class VedicMarriageResponseDTO {

    private String calculationSystem;
    private String calculationVersion;
    private String ashtakootaRuleSet;
    private String manglikRuleSet;
    private String ayanamsa;
    private String nodeType;
    /** Erklärt, wie Ketu entsteht: {@code rahu_plus_180}. */
    private String ketuRule;
    private String contractKey;
    /** Die angefragte Rollenwahl: {@code a_as_bride}, {@code b_as_bride} oder {@code both}. */
    private String traditionalRole;

    private VedicChartDTO personA;
    private VedicChartDTO personB;

    /** Ein Eintrag bei vorgegebener Rolle, zwei bei {@code both}. */
    private List<AshtakootaOrientationDTO> ashtakoota;

    private ManglikResultDTO manglikA;
    private ManglikResultDTO manglikB;
    /** Greift die paarbezogene Aufhebung „beide Manglik" (Dosha Samyam)? */
    private boolean mutualManglikCancellation;

    private MarriageFactorsDTO marriageFactorsA;
    private MarriageFactorsDTO marriageFactorsB;

    private List<VedicWarningDTO> warnings;

    public String getCalculationSystem() { return calculationSystem; }
    public void setCalculationSystem(String v) { this.calculationSystem = v; }
    public String getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(String v) { this.calculationVersion = v; }
    public String getAshtakootaRuleSet() { return ashtakootaRuleSet; }
    public void setAshtakootaRuleSet(String v) { this.ashtakootaRuleSet = v; }
    public String getManglikRuleSet() { return manglikRuleSet; }
    public void setManglikRuleSet(String v) { this.manglikRuleSet = v; }
    public String getAyanamsa() { return ayanamsa; }
    public void setAyanamsa(String ayanamsa) { this.ayanamsa = ayanamsa; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public String getKetuRule() { return ketuRule; }
    public void setKetuRule(String ketuRule) { this.ketuRule = ketuRule; }
    public String getContractKey() { return contractKey; }
    public void setContractKey(String contractKey) { this.contractKey = contractKey; }
    public String getTraditionalRole() { return traditionalRole; }
    public void setTraditionalRole(String v) { this.traditionalRole = v; }
    public VedicChartDTO getPersonA() { return personA; }
    public void setPersonA(VedicChartDTO personA) { this.personA = personA; }
    public VedicChartDTO getPersonB() { return personB; }
    public void setPersonB(VedicChartDTO personB) { this.personB = personB; }
    public List<AshtakootaOrientationDTO> getAshtakoota() { return ashtakoota; }
    public void setAshtakoota(List<AshtakootaOrientationDTO> v) { this.ashtakoota = v; }
    public ManglikResultDTO getManglikA() { return manglikA; }
    public void setManglikA(ManglikResultDTO manglikA) { this.manglikA = manglikA; }
    public ManglikResultDTO getManglikB() { return manglikB; }
    public void setManglikB(ManglikResultDTO manglikB) { this.manglikB = manglikB; }
    public boolean isMutualManglikCancellation() { return mutualManglikCancellation; }
    public void setMutualManglikCancellation(boolean v) { this.mutualManglikCancellation = v; }
    public MarriageFactorsDTO getMarriageFactorsA() { return marriageFactorsA; }
    public void setMarriageFactorsA(MarriageFactorsDTO v) { this.marriageFactorsA = v; }
    public MarriageFactorsDTO getMarriageFactorsB() { return marriageFactorsB; }
    public void setMarriageFactorsB(MarriageFactorsDTO v) { this.marriageFactorsB = v; }
    public List<VedicWarningDTO> getWarnings() { return warnings; }
    public void setWarnings(List<VedicWarningDTO> warnings) { this.warnings = warnings; }
}
