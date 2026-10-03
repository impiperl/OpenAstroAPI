package astroapi.vedic.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Anfrage für {@code POST /api/vedic/marriage-compatibility}.
 *
 * <p>{@code ayanamsa}, {@code nodeType} und {@code traditionalRole} sind optional; fehlen sie,
 * gelten Lahiri, der mittlere Knoten und „beide Richtungen". Alle drei stehen in der Antwort
 * ausdrücklich wieder drin, damit ein gespeichertes Ergebnis selbsterklärend bleibt.
 */
public class VedicMarriageRequest {

    @Valid
    @NotNull(message = "personA must not be null")
    private VedicPersonRequest personA;

    @Valid
    @NotNull(message = "personB must not be null")
    private VedicPersonRequest personB;

    /** Schlüssel einer {@code Ayanamsa} — Vorgabe {@code lahiri}. */
    private String ayanamsa;

    /** Schlüssel eines {@code NodeType} — Vorgabe {@code mean}. */
    private String nodeType;

    /** Schlüssel einer {@code TraditionalRole} — Vorgabe {@code both}. */
    private String traditionalRole;

    public VedicPersonRequest getPersonA() { return personA; }
    public void setPersonA(VedicPersonRequest personA) { this.personA = personA; }

    public VedicPersonRequest getPersonB() { return personB; }
    public void setPersonB(VedicPersonRequest personB) { this.personB = personB; }

    public String getAyanamsa() { return ayanamsa; }
    public void setAyanamsa(String ayanamsa) { this.ayanamsa = ayanamsa; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getTraditionalRole() { return traditionalRole; }
    public void setTraditionalRole(String traditionalRole) { this.traditionalRole = traditionalRole; }
}
