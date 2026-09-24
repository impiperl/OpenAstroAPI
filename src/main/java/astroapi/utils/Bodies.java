package astroapi.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Welche Körper in eine Berechnung eingehen — und unter welchen Bedingungen die erweiterten
 * mitreden dürfen.
 *
 * <p>{@link Planet} zählt auf, was die Ephemeridendateien hergeben; diese Klasse entscheidet, was
 * davon benutzt wird. Der Unterschied ist wichtig, weil die Asteroiden nachträglich dazugekommen
 * sind: jede bestehende Radix-, Synastrie- und Tagesantwort wird durch sie breiter, und jede
 * Aspektliste länger. Ohne Regeln wäre das kein Gewinn an Aussage, sondern Rauschen — sechs
 * zusätzliche Körper erzeugen mit den allgemeinen Orben von bis zu 8° in jedem Horoskop ein
 * Dutzend zusätzlicher Aspekte, von denen kaum einer etwas trägt.
 *
 * <p>Chiron zählt bewusst zum <b>klassischen</b> Satz: er war von Anfang an dabei, steckt in allen
 * Texten, Deutungen und gespeicherten Charts. Ihn nachträglich unter die Filter zu stellen würde
 * bestehende Ergebnisse verändern.
 */
public final class Bodies {

    private Bodies() {}

    /** Der Satz, wie er vor der Asteroiden-Erweiterung galt. Ergebnisse damit bleiben unverändert. */
    public static final List<Planet> CLASSIC = List.of(
            Planet.SUN, Planet.MOON, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER,
            Planet.SATURN, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO,
            Planet.TRUE_NODE, Planet.MEAN_NODE, Planet.CHIRON);

    /** Die nachträglich aktivierten Körper — alle aus bereits vorhandenen Ephemeridendateien. */
    public static final List<Planet> EXTENDED = List.of(
            Planet.CERES, Planet.PALLAS, Planet.JUNO, Planet.VESTA,
            Planet.PHOLUS, Planet.MEAN_LILITH);

    private static final Set<Planet> EXTENDED_SET = EnumSet.copyOf(EXTENDED);

    private static final Set<String> EXTENDED_NAMES = EXTENDED.stream()
            .map(p -> p.getName().toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    /**
     * Punkte, zu denen ein erweiterter Körper überhaupt einen Aspekt bilden darf.
     *
     * <p>Die persönlichen Faktoren plus die beiden Achsen. Ein Asteroid im Quadrat zu Neptun sind
     * zwei langsame Körper: dieser Aspekt steht über Monate und sagt über einen einzelnen Tag
     * nichts aus. Erlebbar wird ein Asteroid erst dort, wo er einen schnellen, persönlichen Punkt
     * berührt.
     */
    public static final Set<String> PERSONAL_POINTS = Set.of(
            "sun", "moon", "mercury", "venus", "mars", "ascendant", "medium coeli", "medium_coeli");

    /** Aspekte, die ein erweiterter Körper bilden darf — die fünf Hauptaspekte. */
    private static final Set<String> MAJOR_ASPECTS = Set.of(
            "conjunction", "opposition", "square", "trine", "sextile");

    /** Allgemeiner Orb für einen Aspekt mit erweitertem Körper. */
    public static final double EXTENDED_ORB = 1.0;

    /** Die Konjunktion darf etwas weiter gefasst sein — sie ist die einzige Verschmelzung. */
    public static final double EXTENDED_CONJUNCTION_ORB = 1.5;

    /**
     * Die Körper einer Chartberechnung.
     *
     * @param extendedEnabled Schalter {@code astro.bodies.extended}
     */
    public static List<Planet> forChart(boolean extendedEnabled) {
        if (!extendedEnabled) return CLASSIC;
        LinkedHashSet<Planet> all = new LinkedHashSet<>(CLASSIC);
        all.addAll(EXTENDED);
        return List.copyOf(all);
    }

    public static boolean isExtended(Planet planet) {
        return planet != null && EXTENDED_SET.contains(planet);
    }

    /** Erkennt einen erweiterten Körper an seinem Namen, wie er in DTOs und Payloads steht. */
    public static boolean isExtended(String bodyName) {
        return bodyName != null && EXTENDED_NAMES.contains(bodyName.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isPersonalPoint(String bodyName) {
        return bodyName != null && PERSONAL_POINTS.contains(bodyName.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Darf dieser Aspekt in eine Antwort?
     *
     * <p>Für zwei klassische Körper ändert sich nichts — die Regeln greifen ausschließlich dort,
     * wo ein erweiterter Körper beteiligt ist:
     *
     * <ol>
     *   <li>Asteroid gegen Asteroid: nein. Zwei Rauschquellen ergeben keine Aussage.</li>
     *   <li>Der Gegenpart muss ein persönlicher Punkt sein.</li>
     *   <li>Nur Hauptaspekte — Quincunx, Quintil und Novil sind schon unter Planeten Feinarbeit.</li>
     *   <li>Enger Orb: {@value #EXTENDED_ORB}°, bei der Konjunktion
     *       {@value #EXTENDED_CONJUNCTION_ORB}°.</li>
     * </ol>
     *
     * @param aspectName Name des Aspekts, wie in {@code AspectType.getDescription()} bzw.
     *                   {@code MajorAspect.getDisplayName()}
     * @param orb        Abweichung vom exakten Winkel in Grad
     */
    public static boolean aspectAllowed(String bodyOne, String bodyTwo, String aspectName, double orb) {
        boolean oneExtended = isExtended(bodyOne);
        boolean twoExtended = isExtended(bodyTwo);
        if (!oneExtended && !twoExtended) return true;
        if (oneExtended && twoExtended) return false;

        String counterpart = oneExtended ? bodyTwo : bodyOne;
        if (!isPersonalPoint(counterpart)) return false;

        String aspect = aspectName == null ? "" : aspectName.trim().toLowerCase(Locale.ROOT);
        if (!MAJOR_ASPECTS.contains(aspect)) return false;

        return orb <= maxOrbFor(aspect);
    }

    /** Orbgrenze für einen Aspekt mit erweitertem Körper. */
    public static double maxOrbFor(String aspectName) {
        return "conjunction".equalsIgnoreCase(aspectName == null ? "" : aspectName.trim())
                ? EXTENDED_CONJUNCTION_ORB : EXTENDED_ORB;
    }

    /**
     * Rasterweite für den Ereignis-Scan, in Tagen.
     *
     * <p>Ceres, Pallas, Juno und Vesta brauchen 3,6 bis 4,6 Jahre für einen Umlauf, Pholus 92
     * Jahre, das mittlere Apogäum knapp 9. Sie bewegen sich also langsamer als Jupiter — das
     * Halbtagesraster der Planeten ist für sie um ein Vielfaches feiner als nötig und kostet nur
     * Ephemeridenaufrufe. Zwei Tage halten den Raster immer noch weit unter jedem Aspektdurchgang.
     */
    public static double scanStepDays(Planet planet, double defaultStepDays) {
        return isExtended(planet) ? 2.0 : defaultStepDays;
    }

    /** Alle Namen der erweiterten Körper — für Tests und Diagnose. */
    public static List<String> extendedNames() {
        return Collections.unmodifiableList(
                Arrays.stream(EXTENDED.toArray(new Planet[0])).map(Planet::getName).collect(Collectors.toList()));
    }
}
