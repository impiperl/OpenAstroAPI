package astroapi.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a single astrological request.
 * <p>
 * This class contains the essential data needed for calculating horoscopes,
 * such as the date and time, geographical coordinates, and the house system to use.
 * </p>
 */
public class AstroRequest {

    @NotNull(message = "DateTime cannot be null")
    private LocalDateTime dateTime; // The date and time of the astrological event

    @Valid
    @NotNull(message = "Coordinates cannot be null")
    @Size(min = 2, max = 2, message = "Coordinates must contain exactly two values: [latitude, longitude]")
    private double[] coordinates; // Geographical coordinates (latitude, longitude)

    private char houseSystem; // The house system used for the horoscope (e.g., 'P' for Placidus)

    /**
     * Retrieves the date and time of the astrological event.
     *
     * @return The {@link LocalDateTime} representing the date and time.
     */
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /**
     * Sets the date and time of the astrological event.
     *
     * @param dateTime A {@link LocalDateTime} representing the date and time.
     */
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Retrieves the geographical coordinates for the astrological calculation.
     *
     * @return A double array containing latitude and longitude.
     */
    public double[] getCoordinates() {
        return coordinates;
    }

    /**
     * Sets the geographical coordinates for the astrological calculation.
     *
     * @param coordinates A double array containing latitude and longitude.
     */
    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Retrieves the house system used for the horoscope calculation.
     *
     * @return A character representing the house system.
     */
    public char getHouseSystem() {
        return houseSystem;
    }

    /**
     * Sets the house system to be used for the horoscope calculation.
     *
     * @param houseSystem A character representing the house system (e.g., 'P' for Placidus).
     */
    public void setHouseSystem(char houseSystem) {
        this.houseSystem = houseSystem;
    }
}
