package astroapi.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Antwort auf {@code POST /api/ephemeris/range}.
 *
 * <p>{@code partial} meldet, dass einzelne Stützstellen nicht gerechnet werden konnten (Lücke in
 * den Ephemeridendateien, Körper außerhalb seines Gültigkeitsbereichs). Der Aufrufer darf ein
 * solches Ergebnis nicht als vollständiges Jahr ablegen — sonst steht die Lücke dauerhaft in der
 * Datenbank, ohne dass sie jemand noch erkennt.
 */
public class EphemerisRangeResponse {

    private OffsetDateTime from;
    private OffsetDateTime to;
    private int stepHours;
    private OffsetDateTime calculatedAt;
    private boolean partial;
    private int missingSamples;
    private List<EphemerisSampleDTO> samples = new ArrayList<>();

    public OffsetDateTime getFrom() { return from; }
    public void setFrom(OffsetDateTime from) { this.from = from; }
    public OffsetDateTime getTo() { return to; }
    public void setTo(OffsetDateTime to) { this.to = to; }
    public int getStepHours() { return stepHours; }
    public void setStepHours(int stepHours) { this.stepHours = stepHours; }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public boolean isPartial() { return partial; }
    public void setPartial(boolean partial) { this.partial = partial; }
    public int getMissingSamples() { return missingSamples; }
    public void setMissingSamples(int missingSamples) { this.missingSamples = missingSamples; }
    public List<EphemerisSampleDTO> getSamples() { return samples; }
    public void setSamples(List<EphemerisSampleDTO> samples) { this.samples = samples; }
}
