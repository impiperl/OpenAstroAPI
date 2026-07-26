package astroapi.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Data Transfer Object for requesting a composite horoscope calculation.
 *
 * <p>
 * This class encapsulates all the necessary information to calculate a composite horoscope
 * by combining the birth data of two individuals. It includes the birth data of both partners,
 * geographical coordinates for the composite location, and the house system to use.
 * </p>
 */
public class CompositeRequest {

    @Valid
    @NotNull(message = "Partner one cannot be null")
    private AstroRequest partnerOne; // Birth data for the first partner

    @Valid
    @NotNull(message = "Partner two cannot be null")
    private AstroRequest partnerTwo; // Birth data for the second partner

    @Valid
    @NotNull(message = "Coordinates cannot be null")
    @Size(min = 2, max = 2, message = "Coordinates must contain exactly two values: [latitude, longitude]")
    private double[] coordinates; // Geographical coordinates (latitude and longitude) for the composite horoscope

    @Valid
    @NotNull(message = "House system cannot be null")
    private char houseSystem; // The house system to use (e.g., 'P' for Placidus)

    /**
     * Retrieves the birth data for the first partner.
     *
     * @return An {@link AstroRequest} object representing the first partner's birth data.
     */
    public AstroRequest getPartnerOne() {
        return partnerOne;
    }

    /**
     * Sets the birth data for the first partner.
     *
     * @param partnerOne An {@link AstroRequest} object representing the first partner's birth data.
     */
    public void setPartnerOne(AstroRequest partnerOne) {
        this.partnerOne = partnerOne;
    }

    /**
     * Retrieves the birth data for the second partner.
     *
     * @return An {@link AstroRequest} object representing the second partner's birth data.
     */
    public AstroRequest getPartnerTwo() {
        return partnerTwo;
    }

    /**
     * Sets the birth data for the second partner.
     *
     * @param partnerTwo An {@link AstroRequest} object representing the second partner's birth data.
     */
    public void setPartnerTwo(AstroRequest partnerTwo) {
        this.partnerTwo = partnerTwo;
    }

    /**
     * Retrieves the composite location's latitude.
     *
     * @return A double representing the latitude.
     */
    public double getCompositeLatitude() {
        return coordinates[0];
    }

    /**
     * Sets the composite location's latitude.
     *
     * @param compositeLatitude A double representing the latitude.
     */
    public void setCompositeLatitude(double compositeLatitude) {
        this.coordinates[0] = compositeLatitude;
    }

    /**
     * Retrieves the composite location's longitude.
     *
     * @return A double representing the longitude.
     */
    public double getCompositeLongitude() {
        return coordinates[1];
    }

    /**
     * Sets the composite location's longitude.
     *
     * @param compositeLongitude A double representing the longitude.
     */
    public void setCompositeLongitude(double compositeLongitude) {
        this.coordinates[1] = compositeLongitude;
    }

    /**
     * Retrieves the house system to be used for the composite horoscope.
     *
     * @return A char representing the house system (e.g., 'P' for Placidus).
     */
    public char getHouseSystem() {
        return houseSystem;
    }

    /**
     * Sets the house system to be used for the composite horoscope.
     *
     * @param houseSystem A char representing the house system (e.g., 'P' for Placidus).
     */
    public void setHouseSystem(char houseSystem) {
        this.houseSystem = houseSystem;
    }

    /**
     * Retrieves the geographical coordinates for the composite location.
     *
     * @return A double array containing two elements: [latitude, longitude].
     */
    public double[] getCoordinates() {
        return coordinates;
    }

    /**
     * Sets the geographical coordinates for the composite location.
     *
     * @param coordinates A double array containing two elements: [latitude, longitude].
     */
    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }
}
