package astroapi.service.events;

import astroapi.dto.EphiDatenDTO;
import astroapi.utils.Bodies;
import astroapi.utils.EphemerisPort;
import astroapi.utils.Planet;

import java.util.List;

/**
 * Alles, was ein Detektor für einen Durchlauf braucht.
 *
 * <p>Der Zeitraum steht als Julianisches Datum drin — innerhalb der Detektoren wird bewusst nicht
 * mehr mit Kalenderzeit gerechnet.
 */
public class EventScanContext {

    private final EphemerisPort ephemeris;
    private final double jdFrom;
    private final double jdTo;
    private final EphiDatenDTO natalChart;
    private final List<Planet> bodies;

    /**
     * Ohne Körperliste gilt der klassische Satz. Das betrifft vor allem Detektor-Tests: sie sollen
     * nicht stillschweigend mitwachsen, wenn im Betrieb ein Körper dazukommt.
     */
    public EventScanContext(EphemerisPort ephemeris, double jdFrom, double jdTo, EphiDatenDTO natalChart) {
        this(ephemeris, jdFrom, jdTo, natalChart, Bodies.CLASSIC);
    }

    public EventScanContext(EphemerisPort ephemeris, double jdFrom, double jdTo, EphiDatenDTO natalChart,
                            List<Planet> bodies) {
        this.ephemeris = ephemeris;
        this.jdFrom = jdFrom;
        this.jdTo = jdTo;
        this.natalChart = natalChart;
        this.bodies = bodies == null || bodies.isEmpty() ? Bodies.CLASSIC : List.copyOf(bodies);
    }

    public EphemerisPort ephemeris() { return ephemeris; }
    public double jdFrom() { return jdFrom; }
    public double jdTo() { return jdTo; }
    public EphiDatenDTO natalChart() { return natalChart; }

    /** Die Körper, die in diesem Scan berücksichtigt werden. */
    public List<Planet> bodies() { return bodies; }

    public boolean hasNatalChart() {
        return natalChart != null && natalChart.getPlanets() != null && !natalChart.getPlanets().isEmpty();
    }

    /** Länge des Zeitraums in Tagen. */
    public double spanDays() {
        return jdTo - jdFrom;
    }

    /** Liegt der Zeitpunkt im angefragten Fenster? Randereignisse werden inklusive behandelt. */
    public boolean contains(double julianDay) {
        return !Double.isNaN(julianDay) && julianDay >= jdFrom && julianDay <= jdTo;
    }
}
