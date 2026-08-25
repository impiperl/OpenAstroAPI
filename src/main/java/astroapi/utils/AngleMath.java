package astroapi.utils;

/**
 * Winkelrechnung für die Event-Detektoren.
 *
 * <p>Jede Ereigniserkennung hier ist im Kern eine Nullstellensuche über einer Winkeldifferenz.
 * Damit die Bisektion funktioniert, muss die Funktion im Suchintervall <b>stetig</b> sein — und
 * genau daran scheitert naives Rechnen mit Graden, weil bei 360° gesprungen wird. Deshalb liefern
 * die Differenzen hier immer einen Wert in [-180, 180).
 */
public final class AngleMath {

    private AngleMath() {}

    /** Normiert einen Winkel auf [0, 360). */
    public static double norm360(double degrees) {
        double d = degrees % 360.0;
        return d < 0 ? d + 360.0 : d;
    }

    /**
     * Vorzeichenbehaftete Differenz {@code a - b}, normiert auf [-180, 180).
     *
     * <p>Das ist die Funktion, auf der alle Detektoren ihre Nullstellen suchen: Sie wechselt beim
     * Durchgang durch den Zielwinkel das Vorzeichen und springt dabei nicht.
     */
    public static double signedDelta(double a, double b) {
        double d = (a - b) % 360.0;
        if (d >= 180.0)  d -= 360.0;
        if (d < -180.0)  d += 360.0;
        return d;
    }

    /** Kleinster Winkelabstand zweier Längen, in [0, 180]. */
    public static double separation(double a, double b) {
        return Math.abs(signedDelta(a, b));
    }

    /** Startgrad des Zeichens, in dem die Länge liegt (0, 30, 60, …). */
    public static double signStart(double longitude) {
        return Math.floor(norm360(longitude) / 30.0) * 30.0;
    }

    /** Index des Zeichens (0 = Widder … 11 = Fische). */
    public static int signIndex(double longitude) {
        return (int) Math.floor(norm360(longitude) / 30.0);
    }

    /** Grad innerhalb des Zeichens, in [0, 30). */
    public static double degreeInSign(double longitude) {
        return norm360(longitude) - signStart(longitude);
    }
}
