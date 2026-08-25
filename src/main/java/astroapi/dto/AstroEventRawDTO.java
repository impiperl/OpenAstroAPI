package astroapi.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein erkanntes Ereignis als reine Fachdatenzeile.
 *
 * <p>Bewusst <b>ohne</b> Texte, Bewertung oder Deutung: AstroAPI liefert nur, was rechnerisch der
 * Fall ist. Titel, Sentiment, Intensität und Ranking entstehen im gAIster-Aggregator, Formulierungen
 * erst im Interpretationspfad.
 *
 * <p>{@code key} ist stabil und wird nie übersetzt — er ist der Fremdschlüssel zu
 * {@code astro_event_definitions.event_key} auf der gAIster-Seite.
 */
public class AstroEventRawDTO {

    private String key;
    private String type;

    private OffsetDateTime startsAt;
    private OffsetDateTime peaksAt;
    private OffsetDateTime endsAt;

    private List<String> planets = new ArrayList<>();
    private String sign;
    private Integer house;
    private Double orb;

    /** true, wenn der Zeitpunkt exakt bestimmt wurde (Nullstelle), nicht nur tagesgenau. */
    private boolean exact;

    private Map<String, Object> payload = new LinkedHashMap<>();

    public AstroEventRawDTO() {}

    public AstroEventRawDTO(String key, String type) {
        this.key = key;
        this.type = type;
    }

    /** Punktereignis: Beginn, Höhepunkt und Ende fallen zusammen. */
    public static AstroEventRawDTO point(String key, String type, OffsetDateTime at) {
        AstroEventRawDTO dto = new AstroEventRawDTO(key, type);
        dto.startsAt = at;
        dto.peaksAt = at;
        dto.exact = true;
        return dto;
    }

    public AstroEventRawDTO with(String payloadKey, Object value) {
        if (value != null) this.payload.put(payloadKey, value);
        return this;
    }

    public AstroEventRawDTO withPlanets(String... names) {
        for (String n : names) if (n != null) this.planets.add(n);
        return this;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getPeaksAt() { return peaksAt; }
    public void setPeaksAt(OffsetDateTime peaksAt) { this.peaksAt = peaksAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public List<String> getPlanets() { return planets; }
    public void setPlanets(List<String> planets) { this.planets = planets; }
    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
    public Integer getHouse() { return house; }
    public void setHouse(Integer house) { this.house = house; }
    public Double getOrb() { return orb; }
    public void setOrb(Double orb) { this.orb = orb; }
    public boolean isExact() { return exact; }
    public void setExact(boolean exact) { this.exact = exact; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
