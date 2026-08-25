package astroapi.service.events;

import astroapi.dto.EphiDatenDTO;
import astroapi.utils.EphemerisPort;

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

    public EventScanContext(EphemerisPort ephemeris, double jdFrom, double jdTo, EphiDatenDTO natalChart) {
        this.ephemeris = ephemeris;
        this.jdFrom = jdFrom;
        this.jdTo = jdTo;
        this.natalChart = natalChart;
    }

    public EphemerisPort ephemeris() { return ephemeris; }
    public double jdFrom() { return jdFrom; }
    public double jdTo() { return jdTo; }
    public EphiDatenDTO natalChart() { return natalChart; }

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
