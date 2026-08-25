package astroapi.utils;

/**
 * Grobe Gattung eines erkannten Ereignisses.
 *
 * <p>Dient der Gruppierung im gAIster-Aggregator („höchstens zwei Events derselben Familie im
 * Tagesfeed"). Die fachliche Identität steckt dagegen im {@code key} des Ereignisses.
 */
public enum EventType {

    /** Neumond, erstes Viertel, Vollmond, letztes Viertel. */
    MOON_PHASE,

    /** Sonnen- und Mondfinsternisse. */
    ECLIPSE,

    /** Planet wird stationär rückläufig oder stationär direktläufig. */
    STATION,

    /** Ein Körper wechselt das Tierkreiszeichen. */
    INGRESS,

    /** Sonnenwenden und Tagundnachtgleichen. */
    SOLAR_POINT,

    /** Exakter Aspekt zwischen zwei laufenden Körpern (kollektiv). */
    ASPECT,

    /** Exakter Aspekt eines laufenden Körpers zu einem Radixpunkt (persönlich). */
    TRANSIT,

    /** Mehrgliedrige Figur: Stellium, großes Trigon, T-Quadrat, Grand Cross, Yod. */
    CONFIGURATION
}
