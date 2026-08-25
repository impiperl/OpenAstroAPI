package astroapi.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class CrossingScanTest {

    @Test
    void findsSingleRootOfALinearFunction() {
        DoubleUnaryOperator f = x -> x - 10.5;
        List<Double> roots = CrossingScan.scan(f, 0, 20, 1.0);

        assertEquals(1, roots.size());
        assertEquals(10.5, roots.get(0), CrossingFinder.ONE_MINUTE);
    }

    @Test
    void findsEveryRootOfAPeriodicFunction() {
        // Sinus mit Periode 10 → Nullstellen bei 0, 5, 10, 15, 20
        DoubleUnaryOperator f = x -> Math.sin(Math.PI * x / 5.0);
        List<Double> roots = CrossingScan.scan(f, 0.5, 19.5, 1.0);

        assertEquals(List.of(5.0, 10.0, 15.0), roots.stream()
                .map(r -> Math.round(r * 1000.0) / 1000.0)
                .collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void ignoresRootsOutsideTheWindow() {
        DoubleUnaryOperator f = x -> x - 25.0;
        assertTrue(CrossingScan.scan(f, 0, 20, 1.0).isEmpty());
    }

    @Test
    void returnsNothingWhenTheFunctionNeverChangesSign() {
        DoubleUnaryOperator f = x -> 1.0;
        assertTrue(CrossingScan.scan(f, 0, 20, 1.0).isEmpty());
    }

    /**
     * Der Grund für {@code nearLimit}: eine Winkeldifferenz springt am Gegenpunkt von +180 auf −180.
     * Das ist ein Vorzeichenwechsel, aber keine Nullstelle.
     */
    @Test
    void nearLimit_rejectsTheWrapAtTheAntipode() {
        // Ein Körper läuft gleichmäßig; betrachtet wird die Differenz zu 0°.
        DoubleUnaryOperator f = x -> AngleMath.signedDelta(x * 30.0, 0.0);

        List<Double> withoutLimit = CrossingScan.scan(f, 0.1, 23.9, 1.0);
        List<Double> withLimit = CrossingScan.scan(f, 0.1, 23.9, 1.0, 90.0);

        // Ohne Schranke wird jeder Durchgang durch den Gegenpunkt fälschlich mitgezählt.
        assertTrue(withoutLimit.size() > withLimit.size(),
                "ohne Schranke müssen zusätzliche Scheinnullstellen auftauchen");

        // Mit Schranke bleiben nur die echten Durchgänge durch 0° übrig: bei x = 12 und x = 24.
        for (double root : withLimit) {
            assertEquals(0.0, Math.abs(AngleMath.signedDelta(root * 30.0, 0.0)), 0.05,
                    "jede gefundene Nullstelle muss tatsächlich bei 0° liegen");
        }
    }

    /**
     * Liegt die Nullstelle exakt auf einem Rasterpunkt, klammern sowohl das endende als auch das
     * beginnende Intervall sie ein — ohne Entdopplung stünde dasselbe Ereignis zweimal im Feed.
     */
    @Test
    void rootExactlyOnAGridPointIsReportedOnlyOnce() {
        DoubleUnaryOperator f = x -> x - 10.0;   // f(10.0) ist exakt 0
        List<Double> roots = CrossingScan.scan(f, 0, 20, 1.0);

        assertEquals(1, roots.size(), "gefunden: " + roots);
        assertEquals(10.0, roots.get(0), 1e-9);
    }

    @Test
    void interruptedScanStopsInsteadOfComputingOn() {
        DoubleUnaryOperator f = x -> Math.sin(x);
        Thread.currentThread().interrupt();
        try {
            assertThrows(ScanCancelledException.class, () -> CrossingScan.scan(f, 0, 10_000, 0.001));
            assertTrue(Thread.currentThread().isInterrupted(),
                    "der Interrupt-Status muss erhalten bleiben, damit der Pool-Thread ihn nicht verliert");
        } finally {
            // Flag löschen, sonst schlägt der nächste Test in diesem Thread fehl.
            Thread.interrupted();
        }
    }

    @Test
    void findRoot_refusesAnIntervalWithoutSignChange() {
        assertTrue(Double.isNaN(CrossingFinder.findRoot(x -> x + 1.0, 0, 10, 1e-6)));
    }

    @Test
    void findRoot_convergesToTheRequestedTolerance() {
        double root = CrossingFinder.findRoot(x -> Math.cos(x), 1.0, 2.0, 1e-9);
        assertEquals(Math.PI / 2.0, root, 1e-8);
    }
}
