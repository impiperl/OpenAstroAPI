package astroapi.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for storing the results of a compatibility analysis.
 * <p>
 * This class holds the counts of harmonious and challenging aspects, the overall
 * compatibility level, and a summary of significant aspects between two horoscopes.
 * </p>
 */
public class CompatibilityResult {

    private int harmoniousAspects; // Count of harmonious aspects (e.g., Trines, Sextiles)
    private int challengingAspects; // Count of challenging aspects (e.g., Squares, Oppositions)
    private String compatibilityLevel; // Overall compatibility level (e.g., High, Moderate, Low)
    private List<String> aspectsSummary; // Detailed summary of significant aspects

    /**
     * Constructor to initialize the compatibility result with default values.
     */
    public CompatibilityResult() {
        this.harmoniousAspects = 0;
        this.challengingAspects = 0;
        this.aspectsSummary = new ArrayList<>();
    }

    /**
     * Retrieves the count of harmonious aspects.
     *
     * @return The number of harmonious aspects.
     */
    public int getHarmoniousAspects() {
        return harmoniousAspects;
    }

    /**
     * Increments the count of harmonious aspects by one.
     */
    public void incrementHarmoniousAspects() {
        this.harmoniousAspects++;
    }

    /**
     * Retrieves the count of challenging aspects.
     *
     * @return The number of challenging aspects.
     */
    public int getChallengingAspects() {
        return challengingAspects;
    }

    /**
     * Increments the count of challenging aspects by one.
     */
    public void incrementChallengingAspects() {
        this.challengingAspects++;
    }

    /**
     * Retrieves the overall compatibility level.
     *
     * @return A string representing the compatibility level (e.g., High, Moderate, Low).
     */
    public String getCompatibilityLevel() {
        return compatibilityLevel;
    }

    /**
     * Sets the overall compatibility level.
     *
     * @param compatibilityLevel A string representing the compatibility level (e.g., High, Moderate, Low).
     */
    public void setCompatibilityLevel(String compatibilityLevel) {
        this.compatibilityLevel = compatibilityLevel;
    }

    /**
     * Retrieves the summary of significant aspects between the two horoscopes.
     *
     * @return A list of strings detailing the significant aspects.
     */
    public List<String> getAspectsSummary() {
        return aspectsSummary;
    }

    /**
     * Adds a new aspect to the summary of significant aspects.
     *
     * @param aspect A string describing the significant aspect.
     */
    public void addAspectSummary(String aspect) {
        this.aspectsSummary.add(aspect);
    }
}
