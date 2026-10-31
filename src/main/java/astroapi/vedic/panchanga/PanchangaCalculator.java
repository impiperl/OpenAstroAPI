package astroapi.vedic.panchanga;

import astroapi.vedic.Nakshatra;
import astroapi.vedic.VedicMath;

/**
 * Die fünf Glieder des vedischen Tages aus zwei Längengraden.
 *
 * <p>Reine Arithmetik, keine Ephemeride: Hereingegeben werden die <b>siderischen</b> Längen von
 * Sonne und Mond sowie der Kalendertag des Sonnenaufgangs. Das hält die Regeln prüfbar — die
 * gesamte Klasse lässt sich ohne Swiss Ephemeris testen, und die Beispiele aus der Literatur
 * lassen sich direkt nachrechnen.
 *
 * <h2>Was wovon abhängt</h2>
 * Das ist die wichtigste Unterscheidung dieser Klasse, und sie wird oft übersehen:
 *
 * <table>
 *   <caption>Abhängigkeit vom Ayanamsha</caption>
 *   <tr><th>Glied</th><th>gebildet aus</th><th>Ayanamsha</th></tr>
 *   <tr><td>Tithi</td><td>Mond − Sonne</td><td>egal</td></tr>
 *   <tr><td>Karana</td><td>Mond − Sonne</td><td>egal</td></tr>
 *   <tr><td>Nakshatra</td><td>Mond</td><td><b>entscheidend</b></td></tr>
 *   <tr><td>Yoga</td><td>Mond + Sonne</td><td><b>entscheidend, doppelt</b></td></tr>
 *   <tr><td>Vara</td><td>Sonnenaufgang</td><td>egal</td></tr>
 * </table>
 *
 * <p>Bei einer Differenz hebt sich eine Verschiebung des Nullpunkts auf, bei einer Summe
 * verdoppelt sie sich. Ein mit tropischen Längen gerechnetes Yoga liegt deshalb rund 48° und
 * damit fast vier Abschnitte daneben, während die Tithi zufällig stimmt — die häufigste Art,
 * einen halb richtigen Panchanga-Rechner zu bauen.
 *
 * <h2>Was hier nicht passiert</h2>
 * Die Wechselzeiten. Ein Glied gilt, bis der zugehörige Winkel die nächste Grenze überschreitet,
 * und wann das ist, lässt sich aus einem einzelnen Zeitpunkt nicht sagen: Der Mond läuft
 * ungleichmäßig. Dafür braucht es eine Nullstellensuche über die Zeit, und die gehört zum
 * Ephemeridendienst, nicht hierher. Diese Klasse liefert aber den Fortschritt innerhalb des
 * laufenden Abschnitts ({@code fraction}) — daraus wird die erste Schätzung für die Suche.
 */
public final class PanchangaCalculator {

    /** Eine Tithi umfasst 12° Längenunterschied; 30 davon sind ein synodischer Monat. */
    public static final double TITHI_SPAN = 12.0;

    /** Ein Karana ist eine halbe Tithi. */
    public static final double KARANA_SPAN = TITHI_SPAN / 2.0;

    /** Ein Yoga umfasst 13°20′ der Summe — dieselbe Spanne wie ein Nakshatra, andere Größe. */
    public static final double YOGA_SPAN = 360.0 / 27.0;

    private PanchangaCalculator() { }

    /**
     * Der Längenunterschied Mond − Sonne, auf 0…360° normalisiert.
     *
     * <p>0° ist Neumond, 180° Vollmond. Diese eine Zahl trägt Tithi und Karana.
     */
    public static double elongation(double sunSidereal, double moonSidereal) {
        return VedicMath.normalizeDegrees(moonSidereal - sunSidereal);
    }

    /** 0…29, gezählt ab dem Neumond. */
    public static int tithiIndex(double sunSidereal, double moonSidereal) {
        return boundedIndex(elongation(sunSidereal, moonSidereal) / TITHI_SPAN, 30);
    }

    /** 0…59, gezählt ab dem Neumond. */
    public static int karanaIndex(double sunSidereal, double moonSidereal) {
        return boundedIndex(elongation(sunSidereal, moonSidereal) / KARANA_SPAN, 60);
    }

    /**
     * 0…26.
     *
     * <p>Beide Längen müssen siderisch sein — siehe die Tabelle oben.
     */
    public static int yogaIndex(double sunSidereal, double moonSidereal) {
        double sum = VedicMath.normalizeDegrees(sunSidereal + moonSidereal);
        return boundedIndex(sum / YOGA_SPAN, 27);
    }

    /** Fortschritt innerhalb der laufenden Tithi, 0…1. */
    public static double tithiFraction(double sunSidereal, double moonSidereal) {
        return fractionOf(elongation(sunSidereal, moonSidereal), TITHI_SPAN);
    }

    /** Fortschritt innerhalb des laufenden Karana, 0…1. */
    public static double karanaFraction(double sunSidereal, double moonSidereal) {
        return fractionOf(elongation(sunSidereal, moonSidereal), KARANA_SPAN);
    }

    /** Fortschritt innerhalb des laufenden Yoga, 0…1. */
    public static double yogaFraction(double sunSidereal, double moonSidereal) {
        return fractionOf(VedicMath.normalizeDegrees(sunSidereal + moonSidereal), YOGA_SPAN);
    }

    /** Fortschritt des Mondes innerhalb seines Nakshatras, 0…1. */
    public static double nakshatraFraction(double moonSidereal) {
        return fractionOf(VedicMath.normalizeDegrees(moonSidereal), VedicMath.NAKSHATRA_SPAN);
    }

    /**
     * Alle fünf Glieder zu einem Zeitpunkt.
     *
     * @param sunSidereal  siderische Länge der Sonne in Grad
     * @param moonSidereal siderische Länge des Mondes in Grad
     * @param sunriseDate  Kalendertag des Sonnenaufgangs, der diesen vedischen Tag eröffnet —
     *                     <b>nicht</b> der Kalendertag des gefragten Zeitpunkts
     */
    public static Panchanga at(double sunSidereal, double moonSidereal, java.time.LocalDate sunriseDate) {
        int tithiIdx = tithiIndex(sunSidereal, moonSidereal);
        int karanaIdx = karanaIndex(sunSidereal, moonSidereal);
        int yogaIdx = yogaIndex(sunSidereal, moonSidereal);
        // Nakshatra und Pada über dieselbe exakte Division wie überall sonst in der vedischen
        // Rechnung; eine zweite Formel hier wäre eine zweite Gelegenheit, an der Grenze
        // danebenzuliegen — genau der Fehler, der im Juli zwischen Pada und Nakshatra auftrat.
        int absolutePada = VedicMath.absolutePada(moonSidereal);

        return new Panchanga(
                tithiIdx + 1, Tithi.ofAbsoluteIndex(tithiIdx), Tithi.pakshaOfAbsoluteIndex(tithiIdx),
                tithiFraction(sunSidereal, moonSidereal),
                karanaIdx + 1, Karana.ofAbsoluteIndex(karanaIdx),
                karanaFraction(sunSidereal, moonSidereal),
                yogaIdx + 1, NityaYoga.ofAbsoluteIndex(yogaIdx),
                yogaFraction(sunSidereal, moonSidereal),
                Nakshatra.ofLongitude(moonSidereal),
                absolutePada % 4 + 1,
                nakshatraFraction(moonSidereal),
                Vara.ofSunriseDate(sunriseDate));
    }

    /**
     * Teilt und deckelt — dieselbe Vorsicht wie in {@link VedicMath#absoluteIndex}.
     *
     * <p>Ohne den Deckel liefert ein Winkel, der durch Rundung minimal über 360° liegt, einen
     * Index außerhalb der Tabelle. Das passiert selten und dann ausgerechnet am Monatswechsel.
     */
    private static int boundedIndex(double raw, int divisions) {
        int index = (int) Math.floor(raw);
        if (index < 0) return 0;
        if (index >= divisions) return divisions - 1;
        return index;
    }

    private static double fractionOf(double angle, double span) {
        double fraction = (angle % span) / span;
        return fraction < 0 ? fraction + 1.0 : fraction;
    }
}
