package astroapi.vedic.dto;

/** Ein Bezugspunkt der Manglik-Prüfung (Lagna, Mond oder Venus) mit dem dort gefundenen Befund. */
public class ManglikReferenceDTO {

    /** {@code lagna}, {@code moon} oder {@code venus}. */
    private String reference;
    /** Zeichen, von dem aus gezählt wird. */
    private String referenceRashi;
    /** Haus des Mars von diesem Bezugspunkt aus, 1…12. */
    private int marsHouse;
    /** Zählt dieses Haus zu den geprüften (1, 2, 4, 7, 8, 12)? */
    private boolean afflicted;

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getReferenceRashi() { return referenceRashi; }
    public void setReferenceRashi(String referenceRashi) { this.referenceRashi = referenceRashi; }
    public int getMarsHouse() { return marsHouse; }
    public void setMarsHouse(int marsHouse) { this.marsHouse = marsHouse; }
    public boolean isAfflicted() { return afflicted; }
    public void setAfflicted(boolean afflicted) { this.afflicted = afflicted; }
}
