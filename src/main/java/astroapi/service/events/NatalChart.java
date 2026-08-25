package astroapi.service.events;

import astroapi.dto.EphiDatenDTO;
import astroapi.utils.AngleMath;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lesehilfe auf ein mitgeschicktes Radix.
 *
 * <p>Fasst zusammen, was ein Transit braucht: die festen Längen der Radixpunkte und die Zuordnung
 * einer laufenden Länge zum betroffenen Radixhaus.
 */
public class NatalChart {

    public static final String ASCENDANT = "Ascendant";
    public static final String MEDIUM_COELI = "Medium Coeli";

    private final Map<String, Double> points = new LinkedHashMap<>();
    private final List<Double> cusps;

    public NatalChart(EphiDatenDTO chart) {
        if (chart.getPlanets() != null) {
            points.putAll(chart.getPlanets());
        }
        // Aszendent und MC sind keine Planeten, aber die am stärksten empfundenen Transitziele.
        points.put(ASCENDANT, chart.getAscendant());
        points.put(MEDIUM_COELI, chart.getMediumCoeli());

        this.cusps = chart.getCusps() != null && chart.getCusps().size() == 12 ? chart.getCusps() : null;
    }

    /** Radixpunkt-Name → Länge in Grad. */
    public Map<String, Double> points() {
        return points;
    }

    /**
     * Radixhaus, in dem eine Länge liegt (1–12), oder {@code null}, wenn keine Häuser vorliegen.
     *
     * <p>Gerechnet wird über die Bogenlänge ab jeder Spitze: Häuser sind unterschiedlich groß, ein
     * einfacher Grad-Vergleich greift deshalb nicht — und über 0° Widder hinweg schon gar nicht.
     */
    public Integer houseOf(double longitude) {
        if (cusps == null) return null;

        for (int i = 0; i < 12; i++) {
            double start = cusps.get(i);
            double end = cusps.get((i + 1) % 12);

            double houseWidth = AngleMath.norm360(end - start);
            double offset = AngleMath.norm360(longitude - start);

            // Ein Haus kann nicht 0° breit sein; ein Wert exakt auf der nächsten Spitze gehört dorthin.
            if (offset < houseWidth) return i + 1;
        }
        return null;
    }

    public boolean hasHouses() {
        return cusps != null;
    }
}
