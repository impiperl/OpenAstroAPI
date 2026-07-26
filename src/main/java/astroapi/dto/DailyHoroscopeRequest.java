package astroapi.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for requesting a daily horoscope calculation.
 *
 * <p>
 * This class encapsulates all the necessary information to calculate a daily horoscope,
 * including the base horoscope details, the date and time for the calculation,
 * geographical coordinates, and the house system to use.
 * </p>
 */
public class DailyHoroscopeRequest {

    @Valid
    @NotNull(message = "Horoscope cannot be null")
    private AstroRequest horoscope; // The base horoscope information

    @Valid
    @NotNull(message = "DateTime cannot be null")
    private LocalDateTime dateTime; // The date and time for the horoscope calculation

    @Valid
    @NotNull(message = "Coordinates cannot be null")
    @Size(min = 2, max = 2, message = "Coordinates must contain exactly two values: [latitude, longitude]")
    private double[] coordinates; // The geographical coordinates (latitude, longitude)

    private char houseSystem; // The house system to use (e.g., 'P' for Placidus)

    /**
     * Retrieves the base horoscope information.
     *
     * @return An {@link AstroRequest} object containing the base horoscope details.
     */
    public AstroRequest getHoroscope() {
        return horoscope;
    }

    /**
     * Sets the base horoscope information.
     *
     * @param horoscope An {@link AstroRequest} object containing the base horoscope details.
     */
    public void setHoroscope(AstroRequest horoscope) {
        this.horoscope = horoscope;
    }

    /**
     * Retrieves the date and time for the horoscope calculation.
     *
     * @return A {@link LocalDateTime} object representing the date and time.
     */
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /**
     * Sets the date and time for the horoscope calculation.
     *
     * @param dateTime A {@link LocalDateTime} object representing the date and time.
     */
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Retrieves the geographical coordinates for the calculation.
     *
     * @return A double array containing two elements: [latitude, longitude].
     */
    public double[] getCoordinates() {
        return coordinates;
    }

    /**
     * Sets the geographical coordinates for the calculation.
     *
     * @param coordinates A double array containing two elements: [latitude, longitude].
     */
    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Retrieves the house system to be used for the calculation.
     *
     * @return A {@code char} representing the house system (e.g., 'P' for Placidus).
     */
    public char getHouseSystem() {
        return houseSystem;
    }

    /**
     * Sets the house system to be used for the calculation.
     *
     * @param houseSystem A {@code char} representing the house system (e.g., 'P' for Placidus).
     */
    public void setHouseSystem(char houseSystem) {
        this.houseSystem = houseSystem;
    }
}
