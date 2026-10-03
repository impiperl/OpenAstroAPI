package astroapi.vedic;

import astroapi.vedic.dto.MarriageFactorDTO;
import astroapi.vedic.dto.MarriageFactorsDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicPlanetDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Die vertieften Ehefaktoren einer Person — als strukturierte Tatsachen, nicht als Deutung.
 *
 * <p>Geliefert werden: der siderische Aszendent, das 7. Whole-Sign-Haus, der Herrscher des
 * 7. Hauses samt seiner Stellung, Venus, Jupiter, Mars, Mond und die zugehörigen D9-Zeichen. Das
 * ist der Faktensatz, den die Deutung erklären darf — und mehr bekommt sie nicht, damit sie nichts
 * hinzurechnen kann.
 *
 * <p>Ausdrücklich <b>nicht</b> dabei: Vimshottari-Dasha und jede Terminsuche. Beides ist für die
 * zweite Ausbaustufe vorgesehen und würde hier nur unfertig mitlaufen.
 *
 * <p><b>Ohne Geburtszeit</b> fehlen Aszendent, 7. Haus und dessen Herrscher — sie hängen an der
 * Achse. Die vier Grahas bleiben mit Zeichen und Navamsa, aber ohne Hausangabe. Die Liste ist dann
 * also kürzer; dass ein Faktor fehlt, ist die ehrliche Antwort und besser als einer, der auf
 * Mitternacht gerechnet ist.
 */
public final class MarriageFactors {

    private MarriageFactors() {}

    /** Die Grahas, die über ihre reine Stellung hinaus als Ehefaktoren gelten. */
    private static final VedicBody[] FACTOR_BODIES = {
            VedicBody.VENUS, VedicBody.JUPITER, VedicBody.MARS, VedicBody.MOON };

    public static MarriageFactorsDTO build(VedicChartDTO chart) {
        List<MarriageFactorDTO> factors = new ArrayList<>();

        // Aszendent, 7. Haus und sein Herrscher hängen alle drei an der Achse — und die hängt an
        // der Minute. Ohne Geburtszeit fehlen sie deshalb ganz. Sie auf Mitternacht zu rechnen
        // hieße, drei der sieben Ehefaktoren zu erfinden, und ausgerechnet die, die in dieser
        // Tradition am schwersten wiegen.
        if (chart.isTimeDependentAvailable() && chart.getAscendantRashiIndex() != null) {
            MarriageFactorDTO ascendant = new MarriageFactorDTO();
            ascendant.setKey("ascendant");
            ascendant.setRashi(chart.getAscendantRashi());
            ascendant.setHouse(1);
            ascendant.setSiderealLongitude(chart.getAscendantLongitude());
            ascendant.setDegreeInSignFormatted(chart.getAscendantDegreeInSignFormatted());
            ascendant.setNavamsaRashi(chart.getAscendantNavamsaRashi());
            ascendant.setNakshatra(chart.getAscendantNakshatra());
            factors.add(ascendant);

            // 7. Haus — in Whole Sign ein ganzes Zeichen, deshalb ohne Grad.
            Rashi seventh = Rashi.byIndex(chart.getAscendantRashiIndex() + 6);
            MarriageFactorDTO seventhHouse = new MarriageFactorDTO();
            seventhHouse.setKey("seventh_house");
            seventhHouse.setRashi(seventh.getKey());
            seventhHouse.setHouse(7);
            seventhHouse.setNavamsaRashi(null);
            factors.add(seventhHouse);

            // Herrscher des 7. Hauses mit seiner tatsächlichen Stellung.
            VedicBody seventhLord = seventh.getLord();
            VedicPlanetDTO lordPosition = planet(chart, seventhLord);
            MarriageFactorDTO lord = new MarriageFactorDTO();
            lord.setKey("seventh_lord");
            lord.setBody(seventhLord.getKey());
            lord.setRashi(lordPosition.getRashi());
            lord.setHouse(lordPosition.getHouse());
            lord.setSiderealLongitude(lordPosition.getSiderealLongitude());
            lord.setDegreeInSignFormatted(lordPosition.getDegreeInSignFormatted());
            lord.setNavamsaRashi(lordPosition.getNavamsaRashi());
            lord.setNakshatra(lordPosition.getNakshatra());
            lord.setRetrograde(lordPosition.isRetrograde());
            factors.add(lord);
        }

        for (VedicBody body : FACTOR_BODIES) {
            VedicPlanetDTO p = planet(chart, body);
            MarriageFactorDTO factor = new MarriageFactorDTO();
            factor.setKey(body.getKey());
            factor.setBody(body.getKey());
            factor.setRashi(p.getRashi());
            factor.setHouse(p.getHouse());
            factor.setSiderealLongitude(p.getSiderealLongitude());
            factor.setDegreeInSignFormatted(p.getDegreeInSignFormatted());
            factor.setNavamsaRashi(p.getNavamsaRashi());
            factor.setNakshatra(p.getNakshatra());
            factor.setRetrograde(p.isRetrograde());
            factors.add(factor);
        }

        MarriageFactorsDTO dto = new MarriageFactorsDTO();
        dto.setLabel(chart.getLabel());
        dto.setFactors(factors);
        return dto;
    }

    private static VedicPlanetDTO planet(VedicChartDTO chart, VedicBody body) {
        for (VedicPlanetDTO p : chart.getPlanets()) {
            if (body.getKey().equals(p.getBody())) return p;
        }
        throw new IllegalStateException("Chart is missing " + body.getKey());
    }
}
