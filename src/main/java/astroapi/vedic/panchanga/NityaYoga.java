package astroapi.vedic.panchanga;

/**
 * Die 27 Nitya Yogas.
 *
 * <p>Gebildet aus der <b>Summe</b> der Längen von Sonne und Mond, geteilt in 27 Abschnitte zu je
 * 13°20′ — dieselbe Spanne wie ein Nakshatra, aber eine ganz andere Größe.
 *
 * <p><b>Ayanamsha-abhängig, anders als Tithi und Karana.</b> Eine Verschiebung des Nullpunkts um
 * δ verschiebt die Summe um 2δ, nicht um 0. Das Yoga muss deshalb aus <b>siderischen</b> Längen
 * gebildet werden; mit tropischen käme ein um rund 48° und damit knapp vier Abschnitte falsches
 * Ergebnis heraus. Das ist der häufigste Fehler bei selbstgebauten Panchanga-Rechnern.
 *
 * <p><b>Ohne Wertung.</b> Welche Yogas als günstig oder ungünstig gelten, hängt vom Anlass ab:
 * Für eine Hochzeit zählen je nach Schule bis zu neun, teils nur deren Visha-Ghati. Diese Klasse
 * nennt das Yoga und sonst nichts; die Bewertung gehört in ein versioniertes Regelwerk
 * ({@code muhurta.vivaha.*}), das den Anlass kennt. Eine Einstufung im neutralen Panchanga wäre
 * eine Hochzeitsregel, die auch jeden anderen Aufrufer trifft.
 */
public enum NityaYoga {

    VISHKAMBHA(1, "vishkambha", "Vishkambha"),
    PRITI(2, "priti", "Priti"),
    AYUSHMAN(3, "ayushman", "Ayushman"),
    SAUBHAGYA(4, "saubhagya", "Saubhagya"),
    SHOBHANA(5, "shobhana", "Shobhana"),
    ATIGANDA(6, "atiganda", "Atiganda"),
    SUKARMA(7, "sukarma", "Sukarma"),
    DHRITI(8, "dhriti", "Dhriti"),
    SHULA(9, "shula", "Shula"),
    GANDA(10, "ganda", "Ganda"),
    VRIDDHI(11, "vriddhi", "Vriddhi"),
    DHRUVA(12, "dhruva", "Dhruva"),
    VYAGHATA(13, "vyaghata", "Vyaghata"),
    HARSHANA(14, "harshana", "Harshana"),
    VAJRA(15, "vajra", "Vajra"),
    SIDDHI(16, "siddhi", "Siddhi"),
    VYATIPATA(17, "vyatipata", "Vyatipata"),
    VARIYANA(18, "variyana", "Variyana"),
    PARIGHA(19, "parigha", "Parigha"),
    SHIVA(20, "shiva", "Shiva"),
    SIDDHA(21, "siddha", "Siddha"),
    SADHYA(22, "sadhya", "Sadhya"),
    SHUBHA(23, "shubha", "Shubha"),
    SHUKLA(24, "shukla_yoga", "Shukla"),
    BRAHMA(25, "brahma", "Brahma"),
    INDRA(26, "indra", "Indra"),
    VAIDHRITI(27, "vaidhriti", "Vaidhriti");

    private final int number;
    private final String key;
    private final String displayName;

    NityaYoga(int number, String key, String displayName) {
        this.number = number;
        this.key = key;
        this.displayName = displayName;
    }

    /** 1…27. */
    public int getNumber() { return number; }
    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }

    /** @param absoluteIndex 0…26 */
    public static NityaYoga ofAbsoluteIndex(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex > 26) {
            throw new IllegalArgumentException("yoga index must be between 0 and 26, was " + absoluteIndex);
        }
        return values()[absoluteIndex];
    }
}
