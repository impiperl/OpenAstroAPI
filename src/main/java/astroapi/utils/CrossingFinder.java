package astroapi.utils;

import java.util.function.DoubleUnaryOperator;

/**
 * Nullstellensuche über Julianischen Tagen.
 *
 * <p>Alle exakten Ereigniszeitpunkte — Mondphase, Station, Ingress, Sonnenwende, exakter Aspekt —
 * sind derselbe Vorgang: eine stetige Funktion der Zeit wechselt das Vorzeichen, und gesucht ist
 * der Moment des Wechsels. Grob abtasten, Vorzeichenwechsel finden, dann einschachteln.
 *
 * <p>Bisektion statt Newton: die Ableitung ist hier nicht analytisch verfügbar, und Bisektion
 * konvergiert garantiert, solange das Intervall wirklich einen Vorzeichenwechsel enthält. Bei
 * 1-Minuten-Toleranz auf einem Tagesintervall sind das rund elf Auswertungen — billig genug.
 */
public final class CrossingFinder {

    /** Eine Minute, ausgedrückt in Julianischen Tagen. */
    public static final double ONE_MINUTE = 1.0 / 1440.0;

    private CrossingFinder() {}

    /**
     * Sucht die Nullstelle von {@code f} im Intervall [jdLow, jdHigh].
     *
     * <p>Vorbedingung: {@code f(jdLow)} und {@code f(jdHigh)} haben verschiedene Vorzeichen. Ist das
     * nicht der Fall, liefert die Methode {@link Double#NaN} — ein stiller Fantasiewert wäre hier
     * schlimmer als gar kein Ereignis.
     *
     * @param tolerance gewünschte Genauigkeit in Tagen, z. B. {@link #ONE_MINUTE}
     */
    public static double findRoot(DoubleUnaryOperator f, double jdLow, double jdHigh, double tolerance) {
        double low = jdLow;
        double high = jdHigh;
        double fLow = f.applyAsDouble(low);
        double fHigh = f.applyAsDouble(high);

        if (Double.isNaN(fLow) || Double.isNaN(fHigh)) return Double.NaN;
        if (fLow == 0.0) return low;
        if (fHigh == 0.0) return high;
        if (Math.signum(fLow) == Math.signum(fHigh)) return Double.NaN;

        // Harte Obergrenze an Iterationen: schützt vor Endlosschleifen, falls f unstetig ist.
        for (int i = 0; i < 60 && (high - low) > tolerance; i++) {
            double mid = (low + high) / 2.0;
            double fMid = f.applyAsDouble(mid);
            if (Double.isNaN(fMid)) return Double.NaN;
            if (fMid == 0.0) return mid;

            if (Math.signum(fMid) == Math.signum(fLow)) {
                low = mid;
                fLow = fMid;
            } else {
                high = mid;
            }
        }
        return (low + high) / 2.0;
    }

    /** Prüft, ob zwischen zwei Funktionswerten ein echter Vorzeichenwechsel liegt. */
    public static boolean brackets(double fA, double fB) {
        if (Double.isNaN(fA) || Double.isNaN(fB)) return false;
        if (fA == 0.0 || fB == 0.0) return true;
        return Math.signum(fA) != Math.signum(fB);
    }
}
