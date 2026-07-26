package astroapi.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing ephemeris data, including planetary positions,
 * house cusps, ascendant, and medium coeli (MC).
 *
 * <p>This class is designed to store calculated astrological data for further interpretation
 * or visualization in horoscopes.</p>
 */
public class EphiDatenDTO {

    private Map<String, Double> planets; // Map of planets and their degrees
    private List<Double> cusps;         // List of house cusp degrees

    private double ascendant;           // Ascendant degree
    private double mediumCoeli;         // Medium Coeli (MC) degree

    private String ascendantInterpretation; // Textual interpretation of the ascendant
    private String mediumCoeliInterpretation; // Textual interpretation of the Medium Coeli (MC)
    private List<AspectDTO> aspects;  // List of aspects

    private double julianDate; // Julian Date of the planetary positions
    private double[] coordinates; // location coordinates
    /**
     * Default constructor.
     */
    public EphiDatenDTO() {
    }

    /**
     * Constructor with initial planetary positions and house cusps.
     *
     * @param planets A {@link Map} of planet names and their degrees.
     * @param cusps   A {@link List} of house cusp degrees.
     */
    public EphiDatenDTO(Map<String, Double> planets, List<Double> cusps) {
        this.planets = planets;
        this.cusps = cusps;
    }

    /**
     * Retrieves the map of planetary positions.
     *
     * @return A {@link Map} where the key is the planet name and the value is its degree.
     */
    public Map<String, Double> getPlanets() {
        return planets;
    }

    /**
     * Sets the map of planetary positions.
     *
     * @param planets A {@link Map} where the key is the planet name and the value is its degree.
     */
    public void setPlanets(Map<String, Double> planets) {
        this.planets = planets;
    }

    /**
     * Retrieves the list of house cusp degrees.
     *
     * @return A {@link List} of {@link Double} values representing the degrees of the house cusps.
     */
    public List<Double> getCusps() {
        return cusps;
    }

    /**
     * Sets the list of house cusp degrees.
     *
     * @param cusps A {@link List} of {@link Double} values representing the degrees of the house cusps.
     */
    public void setCusps(List<Double> cusps) {
        this.cusps = cusps;
    }

    /**
     * Retrieves the degree of the Ascendant.
     *
     * @return A {@code double} representing the Ascendant degree.
     */
    public double getAscendant() {
        return ascendant;
    }

    /**
     * Sets the degree of the Ascendant.
     *
     * @param ascendant A {@code double} representing the Ascendant degree.
     */
    public void setAscendant(double ascendant) {
        this.ascendant = ascendant;
    }

    /**
     * Retrieves the degree of the Medium Coeli (MC).
     *
     * @return A {@code double} representing the Medium Coeli degree.
     */
    public double getMediumCoeli() {
        return mediumCoeli;
    }

    /**
     * Sets the degree of the Medium Coeli (MC).
     *
     * @param mediumCoeli A {@code double} representing the Medium Coeli degree.
     */
    public void setMediumCoeli(double mediumCoeli) {
        this.mediumCoeli = mediumCoeli;
    }

    /**
     * Retrieves the textual interpretation of the Ascendant.
     *
     * @return A {@link String} representing the interpretation of the Ascendant.
     */
    public String getAscendantInterpretation() {
        return ascendantInterpretation;
    }

    /**
     * Sets the textual interpretation of the Ascendant.
     *
     * @param ascendantInterpretation A {@link String} representing the interpretation of the Ascendant.
     */
    public void setAscendantInterpretation(String ascendantInterpretation) {
        this.ascendantInterpretation = ascendantInterpretation;
    }

    /**
     * Retrieves the textual interpretation of the Medium Coeli (MC).
     *
     * @return A {@link String} representing the interpretation of the Medium Coeli.
     */
    public String getMediumCoeliInterpretation() {
        return mediumCoeliInterpretation;
    }

    /**
     * Sets the textual interpretation of the Medium Coeli (MC).
     *
     * @param mediumCoeliInterpretation A {@link String} representing the interpretation of the Medium Coeli.
     */
    public void setMediumCoeliInterpretation(String mediumCoeliInterpretation) {
        this.mediumCoeliInterpretation = mediumCoeliInterpretation;
    }

    /**
     * Converts the list of house cusps into a map where keys are house numbers (1-12).
     *
     * @return A {@link Map} with house numbers as keys and cusp degrees as values.
     * @throws IllegalStateException If the house cusp list is null or does not contain exactly 12 elements.
     */
    public Map<Integer, Double> getCuspsAsMap() {
        if (cusps == null || cusps.size() != 12) {
            throw new IllegalStateException("The cusps list must contain exactly 12 elements.");
        }
        Map<Integer, Double> cuspsMap = new HashMap<>();
        for (int i = 0; i < cusps.size(); i++) {
            cuspsMap.put(i + 1, cusps.get(i)); // House numbers start from 1
        }
        return cuspsMap;
    }
    public List<AspectDTO> getAspects() {
        return aspects;
    }

    public void setAspects(List<AspectDTO> aspects) {
        this.aspects = aspects;
    }
    public double getJulianDate() {
        return julianDate;
    }

    public void setJulianDate(double julianDate) {
        this.julianDate = julianDate;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }
}
