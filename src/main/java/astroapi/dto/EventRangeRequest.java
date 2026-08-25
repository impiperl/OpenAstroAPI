package astroapi.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Anfrage für {@code POST /api/events/range}.
 *
 * <p>{@code from} und {@code to} tragen einen Offset — die Umrechnung nach UT macht der Server.
 * Ein reines Lokaldatum würde bedeuten, dass die Zeitzone des Servers mitentscheidet, was zum
 * Zeitraum gehört.
 *
 * <p>{@code natalChart} ist optional: ohne Radix werden nur kollektive Ereignisse geliefert, mit
 * Radix zusätzlich die persönlichen Transite inklusive betroffenem Haus.
 */
public class EventRangeRequest {

    @NotNull
    private OffsetDateTime from;

    @NotNull
    private OffsetDateTime to;

    @Valid
    private GeoCoordinatesDTO coordinates;

    private EphiDatenDTO natalChart;

    public OffsetDateTime getFrom() { return from; }
    public void setFrom(OffsetDateTime from) { this.from = from; }
    public OffsetDateTime getTo() { return to; }
    public void setTo(OffsetDateTime to) { this.to = to; }
    public GeoCoordinatesDTO getCoordinates() { return coordinates; }
    public void setCoordinates(GeoCoordinatesDTO coordinates) { this.coordinates = coordinates; }
    public EphiDatenDTO getNatalChart() { return natalChart; }
    public void setNatalChart(EphiDatenDTO natalChart) { this.natalChart = natalChart; }
}
