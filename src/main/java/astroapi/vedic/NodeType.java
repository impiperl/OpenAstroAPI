package astroapi.vedic;

import swisseph.SweConst;

/**
 * Welcher Mondknoten als Rahu gilt.
 *
 * <p>Die klassische indische Ephemeride rechnet mit dem <b>mittleren</b> Knoten: Rahu läuft
 * gleichmäßig rückwärts, ohne die Librationen des wahren Knotens (bis etwa ±1,7°). Deshalb ist
 * {@link #MEAN} die Vorgabe. Ketu wird nicht berechnet, sondern exakt gegenüber abgeleitet
 * ({@code Rahu + 180°}, normalisiert) — so liegen die beiden immer genau in Opposition, auch wenn
 * eine Ephemeridenvariante das sonst um Bogensekunden verfehlen würde.
 *
 * <p>Der verwendete Typ steht in den Metadaten der Antwort und in jedem Cache-Schlüssel: Rahus
 * Zeichen entscheidet mit über Nakshatra-nahe Grenzfälle und über die Ehefaktoren.
 */
public enum NodeType {

    /** Mittlerer Mondknoten — Vorgabe, klassischer Panchang-Standard. */
    MEAN(SweConst.SE_MEAN_NODE, "mean"),

    /** Wahrer (oszillierender) Mondknoten. */
    TRUE(SweConst.SE_TRUE_NODE, "true");

    private final int sweConst;
    private final String key;

    NodeType(int sweConst, String key) {
        this.sweConst = sweConst;
        this.key = key;
    }

    public int getSweConst() {
        return sweConst;
    }

    public String getKey() {
        return key;
    }

    public static NodeType fromKey(String value) {
        if (value == null || value.isBlank()) return MEAN;
        String needle = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (NodeType t : values()) {
            if (t.key.equals(needle) || t.name().toLowerCase(java.util.Locale.ROOT).equals(needle)) return t;
        }
        throw new IllegalArgumentException("Unsupported node type: " + value);
    }
}
