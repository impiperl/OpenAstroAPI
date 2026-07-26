package astroapi.utils;

/**
 * Enum representing the types of astrological aspects. Each aspect is defined by:
 * - A description
 * - The exact degree it represents
 * - The allowable orb (tolerance) for considering the aspect valid
 */
public enum AspectType {
    CONJUNCTION("Conjunction", 0, 8),
    SEXTILE("Sextile", 60, 6),
    TRINE("Trine", 120, 6),
    SQUARE("Square", 90, 8),
    OPPOSITION("Opposition", 180, 8),
    QUINCUNX("Quincunx", 150, 2),
    SEMISEXTILE("Semisextile", 30, 2),
    SESQUIQUADRATE("Sesquiquadrate", 135, 4),
    QUINTILE("Quintile", 72, 4),
    BIQUINTILE("Biquintile", 144, 4),
    NOVILE("Novile", 40, 1);

    private final String description;
    private final double exactDegree;
    private final double orb;

    /**
     * Constructs an AspectType enum with the given parameters.
     *
     * @param description A human-readable description of the aspect type.
     * @param exactDegree The exact angular degree associated with the aspect.
     * @param orb         The allowable deviation (orb) from the exact degree for considering the aspect valid.
     */
    AspectType(String description, double exactDegree, double orb) {
        this.description = description;
        this.exactDegree = exactDegree;
        this.orb = orb;
    }

    /**
     * Gets the description of the aspect type.
     *
     * @return The description of the aspect.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the exact degree associated with the aspect.
     *
     * @return The exact angular degree of the aspect.
     */
    public double getExactDegree() {
        return exactDegree;
    }

    /**
     * Gets the allowable orb (tolerance) for the aspect.
     *
     * @return The orb value for the aspect.
     */
    public double getOrb() {
        return orb;
    }
}
