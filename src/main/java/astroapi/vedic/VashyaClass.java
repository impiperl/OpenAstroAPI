package astroapi.vedic;

/**
 * Die fünf Vashya-Klassen des Vashya-Koota.
 *
 * <p>Die Referenztabelle (Saravali, Asta Koota → Vashya Koota) führt fünf Klassen: Quadruped,
 * Human, Jalachara und die beiden Einzelzeichen Löwe und Skorpion. Zwei Zeichen werden halbiert:
 * Steinbock (1. Hälfte Quadruped, 2. Hälfte Jalachara) — und Schütze, der in der Quelltabelle
 * <b>fehlt</b>.
 *
 * <p>Für Schütze wird die in der Literatur übliche Teilung ergänzt: 1. Hälfte Human,
 * 2. Hälfte Quadruped. Diese Ergänzung ist der Grund, weshalb die Regelversion
 * {@code ashtakoota.saravali+dhanu.v1} heißt und nicht einfach {@code saravali}: wer das Ergebnis
 * nachrechnet, muss wissen, dass genau diese eine Zuordnung nicht aus der Quelle stammt.
 */
public enum VashyaClass {

    QUADRUPED("quadruped"),
    HUMAN("human"),
    JALACHARA("jalachara"),
    LEO("leo"),
    SCORPIO("scorpio");

    private final String key;

    VashyaClass(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
