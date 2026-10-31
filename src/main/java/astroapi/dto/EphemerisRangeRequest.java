package astroapi.dto;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Anfrage für {@code POST /api/ephemeris/range}.
 *
 * <p>Bewusst getrennt von {@link EventRangeRequest}: hier wird nichts <i>erkannt</i>, sondern nur
 * abgetastet. Eine Ereigniserkennung sucht Durchgänge und kostet Scan-Schritte; eine Ephemeride
 * liefert die Länge jedes Körpers an festen Stützstellen und ist damit billig genug für ein ganzes
 * Jahr in einem Aufruf.
 *
 * <p>{@code from} und {@code to} tragen einen Offset — die Umrechnung nach UT macht der Server.
 */
public class EphemerisRangeRequest {

    @NotNull
    private OffsetDateTime from;

    @NotNull
    private OffsetDateTime to;

    /**
     * Abstand der Stützstellen in Stunden. Vorgabe 24: für Sonne bis Pluto mehr als ausreichend,
     * da zwischen zwei Stützstellen interpoliert wird. Nur der Mond (13°/Tag) braucht enger.
     */
    private Integer stepHours = 24;

    /**
     * Gewünschte Körper als Namen wie in {@code Planet} ({@code "Saturn"}). {@code null} oder leer
     * bedeutet: der volle Satz des Charts. Unbekannte Namen werden abgewiesen, nicht ignoriert —
     * ein Tippfehler würde sonst stillschweigend eine Lücke in die Jahresdaten reißen.
     */
    private List<String> bodies;

    public OffsetDateTime getFrom() { return from; }
    public void setFrom(OffsetDateTime from) { this.from = from; }
    public OffsetDateTime getTo() { return to; }
    public void setTo(OffsetDateTime to) { this.to = to; }
    public Integer getStepHours() { return stepHours; }
    public void setStepHours(Integer stepHours) { this.stepHours = stepHours; }
    public List<String> getBodies() { return bodies; }
    public void setBodies(List<String> bodies) { this.bodies = bodies; }
}
