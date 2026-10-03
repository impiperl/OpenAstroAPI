package astroapi.vedic;

import swisseph.SweConst;

/**
 * Die Ayanamsha-Varianten, die für die vedische Berechnung zugelassen sind.
 *
 * <p>Die Ayanamsha ist der Winkel zwischen dem tropischen Nullpunkt (Frühlingspunkt) und dem
 * siderischen Nullpunkt. Sie wächst um etwa 50,3″ im Jahr. Welche Variante gewählt wird,
 * entscheidet über die Zeichengrenzen — und damit über jedes Rashi, jedes Nakshatra und jeden
 * Koota-Punkt. Deshalb steht die Wahl ausdrücklich in Anfrage <b>und</b> Antwort und ist Teil
 * jedes Cache-Schlüssels.
 *
 * <p><b>Vorgabe Lahiri</b> (Chitrapaksha). Swiss Ephemeris definiert sie über
 * {@code t0 = JD 2415020.0} (1900) mit {@code ayan_t0 = 22°27′37,70″}; das ist die Definition des
 * indischen Rashtriya Panchang. Gemessen am 1. Januar 2000, 0:00 UT ergibt sich der angewandte
 * Wert 23°51′11,4″.
 *
 * <p><b>Nicht erweitern, ohne die Regelversion zu erhöhen.</b> Jede zusätzliche Variante ist eine
 * neue Zeichengrenze und damit ein anderes Ergebnis.
 */
public enum Ayanamsa {

    /** Lahiri / Chitrapaksha — Standard der indischen Ephemeride. */
    LAHIRI(SweConst.SE_SIDM_LAHIRI, "lahiri"),

    /** B. V. Raman. */
    RAMAN(SweConst.SE_SIDM_RAMAN, "raman"),

    /** K. S. Krishnamurti (KP). */
    KRISHNAMURTI(SweConst.SE_SIDM_KRISHNAMURTI, "krishnamurti"),

    /** Spica/Chitra exakt auf 0° Waage. */
    TRUE_CITRA(SweConst.SE_SIDM_TRUE_CITRA, "true_citra"),

    /** Fagan/Bradley — westlich-siderisch, nicht vedisch; nur für Vergleiche. */
    FAGAN_BRADLEY(SweConst.SE_SIDM_FAGAN_BRADLEY, "fagan_bradley");

    private final int sidMode;
    private final String key;

    Ayanamsa(int sidMode, String key) {
        this.sidMode = sidMode;
        this.key = key;
    }

    /** Der {@code sid_mode} für {@code swe_set_sid_mode(...)}. */
    public int getSidMode() {
        return sidMode;
    }

    /** Stabiler technischer Schlüssel für API, Cache und Übersetzungen. */
    public String getKey() {
        return key;
    }

    public static Ayanamsa fromKey(String value) {
        if (value == null || value.isBlank()) return LAHIRI;
        String needle = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (Ayanamsa a : values()) {
            if (a.key.equals(needle) || a.name().toLowerCase(java.util.Locale.ROOT).equals(needle)) return a;
        }
        throw new IllegalArgumentException("Unsupported ayanamsa: " + value);
    }
}
