package astroapi.vedic;

/**
 * Die vier Varnas des Varna-Koota, mit ihrer Rangfolge.
 *
 * <p>Zuordnung über das Element des Mondzeichens: Wasser → Brahmin, Feuer → Kshatriya,
 * Luft → Vaishya, Erde → Shudra. Der {@link #getRank() Rang} ist die einzige Größe, die in die
 * Wertung eingeht — die Regel lautet „Varna des Bräutigams gleich oder höher als die der Braut".
 *
 * <p>Das ist eine historische Regel eines traditionellen Systems. Sie wird hier berechnet und
 * erklärt, nicht als Bewertung von Menschen übernommen; die Oberflächen formulieren sie
 * entsprechend und die Deutung darf daraus keine Eignung ableiten.
 */
public enum VarnaClass {

    BRAHMIN("brahmin", 4),
    KSHATRIYA("kshatriya", 3),
    VAISHYA("vaishya", 2),
    SHUDRA("shudra", 1);

    private final String key;
    private final int rank;

    VarnaClass(String key, int rank) {
        this.key = key;
        this.rank = rank;
    }

    public String getKey() {
        return key;
    }

    /** 4 = Brahmin … 1 = Shudra. Nur die Ordnung zählt, nicht der Betrag. */
    public int getRank() {
        return rank;
    }
}
