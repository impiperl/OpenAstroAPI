package astroapi.vedic.panchanga;

/** Die beiden Mondhälften: zunehmend (Shukla) und abnehmend (Krishna). */
public enum Paksha {
    SHUKLA("shukla", "Shukla Paksha"),
    KRISHNA("krishna", "Krishna Paksha");

    private final String key;
    private final String displayName;

    Paksha(String key, String displayName) { this.key = key; this.displayName = displayName; }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
}
