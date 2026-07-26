package astroapi.dto;

/**
 * Data Transfer Object representing detailed information about the Moon's position and characteristics.
 *
 * <p>This class holds data about the Moon's ecliptic coordinates, distance from Earth, speed, and its interpretation.</p>
 */
public class MoonDTO {

    private double longitude;                // Ecliptic longitude of the Moon
    private double latitude;                 // Ecliptic latitude of the Moon
    private double distance;                 // Distance of the Moon from Earth in Astronomical Units (AU)
    private double speedLongitude;           // Speed of the Moon in longitude (degrees/day)
    private double speedLatitude;            // Speed of the Moon in latitude (degrees/day)
    private double speedDistance;            // Speed of the Moon's distance change (AU/day)
    private String moonPosInterpretation;    // Interpretation of the Moon's position
    private String moonPhase;                // Current phase of the Moon
    private double julianDate;               // Julian Date of the Moon's position
    /**
     * Retrieves the ecliptic longitude of the Moon.
     *
     * @return A {@code double} representing the Moon's longitude in degrees.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the ecliptic longitude of the Moon.
     *
     * @param longitude A {@code double} representing the Moon's longitude in degrees.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Retrieves the ecliptic latitude of the Moon.
     *
     * @return A {@code double} representing the Moon's latitude in degrees.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the ecliptic latitude of the Moon.
     *
     * @param latitude A {@code double} representing the Moon's latitude in degrees.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Retrieves the distance of the Moon from Earth in astronomical units (AU).
     *
     * @return A {@code double} representing the Moon's distance.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Sets the distance of the Moon from Earth in astronomical units (AU).
     *
     * @param distance A {@code double} representing the Moon's distance.
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * Retrieves the speed of the Moon in longitude (degrees per day).
     *
     * @return A {@code double} representing the Moon's longitudinal speed.
     */
    public double getSpeedLongitude() {
        return speedLongitude;
    }

    /**
     * Sets the speed of the Moon in longitude (degrees per day).
     *
     * @param speedLongitude A {@code double} representing the Moon's longitudinal speed.
     */
    public void setSpeedLongitude(double speedLongitude) {
        this.speedLongitude = speedLongitude;
    }

    /**
     * Retrieves the speed of the Moon in latitude (degrees per day).
     *
     * @return A {@code double} representing the Moon's latitudinal speed.
     */
    public double getSpeedLatitude() {
        return speedLatitude;
    }

    /**
     * Sets the speed of the Moon in latitude (degrees per day).
     *
     * @param speedLatitude A {@code double} representing the Moon's latitudinal speed.
     */
    public void setSpeedLatitude(double speedLatitude) {
        this.speedLatitude = speedLatitude;
    }

    /**
     * Retrieves the speed of the Moon's distance change in astronomical units (AU per day).
     *
     * @return A {@code double} representing the speed of the Moon's distance change.
     */
    public double getSpeedDistance() {
        return speedDistance;
    }

    /**
     * Sets the speed of the Moon's distance change in astronomical units (AU per day).
     *
     * @param speedDistance A {@code double} representing the speed of the Moon's distance change.
     */
    public void setSpeedDistance(double speedDistance) {
        this.speedDistance = speedDistance;
    }

    /**
     * Retrieves the interpretation of the Moon's position.
     *
     * @return A {@link String} containing the interpretation of the Moon's position.
     */
    public String getMoonPosInterpretation() {
        return moonPosInterpretation;
    }

    /**
     * Sets the interpretation of the Moon's position.
     *
     * @param moonPosInterpretation A {@link String} containing the interpretation of the Moon's position.
     */
    public void setMoonPosInterpretation(String moonPosInterpretation) {
        this.moonPosInterpretation = moonPosInterpretation;
    }

    /**
     * Retrieves the current phase of the Moon.
     *
     * @return A {@link String} representing the Moon's phase (e.g., "New Moon", "Full Moon").
     */
    public String getMoonPhase() {
        return moonPhase;
    }

    /**
     * Sets the current phase of the Moon.
     *
     * @param moonPhase A {@link String} representing the Moon's phase.
     */
    public void setMoonPhase(String moonPhase) {
        this.moonPhase = moonPhase;
    }

    public double getJulianDate() {
        return julianDate;
    }

    public void setJulianDate(double julianDate) {
        this.julianDate = julianDate;
    }
}
