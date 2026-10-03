package astroapi.vedic;

import swisseph.SweConst;

/**
 * Die Körper, die in der vedischen Wertung mitreden — und nur diese.
 *
 * <p>Die sieben klassischen Grahas plus Rahu und Ketu. Uranus, Neptun, Pluto, Chiron, Lilith und
 * die Asteroiden sind ausdrücklich <b>nicht</b> dabei: sie haben in Ashtakoota, Manglik, Navamsa
 * und den Ehefaktoren keine Rolle. Sie hier aufzunehmen, würde kein vedisches Ergebnis
 * verfeinern, sondern ein westliches Chart unter vedischem Namen erzeugen.
 *
 * <p>{@link #KETU} hat keinen Ephemeriden-Index: er wird aus Rahu abgeleitet
 * (siehe {@link NodeType}).
 */
public enum VedicBody {

    SUN(SweConst.SE_SUN, "sun", true),
    MOON(SweConst.SE_MOON, "moon", true),
    MARS(SweConst.SE_MARS, "mars", true),
    MERCURY(SweConst.SE_MERCURY, "mercury", true),
    JUPITER(SweConst.SE_JUPITER, "jupiter", true),
    VENUS(SweConst.SE_VENUS, "venus", true),
    SATURN(SweConst.SE_SATURN, "saturn", true),

    /** Nordknoten. Der Ephemeriden-Index kommt aus {@link NodeType}, nicht von hier. */
    RAHU(-1, "rahu", false),

    /** Südknoten — exakt gegenüber Rahu, nicht eigenständig gerechnet. */
    KETU(-1, "ketu", false);

    private final int sweConst;
    private final String key;
    private final boolean directlyCalculated;

    VedicBody(int sweConst, String key, boolean directlyCalculated) {
        this.sweConst = sweConst;
        this.key = key;
        this.directlyCalculated = directlyCalculated;
    }

    /** Swiss-Ephemeris-Index, oder {@code -1} für die Knoten. */
    public int getSweConst() {
        return sweConst;
    }

    /** Stabiler technischer Schlüssel in API und Übersetzungen. */
    public String getKey() {
        return key;
    }

    /** Wird dieser Körper direkt über {@code swe_calc_ut} geholt? */
    public boolean isDirectlyCalculated() {
        return directlyCalculated;
    }

    /** Die sieben klassischen Grahas — die Herrscher der Rashis. */
    public static VedicBody[] classicalGrahas() {
        return new VedicBody[]{SUN, MOON, MARS, MERCURY, JUPITER, VENUS, SATURN};
    }
}
