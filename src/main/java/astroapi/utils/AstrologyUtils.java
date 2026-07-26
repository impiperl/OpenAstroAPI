package astroapi.utils;

import java.util.List;

/**
 * Utility class for performing common astrological calculations and interpretations.
 */
public class AstrologyUtils {

    /**
     * Interprets a degree value (e.g., an ascendant or planetary position) and returns
     * the corresponding zodiac sign along with the specific degree within the sign.
     *
     * @param degree The degree value to interpret (0° to 360°).
     * @return A string representing the zodiac sign and the degree within the sign (e.g., "Aries 15.3°").
     */
    public static String interpretDegree(double degree) {
        // Ensure the degree value is within the 0° to 360° range
        degree = degree % 360;
        if (degree < 0) {
            degree += 360;
        }

        // Determine the zodiac sign
        ZodiacSign sign = ZodiacSign.fromDegree(degree);

        // Calculate the specific degree within the sign
        double degreeInSign = degree - sign.getStartDegree();

        return String.format("%s %.1f°", sign.getName(), degreeInSign);
    }

    /**
     * Determines the house number for a given degree based on the provided house cusps.
     *
     * @param degree     The degree value (0° to 360°) for which to find the house.
     * @param houseCusps A list of house cusp degrees (expected to contain exactly 12 elements).
     * @return The house number (1 to 12) where the degree falls, or null if no house is found.
     * @throws IllegalArgumentException If the house cusps list is null or does not contain exactly 12 elements.
     */
    public static Integer getHouseForDegree(double degree, List<Double> houseCusps) {
        if (houseCusps == null || houseCusps.size() != 12) {
            throw new IllegalArgumentException("House cusps must be a list of 12 elements.");
        }

        // Adjust the degree to ensure it falls within the 0° to 360° range
        degree = (degree % 360 + 360) % 360;

        for (int i = 0; i < 12; i++) {
            double cuspStart = houseCusps.get(i);
            double cuspEnd = houseCusps.get((i + 1) % 12); // Wrap around after the 12th house

            // Normalize cusp degrees to ensure they are within the 0° to 360° range
            cuspStart = (cuspStart % 360 + 360) % 360;
            cuspEnd = (cuspEnd % 360 + 360) % 360;

            boolean inHouse;
            if (cuspStart < cuspEnd) {
                // Standard case where cuspStart < cuspEnd (e.g., 10° to 40°)
                inHouse = degree >= cuspStart && degree < cuspEnd;
            } else {
                // Wrap-around case where cuspStart > cuspEnd (e.g., 330° to 20°)
                inHouse = degree >= cuspStart || degree < cuspEnd;
            }

            if (inHouse) {
                return i + 1; // Return house number (1-based index)
            }
        }
        return null;
    }
}
