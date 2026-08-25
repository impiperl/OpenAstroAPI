package astroapi.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Antwort von {@code POST /api/events/range}: die erkannten Ereignisse, nach Startzeit sortiert. */
public class EventRangeResponse {

    private OffsetDateTime from;
    private OffsetDateTime to;
    private OffsetDateTime calculatedAt;

    /** true, wenn ein Radix mitgeschickt wurde und persönliche Transite enthalten sind. */
    private boolean personal;

    /**
     * true, wenn mindestens ein Detektor fehlgeschlagen ist und die Liste damit unvollständig ist.
     *
     * <p>Ohne dieses Feld wäre „es gab keine Finsternis" nicht von „der Finsternis-Detektor ist
     * abgestürzt" zu unterscheiden. Für die interaktive Anzeige ist eine unvollständige Liste
     * brauchbar; ein Vorausberechnungslauf, der das Ergebnis <b>speichert</b>, darf einen solchen
     * Tag dagegen nicht als berechnet markieren — sonst ist die Lücke dauerhaft.
     */
    private boolean partial;

    /** Namen der fehlgeschlagenen Detektoren, leer im Normalfall. */
    private List<String> failedDetectors = new ArrayList<>();

    private List<AstroEventRawDTO> events = new ArrayList<>();

    public OffsetDateTime getFrom() { return from; }
    public void setFrom(OffsetDateTime from) { this.from = from; }
    public OffsetDateTime getTo() { return to; }
    public void setTo(OffsetDateTime to) { this.to = to; }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public boolean isPersonal() { return personal; }
    public void setPersonal(boolean personal) { this.personal = personal; }
    public boolean isPartial() { return partial; }
    public void setPartial(boolean partial) { this.partial = partial; }
    public List<String> getFailedDetectors() { return failedDetectors; }
    public void setFailedDetectors(List<String> failedDetectors) { this.failedDetectors = failedDetectors; }
    public List<AstroEventRawDTO> getEvents() { return events; }
    public void setEvents(List<AstroEventRawDTO> events) { this.events = events; }
}
