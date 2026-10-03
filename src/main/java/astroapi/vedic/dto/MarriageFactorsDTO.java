package astroapi.vedic.dto;

import java.util.List;

/** Die vertieften Ehefaktoren einer Person — siderischer Aszendent, 7. Haus und seine Mitspieler. */
public class MarriageFactorsDTO {

    private String label;
    private List<MarriageFactorDTO> factors;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public List<MarriageFactorDTO> getFactors() { return factors; }
    public void setFactors(List<MarriageFactorDTO> factors) { this.factors = factors; }
}
