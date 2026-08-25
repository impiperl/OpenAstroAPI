package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;

import java.util.List;

/**
 * Erkennt eine Familie von Ereignissen in einem Zeitraum.
 *
 * <p>Ein Detektor ist eine reine Funktion über der Ephemeride: derselbe Zeitraum ergibt dieselben
 * Keys und dieselben Zeiten. Keine Texte, keine Bewertung, kein GPT — das gehört alles in den
 * gAIster-Aggregator.
 */
public interface EventDetector {

    /** Kurzname für Logging und Metriken („Fehler pro Detector"). */
    String name();

    List<AstroEventRawDTO> detect(EventScanContext context);
}
