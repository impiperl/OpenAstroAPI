package astroapi.vedic;

/**
 * Die drei Nadis des Nadi-Koota.
 *
 * <p>Koota-Namen Adi/Madhya/Antya, ayurvedisch Vata/Pitta/Kapha. Beide Benennungen stehen hier,
 * weil die Oberflächen die ayurvedische verwenden und die Regelliteratur die erste.
 */
public enum Nadi {
    ADI("adi", "vata"),
    MADHYA("madhya", "pitta"),
    ANTYA("antya", "kapha");

    private final String key;
    private final String ayurvedicKey;

    Nadi(String key, String ayurvedicKey) {
        this.key = key;
        this.ayurvedicKey = ayurvedicKey;
    }

    public String getKey() { return key; }

    public String getAyurvedicKey() { return ayurvedicKey; }
}
