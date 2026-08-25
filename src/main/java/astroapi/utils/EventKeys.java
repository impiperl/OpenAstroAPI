package astroapi.utils;

import java.util.Locale;

/**
 * Bildung der stabilen Ereignis-Keys.
 *
 * <p>Keys sind der Vertrag zwischen AstroAPI und der Tabelle {@code astro_event_definitions} in
 * gAIster. Sie werden nie übersetzt und dürfen sich nachträglich nicht ändern — eine Umbenennung
 * würde bestehende Definitionen und Übersetzungen ins Leere laufen lassen.
 */
public final class EventKeys {

    private EventKeys() {}

    /** „True Node" → {@code true_node}, „Sun" → {@code sun}. */
    public static String slug(String name) {
        if (name == null) return "unknown";
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    public static String station(String planetName, boolean retrograde) {
        return slug(planetName) + (retrograde ? "_station_retrograde" : "_station_direct");
    }

    public static String ingress(String planetName, String signName) {
        return slug(planetName) + "_ingress_" + slug(signName);
    }

    public static String aspect(String bodyA, String aspectName, String bodyB) {
        return "aspect_" + slug(bodyA) + "_" + slug(aspectName) + "_" + slug(bodyB);
    }

    public static String transit(String transitingBody, String aspectName, String natalPoint) {
        return "transit_" + slug(transitingBody) + "_" + slug(aspectName) + "_natal_" + slug(natalPoint);
    }

    public static String configuration(String figure) {
        return "configuration_" + slug(figure);
    }
}
