package astroapi.dto;

import java.util.List;

/**
 * Data Transfer Object for the result of a synastry (compatibility) analysis.
 *
 * <p>This class holds the compatibility level and a summary of aspects
 * identified between the two partners' horoscopes.</p>
 */
public class SynastryDTO {

    private String compatibilityLevel;
    private List<AspectDTO> aspects;
    /**
     * Retrieves the compatibility level between the two partners.
     *
     * @return A {@link String} representing the compatibility level (e.g., "High", "Moderate", "Low").
     */
    public String getCompatibilityLevel() {
        return compatibilityLevel;
    }

    /**
     * Sets the compatibility level between the two partners.
     *
     * @param compatibilityLevel A {@link String} representing the compatibility level.
     */
    public void setCompatibilityLevel(String compatibilityLevel) {
        this.compatibilityLevel = compatibilityLevel;
    }

    public List<AspectDTO> getAspects() {
        return aspects;
    }

    public void setAspects(List<AspectDTO> aspects) {
        this.aspects = aspects;
    }
}
