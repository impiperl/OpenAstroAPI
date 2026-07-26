package astroapi.utils;

/**
 * Represents the various house systems used in astrology, each associated with a specific code.
 */
public enum HouseSystem {
    PLACIDUS('P'),
    KOCH('K'),
    CAMPANUS('C'),
    REGIOMONTANUS('R'),
    PORPHYRY('O'),
    EQUAL_HOUSES('E'),
    TOPOCENTRIC('T'),
    WHOLE_SIGN_HOUSES('G'),
    ALCABITUS('A'),
    MERIDIAN('M'),
    DOMICAL('D'),
    WHOLE_TROPICAL_HOUSES('W');

    private final char code;

    /**
     * Constructs a HouseSystem with the given code.
     *
     * @param code The character code representing the house system.
     */
    HouseSystem(char code) {
        this.code = code;
    }

    /**
     * Retrieves the character code associated with the house system.
     *
     * @return The code of the house system.
     */
    public char getCode() {
        return code;
    }

    /**
     * Retrieves the HouseSystem enum corresponding to the given code.
     *
     * @param code The character code of the desired house system.
     * @return The corresponding HouseSystem enum.
     * @throws IllegalArgumentException If no HouseSystem matches the provided code.
     */
    public static HouseSystem fromCode(char code) {
        for (HouseSystem hs : HouseSystem.values()) {
            if (hs.code == code) {
                return hs;
            }
        }
        return PLACIDUS; // Default option if no match is found
    }
}
