package astroapi.utils;


import astroapi.dto.AspectDTO;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AstroUtils {

    // Definiere relevante Transit-Planeten für Tageshoroskope
    private static final Set<String> RELEVANT_TRANSIT_PLANETS = new HashSet<>(Arrays.asList(
            "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"
    ));

    /**
     * Transit-specific orbs, keyed by the <b>transiting</b> body. Much tighter than the
     * general aspect orbs (up to 8°), which are far too wide for day/transit interpretation.
     * The mover determines how long a hit lasts, so fast movers only count near-exact while
     * slow movers get a slightly wider window. Tune these values here.
     */
    private static final Map<String, Double> TRANSIT_ORBS = Map.ofEntries(
            Map.entry("Sun", 1.0),
            Map.entry("Moon", 1.0),     // daily snapshot: kept tight; widen for a "mood window" use case
            Map.entry("Mercury", 1.0),
            Map.entry("Venus", 1.0),
            Map.entry("Mars", 1.5),
            Map.entry("Jupiter", 2.0),
            Map.entry("Saturn", 2.0),
            Map.entry("Uranus", 2.0),
            Map.entry("Neptune", 2.0),
            Map.entry("Pluto", 2.0),
            Map.entry("Chiron", 1.5),
            Map.entry("True Node", 1.5),
            Map.entry("Mean Node", 1.5)
    );
    private static final double DEFAULT_TRANSIT_ORB = 1.5;

    /** Transit orb for the given transiting body (falls back to {@value #DEFAULT_TRANSIT_ORB}°). */
    public static double transitOrbFor(String transitingPlanet) {
        return TRANSIT_ORBS.getOrDefault(transitingPlanet, DEFAULT_TRANSIT_ORB);
    }

    /**
     * Narrows transit aspects to transit-specific orbs (per transiting body). {@code calculateTransits}
     * itself keeps the general aspect orbs, so without this a transit Sun square natal-X could pass at
     * 8° — much too wide for a daily/transit reading. Modifies the list in place.
     *
     * <p><b>Real transits only</b> (natal vs. current sky). Do NOT use for synastry or composite
     * charts, where the wider natal-style orbs are intended.
     */
    public static void filterTransitAspects(List<AspectDTO> aspects) {
        if (aspects == null) return;
        aspects.removeIf(aspect -> aspect.getOrb() > transitOrbFor(aspect.getTransitPlanet()));
    }

    /**
     * Entfernt alle irrelevanten Aspekte aus der übergebenen Liste.
     * Die Liste wird direkt verändert (in-place).
     *
     * @param aspects die Liste der zu überprüfenden Aspekte
     */
    public static void filterRelevantAspects(List<AspectDTO> aspects, HoroscopeType type) {
        if (aspects == null) return;
        double maxOrb;
        switch (type) {
            case NATAL:
                maxOrb = 6.0;
                break;
            case TRANSIT:
            case PHASE:
            default:
                maxOrb = 3.0;
                break;
        }

        Iterator<AspectDTO> iterator = aspects.iterator();
        while (iterator.hasNext()) {
            AspectDTO aspect = iterator.next();
            if (aspect.getOrb() > maxOrb ||
                    !RELEVANT_TRANSIT_PLANETS.contains(aspect.getTransitPlanet())) {
                iterator.remove();
            }
        }
    }
}