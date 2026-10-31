package astroapi.vedic.panchanga;

/**
 * Die elf Karanas — halbe Tithis.
 *
 * <p>Ein synodischer Monat zerfällt in 60 Karana-Abschnitte zu je 6° Längenunterschied. Verteilt
 * werden sie nach einem festen, <b>ungleichmäßigen</b> Schema, und genau darin liegt die
 * Fehlerquelle: Es sind nicht elf gleich häufige Namen.
 *
 * <ul>
 *   <li>Abschnitt 0 — <b>Kimstughna</b>, einmalig am Anfang</li>
 *   <li>Abschnitte 1…56 — die sieben <b>beweglichen</b> Karanas, achtmal im Kreis</li>
 *   <li>Abschnitte 57, 58, 59 — <b>Shakuni, Chatushpada, Naga</b>, einmalig am Ende</li>
 * </ul>
 *
 * <p>1 + 56 + 3 = 60. Die vier einmaligen heißen deshalb „feste" Karanas.
 *
 * <p><b>Vishti</b> (auch Bhadra) ist das für Muhurta wichtigste: In dieser Zeit beginnt man
 * traditionell nichts Bedeutendes. Es ist das siebte der beweglichen und kommt damit achtmal im
 * Monat vor.
 *
 * <p>Wie die Tithi ist das Karana <b>ayanamsha-unabhängig</b> — es zählt die Differenz Mond − Sonne.
 */
public enum Karana {

    BAVA("bava", "Bava", true),
    BALAVA("balava", "Balava", true),
    KAULAVA("kaulava", "Kaulava", true),
    TAITILA("taitila", "Taitila", true),
    GARA("gara", "Gara", true),
    VANIJA("vanija", "Vanija", true),
    VISHTI("vishti", "Vishti", true),
    SHAKUNI("shakuni", "Shakuni", false),
    CHATUSHPADA("chatushpada", "Chatushpada", false),
    NAGA("naga", "Naga", false),
    KIMSTUGHNA("kimstughna", "Kimstughna", false);

    /** Die sieben beweglichen in ihrer festen Reihenfolge. */
    private static final Karana[] MOVABLE = { BAVA, BALAVA, KAULAVA, TAITILA, GARA, VANIJA, VISHTI };

    private final String key;
    private final String displayName;
    private final boolean movable;

    Karana(String key, String displayName, boolean movable) {
        this.key = key;
        this.displayName = displayName;
        this.movable = movable;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public boolean isMovable() { return movable; }

    /** @param absoluteIndex 0…59, gezählt ab dem Neumond */
    public static Karana ofAbsoluteIndex(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex > 59) {
            throw new IllegalArgumentException("karana index must be between 0 and 59, was " + absoluteIndex);
        }
        if (absoluteIndex == 0) return KIMSTUGHNA;
        if (absoluteIndex <= 56) return MOVABLE[(absoluteIndex - 1) % 7];
        return switch (absoluteIndex) {
            case 57 -> SHAKUNI;
            case 58 -> CHATUSHPADA;
            default -> NAGA;
        };
    }
}
