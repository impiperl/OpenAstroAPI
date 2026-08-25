package astroapi.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Tastet einen Zeitraum ab und liefert alle exakten Nullstellen darin.
 *
 * <p>Das ist das gemeinsame Skelett aller Detektoren: grob abtasten, Vorzeichenwechsel finden,
 * einschachteln. Was sich unterscheidet, ist allein die Funktion — Elongation zum Zielwinkel,
 * Geschwindigkeit, Abstand zur Zeichengrenze, Winkeldifferenz zum Aspekt.
 */
public final class CrossingScan {

    private CrossingScan() {}

    /**
     * @param f         stetige Funktion über Julianischen Tagen
     * @param stepDays  Rasterweite; muss fein genug sein, dass zwischen zwei Punkten höchstens eine
     *                  Nullstelle liegt
     * @param nearLimit Nur Vorzeichenwechsel zählen, bei denen beide Randwerte betragsmäßig darunter
     *                  liegen. Für Winkelfunktionen aus {@link AngleMath#signedDelta} verhindert das,
     *                  dass der Sprung von +180 auf −180 am Gegenpunkt als Nullstelle gilt. Wer keine
     *                  Winkelfunktion abtastet, übergibt {@link Double#MAX_VALUE}.
     * @return exakte Zeitpunkte, aufsteigend; nur Werte innerhalb von [jdFrom, jdTo]
     */
    public static List<Double> scan(DoubleUnaryOperator f, double jdFrom, double jdTo,
                                    double stepDays, double nearLimit) {
        List<Double> roots = new ArrayList<>();
        if (jdTo <= jdFrom || stepDays <= 0) return roots;

        double previousJd = jdFrom;
        double previousValue = f.applyAsDouble(previousJd);

        while (previousJd < jdTo) {
            // Ohne diese Prüfung rechnet ein abgebrochener Scan im Hintergrund weiter und belegt
            // einen Pool-Thread, auf dessen Ergebnis niemand mehr wartet.
            ScanCancelledException.throwIfInterrupted("crossing scan");

            double currentJd = Math.min(previousJd + stepDays, jdTo);
            double currentValue = f.applyAsDouble(currentJd);

            boolean withinLimit = Math.abs(previousValue) < nearLimit && Math.abs(currentValue) < nearLimit;
            if (withinLimit && CrossingFinder.brackets(previousValue, currentValue)) {
                double root = CrossingFinder.findRoot(f, previousJd, currentJd, CrossingFinder.ONE_MINUTE);
                if (!Double.isNaN(root) && root >= jdFrom && root <= jdTo && !isDuplicate(roots, root)) {
                    roots.add(root);
                }
            }

            previousJd = currentJd;
            previousValue = currentValue;
        }
        return roots;
    }

    /** Variante ohne Winkelschranke. */
    public static List<Double> scan(DoubleUnaryOperator f, double jdFrom, double jdTo, double stepDays) {
        return scan(f, jdFrom, jdTo, stepDays, Double.MAX_VALUE);
    }

    /**
     * Fängt die Nullstelle ab, die genau auf einem Rasterpunkt liegt.
     *
     * <p>Trifft {@code f} dort exakt die Null, klammert sowohl das endende als auch das beginnende
     * Intervall sie ein — {@code findRoot} liefert beide Male denselben Zeitpunkt. Ohne diese Prüfung
     * stünde dasselbe Ereignis zweimal im Feed, und spätestens beim Persistieren wäre unklar, welches
     * der beiden das richtige ist.
     *
     * <p>Die Toleranz entspricht der Genauigkeit der Nullstellensuche selbst: näher als eine Minute
     * beieinander liegende Treffer sind derselbe Durchgang, nicht zwei.
     */
    private static boolean isDuplicate(List<Double> roots, double candidate) {
        if (roots.isEmpty()) return false;
        double last = roots.get(roots.size() - 1);
        return Math.abs(candidate - last) <= CrossingFinder.ONE_MINUTE;
    }
}
