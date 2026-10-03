package astroapi.vedic;

/**
 * Welche der beiden Personen in den <b>asymmetrischen</b> Kootas die traditionelle Rolle „Braut"
 * einnimmt.
 *
 * <p>Vier der acht Kootas sind richtungsabhängig — Varna, Vashya, Yoni und Gana. Dieselben zwei
 * Geburtsdaten ergeben je nach Reihenfolge eine andere Punktzahl. Das ist eine Eigenschaft der
 * historischen Tabellen, keine Aussage über die Menschen.
 *
 * <p><b>Die Rolle wird ausschließlich hier gesetzt.</b> Sie wird nie aus Namen, Profil, Geschlecht
 * oder Begleiter-Persona abgeleitet — eine Ableitung wäre eine Behauptung über Menschen, die die
 * Berechnung nicht treffen darf, und würde gleichgeschlechtliche und nichtbinäre Paare technisch
 * ausschließen.
 *
 * <p>Vorgabe ist {@link #BOTH}: beide Richtungen werden gerechnet und beide ausgegeben. Erst wenn
 * jemand eine Richtung ausdrücklich wählt, wird nur sie berechnet.
 */
public enum TraditionalRole {

    /** Person A nimmt die Braut-Rolle der Tabellen ein, Person B die des Bräutigams. */
    A_AS_BRIDE("a_as_bride"),

    /** Umgekehrt. */
    B_AS_BRIDE("b_as_bride"),

    /** Beide Richtungen rechnen und beide zeigen — Vorgabe. */
    BOTH("both");

    private final String key;

    TraditionalRole(String key) { this.key = key; }

    public String getKey() { return key; }

    public static TraditionalRole fromKey(String value) {
        if (value == null || value.isBlank()) return BOTH;
        String needle = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (TraditionalRole r : values()) {
            if (r.key.equals(needle) || r.name().toLowerCase(java.util.Locale.ROOT).equals(needle)) return r;
        }
        throw new IllegalArgumentException("Unsupported traditional role: " + value);
    }
}
