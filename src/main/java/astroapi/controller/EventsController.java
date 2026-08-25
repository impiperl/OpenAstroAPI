package astroapi.controller;

import astroapi.dto.EventRangeRequest;
import astroapi.dto.EventRangeResponse;
import astroapi.service.AstroEventRangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Ereigniserkennung über einen Zeitraum.
 *
 * <p>Bewusst ein eigener Controller neben {@code SEController}: die bestehenden Endpunkte liefern
 * Momentaufnahmen, hier geht es um einen Zeitraum. Vor allem aber bleiben die vorhandenen
 * Antwort-DTOs damit unangetastet — gAIster deserialisiert sie strikt, ein zusätzliches Feld in
 * einer bestehenden Antwort würde die laufende Backend-Version brechen.
 */
@RestController
@RequestMapping("/api/events")
public class EventsController {

    @Autowired
    private AstroEventRangeService astroEventRangeService;

    /**
     * Alle erkannten Ereignisse im Zeitraum.
     *
     * <p>Ohne {@code natalChart} nur kollektive Ereignisse; mit Radix zusätzlich die persönlichen
     * Transite inklusive betroffenem Radixhaus.
     */
    @PostMapping("/range")
    public EventRangeResponse getEventRange(@Valid @RequestBody EventRangeRequest request) {
        return astroEventRangeService.detect(request);
    }
}
