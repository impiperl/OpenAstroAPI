package astroapi.vedic.panchanga;

/**
 * Die 30 Tithis eines Mondmonats.
 *
 * <p>Eine Tithi ist der Abschnitt, in dem der Mond die Sonne um je 12° überholt: der synodische
 * Monat (360° Längenunterschied) zerfällt in 30 gleiche Teile. Die Namen laufen zweimal durch —
 * einmal in der zunehmenden Hälfte (Shukla) und einmal in der abnehmenden (Krishna). Die
 * fünfzehnte heißt deshalb je nach Hälfte <b>Purnima</b> (Vollmond) oder <b>Amavasya</b> (Neumond).
 *
 * <p><b>Ayanamsha-unabhängig.</b> Eine Tithi ergibt sich aus der Differenz Mond − Sonne, und eine
 * Verschiebung des Nullpunkts verschiebt beide gleich weit. Tithi und Karana sind damit in jedem
 * Ayanamsha identisch — im Gegensatz zu Nakshatra und Yoga. Deshalb nimmt der Rechner hier
 * ausdrücklich keine siderische Länge entgegen, sondern die bereits gebildete Differenz.
 */
public enum Tithi {

    PRATIPADA(1, "pratipada", "Pratipada"),
    DVITIYA(2, "dvitiya", "Dvitiya"),
    TRITIYA(3, "tritiya", "Tritiya"),
    CHATURTHI(4, "chaturthi", "Chaturthi"),
    PANCHAMI(5, "panchami", "Panchami"),
    SHASHTHI(6, "shashthi", "Shashthi"),
    SAPTAMI(7, "saptami", "Saptami"),
    ASHTAMI(8, "ashtami", "Ashtami"),
    NAVAMI(9, "navami", "Navami"),
    DASHAMI(10, "dashami", "Dashami"),
    EKADASHI(11, "ekadashi", "Ekadashi"),
    DVADASHI(12, "dvadashi", "Dvadashi"),
    TRAYODASHI(13, "trayodashi", "Trayodashi"),
    CHATURDASHI(14, "chaturdashi", "Chaturdashi"),
    /** Je nach Hälfte Purnima (Shukla) oder Amavasya (Krishna) — siehe {@link #displayNameIn}. */
    PANCHADASHI(15, "panchadashi", "Panchadashi");

    private final int numberInPaksha;
    private final String key;
    private final String displayName;

    Tithi(int numberInPaksha, String key, String displayName) {
        this.numberInPaksha = numberInPaksha;
        this.key = key;
        this.displayName = displayName;
    }

    /** 1…15 innerhalb der Mondhälfte. */
    public int getNumberInPaksha() { return numberInPaksha; }
    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }

    /** Der Übersetzungsschlüssel samt Hälfte — die Fünfzehnte heißt in jeder Hälfte anders. */
    public String keyIn(Paksha paksha) {
        if (this != PANCHADASHI) return key;
        return paksha == Paksha.SHUKLA ? "purnima" : "amavasya";
    }

    public String displayNameIn(Paksha paksha) {
        if (this != PANCHADASHI) return displayName;
        return paksha == Paksha.SHUKLA ? "Purnima" : "Amavasya";
    }

    /** @param absoluteIndex 0…29, gezählt ab dem Neumond */
    public static Tithi ofAbsoluteIndex(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex > 29) {
            throw new IllegalArgumentException("tithi index must be between 0 and 29, was " + absoluteIndex);
        }
        return values()[absoluteIndex % 15];
    }

    /** @param absoluteIndex 0…29, gezählt ab dem Neumond */
    public static Paksha pakshaOfAbsoluteIndex(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex > 29) {
            throw new IllegalArgumentException("tithi index must be between 0 and 29, was " + absoluteIndex);
        }
        return absoluteIndex < 15 ? Paksha.SHUKLA : Paksha.KRISHNA;
    }
}
