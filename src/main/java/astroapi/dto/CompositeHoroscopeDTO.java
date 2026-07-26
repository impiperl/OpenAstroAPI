package astroapi.dto;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for a Composite Horoscope.
 *
 * <p>
 * This class contains the calculated planetary positions, house cusps, and key points
 * (Ascendant and Medium Coeli) of a composite horoscope, as well as their interpretations.
 * It provides getters and setters for all attributes.
 * </p>
 */
public class CompositeHoroscopeDTO {

    private Map<String, Double> planets;                 // Planetary positions in the composite horoscope
    private List<Double> cusps;                          // House cusps (12 elements)
    private double ascendant;                            // Numeric position of the Ascendant
    private double mediumCoeli;                          // Numeric position of the Medium Coeli
    private String ascendantInterpretation;              // Interpretation of the Ascendant
    private String mediumCoeliInterpretation;            // Interpretation of the Medium Coeli
    private List<AspectDTO> aspects;  // List of aspects
    /**
     * Retrieves the planetary positions in the composite horoscope.
     *
     * @return A map with planets as keys and their positions as values.
     */
    public Map<String, Double> getPlanets() {
        return planets;
    }

    /**
     * Sets the planetary positions in the composite horoscope.
     *
     * @param planets A map with planets as keys and their positions as values.
     */
    public void setPlanets(Map<String, Double> planets) {
        this.planets = planets;
    }

    /**
     * Retrieves the house cusps in the composite horoscope.
     *
     * @return A list of 12 house cusps in degrees.
     */
    public List<Double> getCusps() {
        return cusps;
    }

    /**
     * Sets the house cusps in the composite horoscope.
     *
     * @param cusps A list of 12 house cusps in degrees.
     */
    public void setCusps(List<Double> cusps) {
        this.cusps = cusps;
    }

    /**
     * Retrieves the numeric position of the Ascendant in the composite horoscope.
     *
     * @return The Ascendant in degrees.
     */
    public double getAscendant() {
        return ascendant;
    }

    /**
     * Sets the numeric position of the Ascendant in the composite horoscope.
     *
     * @param ascendant The Ascendant in degrees.
     */
    public void setAscendant(double ascendant) {
        this.ascendant = ascendant;
    }

    /**
     * Retrieves the numeric position of the Medium Coeli in the composite horoscope.
     *
     * @return The Medium Coeli in degrees.
     */
    public double getMediumCoeli() {
        return mediumCoeli;
    }

    /**
     * Sets the numeric position of the Medium Coeli in the composite horoscope.
     *
     * @param mediumCoeli The Medium Coeli in degrees.
     */
    public void setMediumCoeli(double mediumCoeli) {
        this.mediumCoeli = mediumCoeli;
    }

    /**
     * Retrieves the interpretation of the Ascendant.
     *
     * @return A descriptive interpretation of the Ascendant.
     */
    public String getAscendantInterpretation() {
        return ascendantInterpretation;
    }

    /**
     * Sets the interpretation of the Ascendant.
     *
     * @param ascendantInterpretation A descriptive interpretation of the Ascendant.
     */
    public void setAscendantInterpretation(String ascendantInterpretation) {
        this.ascendantInterpretation = ascendantInterpretation;
    }

    /**
     * Retrieves the interpretation of the Medium Coeli.
     *
     * @return A descriptive interpretation of the Medium Coeli.
     */
    public String getMediumCoeliInterpretation() {
        return mediumCoeliInterpretation;
    }

    /**
     * Sets the interpretation of the Medium Coeli.
     *
     * @param mediumCoeliInterpretation A descriptive interpretation of the Medium Coeli.
     */
    public void setMediumCoeliInterpretation(String mediumCoeliInterpretation) {
        this.mediumCoeliInterpretation = mediumCoeliInterpretation;
    }
    public List<AspectDTO> getAspects() {
        return aspects;
    }

    public void setAspects(List<AspectDTO> aspects) {
        this.aspects = aspects;
    }
}
