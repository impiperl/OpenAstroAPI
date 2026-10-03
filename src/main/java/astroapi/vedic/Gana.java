package astroapi.vedic;

/** Die drei Ganas des Gana-Koota (Temperament). */
public enum Gana {
    DEVA("deva"),
    MANUSHYA("manushya"),
    RAKSHASA("rakshasa");

    private final String key;

    Gana(String key) { this.key = key; }

    public String getKey() { return key; }
}
