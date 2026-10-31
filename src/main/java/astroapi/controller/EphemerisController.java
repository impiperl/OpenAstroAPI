package astroapi.controller;

import astroapi.dto.EphemerisRangeRequest;
import astroapi.dto.EphemerisRangeResponse;
import astroapi.service.EphemerisRangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Positionen über einen Zeitraum — die Ephemeride.
 *
 * <p>Eigener Controller aus demselben Grund wie {@link EventsController}: die bestehenden
 * Antwort-DTOs bleiben unangetastet, weil gAIster sie strikt deserialisiert.
 *
 * <p>Abgrenzung: {@code /api/events/range} erkennt <i>Ereignisse</i> (exakte Durchgänge,
 * Finsternisse, Stationen), hier kommen nur <i>Positionen</i> an festen Stützstellen. Alles, was
 * sich daraus ableiten lässt — Hausdurchgänge durch ein Radix, Transitorben zu Radixpunkten —
 * rechnet der Aufrufer selbst und braucht dafür kein Geburtsdatum zu übertragen.
 */
@RestController
@RequestMapping("/api/ephemeris")
public class EphemerisController {

    @Autowired
    private EphemerisRangeService ephemerisRangeService;

    @PostMapping("/range")
    public EphemerisRangeResponse getRange(@Valid @RequestBody EphemerisRangeRequest request) {
        return ephemerisRangeService.calculate(request);
    }
}
