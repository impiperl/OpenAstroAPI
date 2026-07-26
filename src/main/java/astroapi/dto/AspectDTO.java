package astroapi.dto;

/**
 * Data Transfer Object representing a specific aspect between two planets,
 * such as a natal planet and a transiting planet.
 * <p>
 * This DTO contains detailed information about the aspect, including the
 * type of aspect (e.g., conjunction, opposition), the planets involved,
 * and the astrological houses and signs they are located in.
 * </p>
 */
public class AspectDTO {

    private String natalPlanet;      // The natal planet involved in the aspect
    private String transitPlanet;    // The transiting planet involved in the aspect
    private String aspectType;       // The type of aspect, e.g., "Conjunction"
    private double exactAngle;       // The exact angle of the aspect, e.g., 0.0 for Conjunction
    private double orb;              // The difference from the exact angle of the aspect
    private String natalSign;        // The zodiac sign of the natal planet
    private String transitSign;      // The zodiac sign of the transiting planet
    private Integer natalHouse;      // The house of the natal planet
    private Integer transitHouse;    // The house of the transiting planet

    // Getters and Setters

    /**
     * Gets the natal planet involved in the aspect.
     *
     * @return The natal planet as a String.
     */
    public String getNatalPlanet() {
        return natalPlanet;
    }

    /**
     * Sets the natal planet involved in the aspect.
     *
     * @param natalPlanet The natal planet as a String.
     */
    public void setNatalPlanet(String natalPlanet) {
        this.natalPlanet = natalPlanet;
    }

    /**
     * Gets the transiting planet involved in the aspect.
     *
     * @return The transiting planet as a String.
     */
    public String getTransitPlanet() {
        return transitPlanet;
    }

    /**
     * Sets the transiting planet involved in the aspect.
     *
     * @param transitPlanet The transiting planet as a String.
     */
    public void setTransitPlanet(String transitPlanet) {
        this.transitPlanet = transitPlanet;
    }

    /**
     * Gets the type of the aspect, such as "Conjunction" or "Opposition".
     *
     * @return The aspect type as a String.
     */
    public String getAspectType() {
        return aspectType;
    }

    /**
     * Sets the type of the aspect, such as "Conjunction" or "Opposition".
     *
     * @param aspectType The aspect type as a String.
     */
    public void setAspectType(String aspectType) {
        this.aspectType = aspectType;
    }

    /**
     * Gets the exact angle of the aspect in degrees.
     *
     * @return The exact angle as a double.
     */
    public double getExactAngle() {
        return exactAngle;
    }

    /**
     * Sets the exact angle of the aspect in degrees.
     *
     * @param exactAngle The exact angle as a double.
     */
    public void setExactAngle(double exactAngle) {
        this.exactAngle = exactAngle;
    }

    /**
     * Gets the orb, which is the deviation from the exact angle of the aspect.
     *
     * @return The orb as a double.
     */
    public double getOrb() {
        return orb;
    }

    /**
     * Sets the orb, which is the deviation from the exact angle of the aspect.
     *
     * @param orb The orb as a double.
     */
    public void setOrb(double orb) {
        this.orb = orb;
    }

    /**
     * Gets the zodiac sign of the natal planet.
     *
     * @return The natal sign as a String.
     */
    public String getNatalSign() {
        return natalSign;
    }

    /**
     * Sets the zodiac sign of the natal planet.
     *
     * @param natalSign The natal sign as a String.
     */
    public void setNatalSign(String natalSign) {
        this.natalSign = natalSign;
    }

    /**
     * Gets the zodiac sign of the transiting planet.
     *
     * @return The transit sign as a String.
     */
    public String getTransitSign() {
        return transitSign;
    }

    /**
     * Sets the zodiac sign of the transiting planet.
     *
     * @param transitSign The transit sign as a String.
     */
    public void setTransitSign(String transitSign) {
        this.transitSign = transitSign;
    }

    /**
     * Gets the house of the natal planet.
     *
     * @return The natal house as an Integer.
     */
    public Integer getNatalHouse() {
        return natalHouse;
    }

    /**
     * Sets the house of the natal planet.
     *
     * @param natalHouse The natal house as an Integer.
     */
    public void setNatalHouse(Integer natalHouse) {
        this.natalHouse = natalHouse;
    }

    /**
     * Gets the house of the transiting planet.
     *
     * @return The transit house as an Integer.
     */
    public Integer getTransitHouse() {
        return transitHouse;
    }

    /**
     * Sets the house of the transiting planet.
     *
     * @param transitHouse The transit house as an Integer.
     */
    public void setTransitHouse(Integer transitHouse) {
        this.transitHouse = transitHouse;
    }
}