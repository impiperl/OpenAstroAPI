package astroapi.vedic;

import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicWarningDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Vorbehalte, die an einem einzelnen Chart hängen — gemeinsam für Partneranalyse und Kundli.
 *
 * <p>Sie sind kein Beiwerk. Eine fehlende Geburtszeit nimmt dem Aszendenten, den Häusern und dem
 * Lagna-Bezug des Manglik die Grundlage; ein grenznaher Mond kann das Nakshatra und alles, was
 * daran hängt, kippen. Beides gehört in jede Oberfläche und in jedes gespeicherte Ergebnis.
 */
public final class VedicWarnings {

    private VedicWarnings() {}

    /**
     * Ab welchem Abstand zur Zeichengrenze das Lagna als grenznah gilt, in Grad.
     *
     * <p>Der Aszendent wandert im Mittel etwa ein Grad in vier Minuten — ein Grad entspricht also
     * ungefähr der Unschärfe einer gerundeten Geburtszeit. Je nach Zeichen und Breite ist er
     * schneller oder langsamer; die Minutenangabe der Warnung ist deshalb ausdrücklich ungefähr.
     */
    public static final double LAGNA_BOUNDARY_WARN_DEGREES = 1.0;

    /** Unbekannte Geburtszeit und grenznaher Mond — was jedes Chart mitbringt. */
    public static List<VedicWarningDTO> forChart(VedicChartDTO chart) {
        List<VedicWarningDTO> warnings = new ArrayList<>();
        if (!chart.isBirthTimeKnown()) {
            warnings.add(new VedicWarningDTO("birth_time_unknown", "caution", chart.getLabel(),
                    Map.of()));
        }
        double remaining = chart.getMoonNakshatra().getRemainingDegrees();
        double entered = chart.getMoonNakshatra().getDegreeInNakshatra();
        double distance = Math.min(remaining, entered);
        if (distance <= VedicEphemerisService.NAKSHATRA_BOUNDARY_WARN_DEGREES) {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("degrees", Math.round(distance * 1000.0) / 1000.0);
            // Der Mond läuft etwa 13,2° am Tag; daraus die Zeitspanne, die den Befund kippt.
            args.put("approxMinutes", (int) Math.round(distance / 13.2 * 24 * 60));
            warnings.add(new VedicWarningDTO("moon_near_nakshatra_boundary", "caution",
                    chart.getLabel(), args));
        }
        return warnings;
    }

    /**
     * Lagna nahe einer Zeichengrenze — nur in der Kundli, weil nur dort das Lagna ein eigenes
     * Kernthema ist. Ohne Geburtszeit gibt es kein Lagna und damit auch diese Warnung nicht.
     */
    public static List<VedicWarningDTO> lagnaBoundary(VedicChartDTO chart) {
        if (!chart.isTimeDependentAvailable() || chart.getAscendantDegreeInSign() == null) {
            return List.of();
        }
        double inSign = chart.getAscendantDegreeInSign();
        double distance = Math.min(inSign, 30.0 - inSign);
        if (distance > LAGNA_BOUNDARY_WARN_DEGREES) return List.of();
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("degrees", Math.round(distance * 1000.0) / 1000.0);
        args.put("approxMinutes", Math.max(1, (int) Math.round(distance * 4)));
        return List.of(new VedicWarningDTO("lagna_near_sign_boundary", "caution",
                chart.getLabel(), args));
    }
}
