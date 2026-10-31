package astroapi.vedic;

import astroapi.vedic.dto.GrahaDignityDTO;
import astroapi.vedic.dto.KaalSarpaDTO;
import astroapi.vedic.dto.StrongestGrahaDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicPlanetDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Was die Kundli über das reine Chart hinaus rechnet: Würden, den stärksten Graha und Kaal Sarpa.
 *
 * <p>Rein auf einem fertigen {@link VedicChartDTO} — keine Ephemeridenabfrage, kein Zustand. Wie
 * {@link ManglikCalculator} lässt sich jede Regel so mit synthetischen Charts gezielt prüfen.
 */
public final class KundliCalculator {

    private KundliCalculator() {}

    /**
     * Natürliche Stärke (Naisargika Bala) nach BPHS, absteigend: Sonne, Mond, Venus, Jupiter,
     * Merkur, Mars, Saturn. Sie entscheidet nur, wenn D1- und D9-Würde gleich sind — als feste,
     * nachlesbare Regel statt einer Wahl nach Gutdünken.
     */
    static final List<VedicBody> NATURAL_STRENGTH = List.of(
            VedicBody.SUN, VedicBody.MOON, VedicBody.VENUS, VedicBody.JUPITER,
            VedicBody.MERCURY, VedicBody.MARS, VedicBody.SATURN);

    /** Würde der sieben klassischen Grahas in D1 und D9, in der Reihenfolge des Charts. */
    public static List<GrahaDignityDTO> dignities(VedicChartDTO chart) {
        List<GrahaDignityDTO> out = new ArrayList<>(7);
        for (VedicBody body : VedicBody.classicalGrahas()) {
            VedicPlanetDTO p = planet(chart, body);
            Rashi rashi = Rashi.byIndex(p.getRashiIndex());
            Rashi navamsa = Rashi.byIndex(p.getNavamsaRashiIndex());
            GrahaDignityDTO d = new GrahaDignityDTO();
            d.setBody(body.getKey());
            d.setRashi(rashi.getKey());
            d.setDignity(GrahaDignity.ofPosition(body, rashi, p.getDegreeInSign()).getKey());
            d.setNavamsaRashi(navamsa.getKey());
            d.setNavamsaDignity(GrahaDignity.ofSign(body, navamsa).getKey());
            d.setVargottama(rashi == navamsa);
            out.add(d);
        }
        return out;
    }

    /**
     * Der würdigste Graha: zuerst nach D1-Würde, bei Gleichstand nach D9-Würde, dann nach der
     * natürlichen Stärke. {@code decidedBy} nennt die Stufe, die entschied.
     */
    public static StrongestGrahaDTO strongest(VedicChartDTO chart) {
        List<Ranked> ranked = new ArrayList<>(7);
        for (VedicBody body : VedicBody.classicalGrahas()) {
            VedicPlanetDTO p = planet(chart, body);
            Rashi rashi = Rashi.byIndex(p.getRashiIndex());
            ranked.add(new Ranked(body,
                    GrahaDignity.ofPosition(body, rashi, p.getDegreeInSign()),
                    GrahaDignity.ofSign(body, Rashi.byIndex(p.getNavamsaRashiIndex()))));
        }
        ranked.sort(Comparator
                .comparingInt((Ranked r) -> -r.d1.rank())
                .thenComparingInt(r -> -r.d9.rank())
                .thenComparingInt(r -> NATURAL_STRENGTH.indexOf(r.body)));

        Ranked top = ranked.get(0);
        Ranked second = ranked.get(1);
        String decidedBy = top.d1 != second.d1 ? "d1_dignity"
                : top.d9 != second.d9 ? "d9_dignity"
                : "natural_strength";

        List<String> tied = new ArrayList<>();
        for (Ranked r : ranked.subList(1, ranked.size())) {
            if (r.d1 == top.d1 && r.d9 == top.d9) tied.add(r.body.getKey());
        }

        StrongestGrahaDTO dto = new StrongestGrahaDTO();
        dto.setBody(top.body.getKey());
        dto.setDignity(top.d1.getKey());
        dto.setNavamsaDignity(top.d9.getKey());
        dto.setDecidedBy(decidedBy);
        dto.setTiedOnDignity(tied);
        return dto;
    }

    /**
     * Kaal Sarpa: alle sieben klassischen Grahas innerhalb eines Halbkreises der Knotenachse.
     *
     * <p>Gemessen wird der Abstand jedes Grahas von Rahu in Tierkreisrichtung. Liegen alle bei
     * 0–180°, stehen sie zwischen Rahu und Ketu; liegen alle bei 180–360°, zwischen Ketu und Rahu.
     * Ein Graha genau auf einem Knoten zählt zu beiden Seiten — er unterbricht die Kette nicht.
     *
     * <p>Unabhängig von der Geburtszeit: es hängt nur an Längen, nicht an der Achse. Der Mond
     * wandert am Tag allerdings bis 15°; liegt er nahe einem Knoten, kann der Befund an der
     * Uhrzeit hängen. Dafür gibt es die allgemeine Warnung zur fehlenden Geburtszeit.
     */
    public static KaalSarpaDTO kaalSarpa(VedicChartDTO chart) {
        double rahu = planet(chart, VedicBody.RAHU).getSiderealLongitude();
        boolean allRahuToKetu = true;
        boolean allKetuToRahu = true;
        for (VedicBody body : VedicBody.classicalGrahas()) {
            double d = ((planet(chart, body).getSiderealLongitude() - rahu) % 360 + 360) % 360;
            if (d > 180) allRahuToKetu = false;
            if (d < 180 && d > 0) allKetuToRahu = false;
        }
        if (allRahuToKetu) return new KaalSarpaDTO(true, "rahu_to_ketu");
        if (allKetuToRahu) return new KaalSarpaDTO(true, "ketu_to_rahu");
        return new KaalSarpaDTO(false, null);
    }

    private static VedicPlanetDTO planet(VedicChartDTO chart, VedicBody body) {
        return chart.getPlanets().stream()
                .filter(p -> body.getKey().equals(p.getBody()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Chart has no " + body.getKey()));
    }

    private static final class Ranked {
        final VedicBody body;
        final GrahaDignity.Level d1;
        final GrahaDignity.Level d9;

        Ranked(VedicBody body, GrahaDignity.Level d1, GrahaDignity.Level d9) {
            this.body = body;
            this.d1 = d1;
            this.d9 = d9;
        }
    }
}
