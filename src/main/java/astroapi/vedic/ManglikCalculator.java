package astroapi.vedic;

import astroapi.vedic.dto.ManglikReferenceDTO;
import astroapi.vedic.dto.ManglikResultDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicPlanetDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Manglik- / Kuja-Dosha, Regelwerk {@link VedicRuleSet#MANGLIK_RULE_SET}.
 *
 * <h2>Was geprüft wird</h2>
 * Mars in einem der Häuser {@code 1, 2, 4, 7, 8, 12}, in Whole-Sign-Zählung, von drei
 * Bezugspunkten aus: <b>Lagna</b> (siderischer Aszendent), <b>Mond</b> und <b>Venus</b>. Alle drei
 * werden einzeln gemeldet — welcher Bezugspunkt anspricht, ist für die Erklärung wichtiger als die
 * Tatsache, dass überhaupt einer anspricht.
 *
 * <h2>Aufhebungsregeln (Parihara)</h2>
 * Umgesetzt sind nur Regeln, die deterministisch aus dem Chart folgen und in der Literatur
 * übereinstimmend genannt werden:
 *
 * <ul>
 *   <li>{@code mars_own_sign} — Mars in Widder oder Skorpion</li>
 *   <li>{@code mars_exalted} — Mars in Steinbock</li>
 *   <li>{@code mars_first_house_sign} — Mars im 1. Haus in Widder, Löwe oder Wassermann</li>
 *   <li>{@code mars_second_house_sign} — Mars im 2. Haus in Zwillinge oder Jungfrau</li>
 *   <li>{@code mars_twelfth_house_sign} — Mars im 12. Haus in Stier oder Waage</li>
 *   <li>{@code jupiter_aspects_mars} — Jupiter sieht Mars nach vedischer Graha-Drishti
 *       (Konjunktion oder 5., 7., 9. Zeichen von Jupiter aus gezählt)</li>
 *   <li>{@code venus_conjunct_mars} — Venus im selben Zeichen wie Mars</li>
 * </ul>
 *
 * <p>Die Graha-Drishti ist hier <b>zeichenweise</b> gerechnet, nicht gradgenau — das ist die
 * klassische vedische Form des Aspekts und nicht die westliche Orbis-Rechnung. Das steht
 * ausdrücklich in der Regelkennung, damit niemand einen Orbis hineinliest.
 *
 * <p>Die paarbezogene Aufhebung „beide Manglik" (Dosha Samyam) gehört nicht hierher, sondern auf
 * die Paarebene — sie braucht beide Charts. Sie steht in der Antwort als eigenes Feld.
 *
 * <p><b>Nicht umgesetzt</b> und in der Dokumentation als offen benannt: die Yogakaraka-Regel für
 * bestimmte Aszendenten, die Altersregel („ab 28") und pauschale „sonstige starke Yogas". Die
 * ersten beiden sind keine Chart-Eigenschaft im Sinn dieser Berechnung, die dritte ist keine Regel,
 * sondern ein Ermessen — und erfundene Aufhebungen sind schlimmer als fehlende.
 *
 * <h2>Schweregrad</h2>
 * {@code severity} fasst die Einzelbefunde dieses Regelwerks zusammen: {@code strong}, wenn das
 * 7. oder 8. Haus vom Lagna betroffen ist, {@code moderate} bei einem anderen betroffenen Haus vom
 * Lagna, {@code mild}, wenn nur Mond oder Venus ansprechen, {@code none} sonst oder nach
 * Aufhebung. Das ist eine <b>Zusammenfassung dieser Umsetzung</b>, keine klassische Größe; die
 * Dokumentation benennt sie als solche, und die Oberflächen formulieren sie entsprechend
 * zurückhaltend.
 *
 * <h2>Ohne Geburtszeit</h2>
 * Fehlt die Geburtszeit, entfällt der <b>Lagna-Bezug</b> ganz: {@code lagnaAvailable} ist
 * {@code false}, der Bezugspunkt steht nicht in der Liste, die drei hausgebundenen Aufhebungen
 * greifen nicht, und der Schweregrad kann höchstens {@code mild} werden. Mond und Venus bleiben —
 * sie hängen an Planetenstellungen und nicht an der Achse.
 *
 * <h2>Was hier nicht entsteht</h2>
 * Keine Aussage über Gelingen oder Scheitern einer Ehe, keine Prognose, kein Urteil über Menschen.
 * Die Klasse liefert Stellungen und Regelkennungen.
 */
public final class ManglikCalculator {

    private ManglikCalculator() {}

    /** Die geprüften Häuser dieses Regelwerks. */
    public static final List<Integer> CHECKED_HOUSES = List.of(1, 2, 4, 7, 8, 12);

    /** Alle Aufhebungsregeln, die dieses Regelwerk kennt — auch die, die gerade nicht greifen. */
    public static final List<String> KNOWN_CANCELLATIONS = List.of(
            "mars_own_sign",
            "mars_exalted",
            "mars_first_house_sign",
            "mars_second_house_sign",
            "mars_twelfth_house_sign",
            "jupiter_aspects_mars",
            "venus_conjunct_mars");

    public static ManglikResultDTO calculate(VedicChartDTO chart) {
        VedicPlanetDTO mars = planet(chart, VedicBody.MARS);
        VedicPlanetDTO venus = planet(chart, VedicBody.VENUS);
        VedicPlanetDTO jupiter = planet(chart, VedicBody.JUPITER);
        VedicPlanetDTO moon = planet(chart, VedicBody.MOON);

        Rashi marsRashi = Rashi.byIndex(mars.getRashiIndex());
        Rashi moonRashi = Rashi.byIndex(moon.getRashiIndex());
        Rashi venusRashi = Rashi.byIndex(venus.getRashiIndex());

        // Ohne Geburtszeit gibt es kein Lagna — und damit keinen Lagna-Bezug. Der Bezugspunkt
        // entfällt dann ganz, statt auf Mitternacht gerechnet zu werden. Mond und Venus bleiben:
        // sie hängen an Planetenstellungen, nicht an der Achse.
        boolean lagnaAvailable = chart.isTimeDependentAvailable()
                && chart.getAscendantRashiIndex() != null;
        Rashi lagnaRashi = lagnaAvailable ? Rashi.byIndex(chart.getAscendantRashiIndex()) : null;

        List<ManglikReferenceDTO> references = new ArrayList<>(3);
        ManglikReferenceDTO fromLagna = lagnaAvailable ? reference("lagna", lagnaRashi, marsRashi) : null;
        if (fromLagna != null) references.add(fromLagna);
        references.add(reference("moon", moonRashi, marsRashi));
        references.add(reference("venus", venusRashi, marsRashi));

        boolean manglik = references.stream().anyMatch(ManglikReferenceDTO::isAfflicted);

        List<String> cancellations = cancellations(marsRashi,
                fromLagna == null ? null : fromLagna.getMarsHouse(), jupiter, venus, mars);
        boolean cancelled = !cancellations.isEmpty();

        ManglikResultDTO dto = new ManglikResultDTO();
        dto.setLabel(chart.getLabel());
        dto.setLagnaAvailable(lagnaAvailable);
        dto.setManglik(manglik);
        dto.setManglikAfterCancellation(manglik && !cancelled);
        dto.setSeverity(severity(manglik && !cancelled, fromLagna, references));
        dto.setMarsRashi(marsRashi.getKey());
        dto.setMarsSiderealLongitude(mars.getSiderealLongitude());
        dto.setMarsDegreeInSignFormatted(mars.getDegreeInSignFormatted());
        dto.setMarsNavamsaRashi(mars.getNavamsaRashi());
        dto.setMarsNakshatra(mars.getNakshatra());
        dto.setCheckedHouses(CHECKED_HOUSES);
        dto.setReferences(references);
        dto.setCancellations(cancellations);
        dto.setCancellationsChecked(KNOWN_CANCELLATIONS);
        return dto;
    }

    /**
     * Paarbezogene Aufhebung: tragen beide Menschen einen Befund, gilt er nach der verbreiteten
     * Regel „Dosha Samyam" als ausgeglichen. Geprüft wird der Befund <b>vor</b> den
     * personenbezogenen Aufhebungen — sonst würde eine schon aufgehobene Stellung die Aufhebung
     * der anderen Seite begründen.
     */
    public static boolean mutualCancellation(ManglikResultDTO a, ManglikResultDTO b) {
        return a.isManglik() && b.isManglik();
    }

    // ── Regeln ───────────────────────────────────────────────────────────────

    private static ManglikReferenceDTO reference(String name, Rashi from, Rashi marsRashi) {
        int house = Rashi.countFrom(from, marsRashi);
        ManglikReferenceDTO dto = new ManglikReferenceDTO();
        dto.setReference(name);
        dto.setReferenceRashi(from.getKey());
        dto.setMarsHouse(house);
        dto.setAfflicted(CHECKED_HOUSES.contains(house));
        return dto;
    }

    /**
     * @param marsHouseFromLagna Haus des Mars vom Lagna, oder {@code null} ohne Geburtszeit. Die
     *                           drei hausgebundenen Aufhebungen greifen dann nicht — sie brauchen
     *                           das Haus, und ein geratenes Haus würde einen Befund aufheben, der
     *                           vielleicht gar nicht dort steht.
     */
    private static List<String> cancellations(Rashi marsRashi, Integer marsHouseFromLagna,
                                              VedicPlanetDTO jupiter, VedicPlanetDTO venus,
                                              VedicPlanetDTO mars) {
        List<String> found = new ArrayList<>();

        if (marsRashi == Rashi.MESHA || marsRashi == Rashi.VRISHCHIKA) {
            found.add("mars_own_sign");
        }
        if (marsRashi == Rashi.MAKARA) {
            found.add("mars_exalted");
        }
        if (marsHouseFromLagna != null && marsHouseFromLagna == 1
                && (marsRashi == Rashi.MESHA || marsRashi == Rashi.SIMHA || marsRashi == Rashi.KUMBHA)) {
            found.add("mars_first_house_sign");
        }
        if (marsHouseFromLagna != null && marsHouseFromLagna == 2
                && (marsRashi == Rashi.MITHUNA || marsRashi == Rashi.KANYA)) {
            found.add("mars_second_house_sign");
        }
        if (marsHouseFromLagna != null && marsHouseFromLagna == 12
                && (marsRashi == Rashi.VRISHABHA || marsRashi == Rashi.TULA)) {
            found.add("mars_twelfth_house_sign");
        }
        if (jupiterSeesMars(jupiter.getRashiIndex(), mars.getRashiIndex())) {
            found.add("jupiter_aspects_mars");
        }
        if (venus.getRashiIndex() == mars.getRashiIndex()) {
            found.add("venus_conjunct_mars");
        }
        return found;
    }

    /**
     * Vedische Graha-Drishti Jupiters: jeder Graha sieht das 7. Zeichen von sich, Jupiter
     * zusätzlich das 5. und das 9. Die Konjunktion (dasselbe Zeichen) zählt mit.
     */
    private static boolean jupiterSeesMars(int jupiterRashiIndex, int marsRashiIndex) {
        int distance = Rashi.countFrom(Rashi.byIndex(jupiterRashiIndex), Rashi.byIndex(marsRashiIndex));
        return distance == 1 || distance == 5 || distance == 7 || distance == 9;
    }

    /**
     * @param fromLagna der Lagna-Befund, oder {@code null} ohne Geburtszeit. Dann kann der Grad
     *                  höchstens {@code mild} werden — die beiden stärkeren Stufen hängen am
     *                  Lagna, und den gibt es ohne Uhrzeit nicht.
     */
    private static String severity(boolean effective, ManglikReferenceDTO fromLagna,
                                   List<ManglikReferenceDTO> references) {
        if (!effective) return "none";
        if (fromLagna != null && fromLagna.isAfflicted()) {
            return (fromLagna.getMarsHouse() == 7 || fromLagna.getMarsHouse() == 8)
                    ? "strong" : "moderate";
        }
        return references.stream().anyMatch(ManglikReferenceDTO::isAfflicted) ? "mild" : "none";
    }

    private static VedicPlanetDTO planet(VedicChartDTO chart, VedicBody body) {
        for (VedicPlanetDTO p : chart.getPlanets()) {
            if (body.getKey().equals(p.getBody())) return p;
        }
        throw new IllegalStateException("Chart is missing " + body.getKey()
                + " — the vedic chart must always carry all nine bodies.");
    }
}
