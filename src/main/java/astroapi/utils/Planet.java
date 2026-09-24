package astroapi.utils;

import swisseph.SweConst;

/**
 * Represents the planets used in astrology, each associated with a Swiss Ephemeris constant and a name.
 *
 * <p>Alle hier gelisteten Körper stehen in den Ephemeridendateien, die im Image liegen: die
 * Planeten in {@code sepl_*}, Mond und Mondapogäum in {@code semo_*}, Chiron, Pholus, Ceres,
 * Pallas, Juno und Vesta in der Asteroiden-Sammeldatei {@code seas_*}.
 *
 * <p><b>Weitere Asteroiden</b> (Eris, Sedna, Hygiea, Psyche, Eros, Chariklo, Nessus …) sprechen
 * über {@code SweConst.SE_AST_OFFSET + Nummer} an, brauchen aber je eine eigene Datei
 * ({@code se00433.se1} usw.) im Verzeichnis {@code ephe/}. Solange die fehlt, liefert
 * {@code swe_calc_ut} nur einen Fehler — deshalb steht hier keiner von ihnen.
 *
 * <p>Welche dieser Körper tatsächlich in eine Berechnung eingehen, entscheidet nicht diese
 * Aufzählung, sondern {@link Bodies}.
 */
public enum Planet {
    // Klassische Planeten
    SUN(SweConst.SE_SUN, "Sun"),
    MOON(SweConst.SE_MOON, "Moon"),
    MERCURY(SweConst.SE_MERCURY, "Mercury"),
    VENUS(SweConst.SE_VENUS, "Venus"),
    MARS(SweConst.SE_MARS, "Mars"),
    JUPITER(SweConst.SE_JUPITER, "Jupiter"),
    SATURN(SweConst.SE_SATURN, "Saturn"),

    // Moderne Planeten
    URANUS(SweConst.SE_URANUS, "Uranus"),
    NEPTUNE(SweConst.SE_NEPTUNE, "Neptune"),
    PLUTO(SweConst.SE_PLUTO, "Pluto"),

    // Lilith (Schwarzer Mond)
    // Nur das MITTLERE Apogäum. Das oszillierende (SE_OSCU_APOG) schwankt um mehrere Grad hin und
    // zurück; im Transit-Scan erzeugt jede dieser Schwingungen echte Nulldurchgänge und damit
    // Ereignisse, die nichts bedeuten.
    MEAN_LILITH(SweConst.SE_MEAN_APOG, "Mean Lilith"),

    // Mondknoten
    TRUE_NODE(SweConst.SE_TRUE_NODE, "True Node"),
    MEAN_NODE(SweConst.SE_MEAN_NODE, "Mean Node"),

    // Zentauren
    CHIRON(SweConst.SE_CHIRON, "Chiron"),
    PHOLUS(SweConst.SE_PHOLUS, "Pholus"),

    // Hauptasteroiden
    CERES(SweConst.SE_CERES, "Ceres"),
    PALLAS(SweConst.SE_PALLAS, "Pallas"),
    JUNO(SweConst.SE_JUNO, "Juno"),
    VESTA(SweConst.SE_VESTA, "Vesta");

    // Fiktive Planeten (Hamburger Schule)
    // CUPIDO(SweConst.SE_CUPIDO, "Cupido"),
    // HADES(SweConst.SE_HADES, "Hades"),
    // ZEUS(SweConst.SE_ZEUS, "Zeus"),
    // KRONOS(SweConst.SE_KRONOS, "Kronos"),
    // APOLLON(SweConst.SE_APOLLON, "Apollon"),
    // ADMETOS(SweConst.SE_ADMETOS, "Admetos"),
    // VULKANUS(SweConst.SE_VULKANUS, "Vulkanus"),
    // POSEIDON(SweConst.SE_POSEIDON, "Poseidon");

    private final int sweConstValue;
    private final String name;

    /**
     * Constructs a Planet with the given Swiss Ephemeris constant and name.
     *
     * @param sweConstValue The Swiss Ephemeris constant representing the planet.
     * @param name          The name of the planet.
     */
    Planet(int sweConstValue, String name) {
        this.sweConstValue = sweConstValue;
        this.name = name;
    }

    /**
     * Returns the Swiss Ephemeris constant value of the planet.
     *
     * @return The SweConst value of the planet.
     */
    public int getSweConstValue() {
        return sweConstValue;
    }

    /**
     * Returns the name of the planet.
     *
     * @return The name of the planet.
     */
    public String getName() {
        return name;
    }

    /**
     * Allows searching for a Planet based on the Swiss Ephemeris constant.
     *
     * @param sweConstValue The Swiss Ephemeris constant of the planet.
     * @return The corresponding Planet enum.
     * @throws IllegalArgumentException If no planet is found with the given constant.
     */
    public static Planet fromSweConstValue(int sweConstValue) {
        for (Planet planet : Planet.values()) {
            if (planet.getSweConstValue() == sweConstValue) {
                return planet;
            }
        }
        throw new IllegalArgumentException("Unknown Swiss Ephemeris constant value: " + sweConstValue);
    }
}
