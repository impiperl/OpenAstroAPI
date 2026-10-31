package astroapi.vedic.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Anfrage für {@code POST /api/vedic/kundli} — eine Person.
 *
 * <p>{@code ayanamsa} und {@code nodeType} sind optional; fehlen sie, gelten Lahiri und der
 * mittlere Knoten. Beide stehen in der Antwort ausdrücklich wieder drin.
 */
public class VedicKundliRequest {

    @Valid
    @NotNull(message = "person must not be null")
    private VedicPersonRequest person;

    /** Schlüssel einer {@code Ayanamsa} — Vorgabe {@code lahiri}. */
    private String ayanamsa;

    /** Schlüssel eines {@code NodeType} — Vorgabe {@code mean}. */
    private String nodeType;

    public VedicPersonRequest getPerson() { return person; }
    public void setPerson(VedicPersonRequest person) { this.person = person; }

    public String getAyanamsa() { return ayanamsa; }
    public void setAyanamsa(String ayanamsa) { this.ayanamsa = ayanamsa; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
}
