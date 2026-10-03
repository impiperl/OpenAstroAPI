package astroapi.vedic;

/**
 * Navamsa (D9) — die Neunteilung.
 *
 * <p>Jedes Rashi wird in neun Abschnitte von 3°20′ geteilt. Wo die Reihe beginnt, hängt von der
 * Bewegungsart des Zeichens ab:
 *
 * <ul>
 *   <li>beweglich (Widder, Krebs, Waage, Steinbock): beim Zeichen selbst</li>
 *   <li>fest (Stier, Löwe, Skorpion, Wassermann): beim 9. Zeichen davon</li>
 *   <li>zweigestaltig (Zwillinge, Jungfrau, Schütze, Fische): beim 5. Zeichen davon</li>
 * </ul>
 *
 * <p>Diese drei Fälle fallen zu <b>einer</b> Formel zusammen, wenn man von 0° Widder durchzählt:
 * {@code navamsa = floor(siderischeLänge / 3°20′) mod 12}. Dass das wirklich dasselbe ist, prüft
 * {@link #signIndexFromRule(Rashi, double)} gegen die Formel — und der Test prüft beide
 * gegeneinander für alle drei Bewegungsarten. Die Formel bleibt die Implementierung, weil sie
 * keine Fallunterscheidung hat und damit auch an den Grenzen nicht auseinanderlaufen kann.
 *
 * <p>Nebenbei fällt dasselbe Raster zweimal an: ein Navamsa ist genau so breit wie ein Pada
 * ({@link VedicMath#PADA_SPAN}). Das ist kein Zufall, sondern dieselbe Neunteilung von einer
 * anderen Seite betrachtet.
 */
public final class Navamsa {

    private Navamsa() {}

    /** Das Navamsa-Zeichen einer siderischen Länge. */
    public static Rashi signOf(double siderealLongitude) {
        return Rashi.byIndex(VedicMath.absolutePada(siderealLongitude) % 12);
    }

    /**
     * Welcher der neun Abschnitte innerhalb des Rashi, 1…9.
     *
     * <p>Reine Ganzzahlrechnung aus dem 108er-Abschnitt: 108 = 9 × 12, also ist der Abschnitt
     * innerhalb des Zeichens die Differenz zum Zeichenanfang. Keine zweite Division, kein
     * Auseinanderlaufen an der Grenze.
     */
    public static int divisionOf(double siderealLongitude) {
        int absolute = VedicMath.absolutePada(siderealLongitude);
        return absolute - (absolute / 9) * 9 + 1;
    }

    /**
     * Dasselbe Ergebnis über die klassische Fallunterscheidung — nur zur Prüfung der Formel,
     * nicht für den Betrieb.
     *
     * @param rashi        das D1-Zeichen
     * @param degreeInSign Grad darin, 0…30
     */
    public static Rashi signIndexFromRule(Rashi rashi, double degreeInSign) {
        int division = Math.min(9, Math.max(1, (int) Math.floor(degreeInSign / VedicMath.PADA_SPAN) + 1));
        int start;
        switch (rashi.getMobility()) {
            case MOVABLE: start = rashi.getIndex(); break;
            case FIXED:   start = (rashi.getIndex() + 8) % 12; break;  // 9. Zeichen, 1-basiert gezählt
            case DUAL:    start = (rashi.getIndex() + 4) % 12; break;  // 5. Zeichen, 1-basiert gezählt
            default: throw new IllegalStateException("Unmapped mobility: " + rashi.getMobility());
        }
        return Rashi.byIndex(start + division - 1);
    }
}
