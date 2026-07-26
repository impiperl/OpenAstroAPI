package astroapi.utils;

/**
 * Represents the twelve zodiac signs, each associated with a specific range of degrees.
 */
public enum ZodiacSign {
    ARIES("Aries", 0),
    TAURUS("Taurus", 30),
    GEMINI("Gemini", 60),
    CANCER("Cancer", 90),
    LEO("Leo", 120),
    VIRGO("Virgo", 150),
    LIBRA("Libra", 180),
    SCORPIO("Scorpio", 210),
    SAGITTARIUS("Sagittarius", 240),
    CAPRICORN("Capricorn", 270),
    AQUARIUS("Aquarius", 300),
    PISCES("Pisces", 330);

    private final String name;
    private final int startDegree;

    /**
     * Constructs a ZodiacSign with the given name and starting degree.
     *
     * @param name        The name of the zodiac sign.
     * @param startDegree The starting degree of the sign (from 0° to 360°).
     */
    ZodiacSign(String name, int startDegree) {
        this.name = name;
        this.startDegree = startDegree;
    }

    /**
     * Returns the name of the zodiac sign.
     *
     * @return The name of the sign.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the starting degree of the zodiac sign.
     *
     * @return The starting degree of the sign.
     */
    public int getStartDegree() {
        return startDegree;
    }

    /**
     * Determines the zodiac sign based on the given degree.
     *
     * @param degree The degree value (0° to 360°).
     * @return The corresponding ZodiacSign enum.
     */
    public static ZodiacSign fromDegree(double degree) {
        for (ZodiacSign sign : ZodiacSign.values()) {
            if (degree >= sign.getStartDegree() && degree < sign.getStartDegree() + 30) {
                return sign;
            }
        }
        // In case the degree is exactly 360°, return Aries
        return ARIES;
    }
}
