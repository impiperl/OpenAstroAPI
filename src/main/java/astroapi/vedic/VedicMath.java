package astroapi.vedic;

/**
 * Winkel- und Zählhilfen der vedischen Domäne.
 *
 * <p>Eigene Klasse und nicht {@code astroapi.utils.AngleMath}, damit die vedischen Grenzregeln
 * (Nakshatra, Pada, Navamsa) an einer Stelle stehen und beim Lesen nachvollziehbar bleiben.
 * Alle Methoden sind rein — kein Zustand, keine Ephemeride.
 */
public final class VedicMath {

    private VedicMath() {}

    /** Ein Nakshatra: 360° / 27 = 13°20′. */
    public static final double NAKSHATRA_SPAN = 360.0 / 27.0;

    /** Ein Pada: ein Viertel Nakshatra = 3°20′. Gleich der Navamsa-Breite. */
    public static final double PADA_SPAN = NAKSHATRA_SPAN / 4.0;

    /**
     * Normalisiert auf [0, 360).
     *
     * <p>Der Fallstrick steht genau hier: {@code 360.0} muss zu {@code 0.0} werden und darf nicht
     * als 360 durchrutschen, sonst liefert jede nachfolgende Division Index 27 statt 0 — also ein
     * 28. Nakshatra, das es nicht gibt. Ebenso muss {@code -0.000001} zu einem Wert knapp unter
     * 360 werden und nicht zu {@code 360.0} selbst; der Nachlauf deckt den Fall ab, in dem die
     * Fließkomma-Addition genau auf 360 aufrundet.
     */
    public static double normalizeDegrees(double degrees) {
        double d = degrees % 360.0;
        if (d < 0) d += 360.0;
        if (d >= 360.0) d = 0.0;
        return d;
    }

    /**
     * Der Index des Abschnitts, in dem eine Länge liegt, wenn der Kreis in {@code divisions}
     * gleiche Teile zerlegt wird — 0 bis {@code divisions - 1}.
     *
     * <p><b>Das ist die einzige Division der ganzen Domäne</b>, und das aus einem Grund, der sich
     * im Test gezeigt hat: Nakshatra-, Pada- und Navamsa-Breite sind in {@code double} nicht
     * darstellbar (13°20′ ist 13,333…). Rechnet man „Länge minus Nakshatra-Anfang, geteilt durch
     * Pada-Breite", löscht die Subtraktion führende Stellen aus, und genau auf einer Grenze kippt
     * das Ergebnis um eins: bei 20,0° siderisch ergab das Pada 2 statt Pada 3, während die
     * Nakshatra-Zuordnung schon weitergezählt hatte. Zwei Angaben, die sich widersprechen.
     *
     * <p>Mit {@code Länge × divisions / 360} verschwindet das Problem: die Multiplikation mit einer
     * ganzen Zahl und die Division durch 360 sind beide exakt, solange die Länge selbst exakt ist.
     * Und weil Rashi, Nakshatra, Pada und Navamsa anschließend <b>alle</b> aus derselben
     * 108er-Zahl abgeleitet werden (108 = 4 × 27 = 9 × 12), können sie einander nicht mehr
     * widersprechen — die Zuordnung ist eine Ganzzahlrechnung, keine zweite Division.
     */
    public static int absoluteIndex(double siderealLongitude, int divisions) {
        double normalized = normalizeDegrees(siderealLongitude);
        int index = (int) Math.floor(normalized * divisions / 360.0);
        if (index < 0) return 0;
        if (index >= divisions) return divisions - 1;
        return index;
    }

    /** Die 108 gemeinsamen Abschnitte: ein Pada und ein Navamsa sind dasselbe Raster. */
    public static final int PADA_DIVISIONS = 108;

    /** Der Pada-/Navamsa-Abschnitt einer Länge, 0…107 — die Grundlage aller weiteren Zuordnungen. */
    public static int absolutePada(double siderealLongitude) {
        return absoluteIndex(siderealLongitude, PADA_DIVISIONS);
    }

    /**
     * Grad innerhalb des Rashi, 0 (einschließlich) bis 30 (ausschließlich).
     *
     * <p>Wird aus dem Rashi-Index abgeleitet und nicht über {@code % 30}, damit Gradangabe und
     * Zeichen am Rand nicht auseinanderlaufen.
     */
    public static double degreeInSign(double siderealLongitude) {
        double normalized = normalizeDegrees(siderealLongitude);
        int rashiIndex = absolutePada(normalized) / 9;
        double inSign = normalized - rashiIndex * 30.0;
        if (inSign < 0) return 0.0;
        if (inSign >= 30.0) return Math.nextDown(30.0);
        return inSign;
    }

    /** Grad als {@code 12°34′56″} — für Anzeige und Protokoll, nie für Rechnung. */
    public static String formatDegreeMinuteSecond(double degrees) {
        double d = Math.abs(degrees);
        int deg = (int) d;
        double minutesFull = (d - deg) * 60.0;
        int min = (int) minutesFull;
        double sec = (minutesFull - min) * 60.0;
        // Rundung auf ganze Sekunden darf nicht 60″ oder 60′ erzeugen.
        int secRounded = (int) Math.round(sec);
        if (secRounded == 60) { secRounded = 0; min++; }
        if (min == 60) { min = 0; deg++; }
        return String.format("%d°%02d′%02d″", deg, min, secRounded);
    }

    /**
     * Zählt 1-basiert und inklusiv von einem Nakshatra zum anderen, wie die Tara-Regel es verlangt:
     * dasselbe Nakshatra ergibt 1, das nächste 2, und nach 27 beginnt es wieder bei 1.
     *
     * @param fromIndex 1…27
     * @param toIndex   1…27
     * @return 1…27
     */
    public static int countNakshatras(int fromIndex, int toIndex) {
        return (((toIndex - fromIndex) % 27) + 27) % 27 + 1;
    }
}
