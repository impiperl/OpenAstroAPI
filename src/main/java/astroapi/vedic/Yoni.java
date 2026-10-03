package astroapi.vedic;

/**
 * Die 14 Yoni-Tiere des Yoni-Koota samt Bewertungsmatrix.
 *
 * <p>Quelle der Matrix: Saravali-Referenz „Asta Koota → Yoni Koota" (CC BY-SA 4.0), die Fassung
 * wie in Maitreya umgesetzt. Sie ist <b>asymmetrisch</b> — Braut Pferd × Bräutigam Hirsch ergibt
 * 3, umgekehrt 1. Deshalb braucht das Yoni-Koota wie Varna, Vashya und Gana eine festgelegte
 * Reihenfolge der beiden Menschen; siehe {@link TraditionalRole}.
 *
 * <p>Die Nullen der Matrix sind genau die vierzehn Erzfeind-Paare (Pferd–Büffel, Elefant–Löwe,
 * Schaf–Affe, Schlange–Mungo, Hund–Hirsch, Katze–Ratte, Kuh–Tiger) — sie stehen zusätzlich als
 * {@link #getInimical()}, damit die Erklärung in der Oberfläche den Grund benennen kann und nicht
 * nur die Zahl.
 */
public enum Yoni {

    HORSE("horse", 0),
    ELEPHANT("elephant", 1),
    SHEEP("sheep", 2),
    SERPENT("serpent", 3),
    DOG("dog", 4),
    CAT("cat", 5),
    RAT("rat", 6),
    COW("cow", 7),
    BUFFALO("buffalo", 8),
    TIGER("tiger", 9),
    DEER("deer", 10),
    MONKEY("monkey", 11),
    MONGOOSE("mongoose", 12),
    LION("lion", 13);

    private final String key;
    private final int matrixIndex;

    Yoni(String key, int matrixIndex) {
        this.key = key;
        this.matrixIndex = matrixIndex;
    }

    public String getKey() { return key; }

    int getMatrixIndex() { return matrixIndex; }

    /**
     * Bewertungsmatrix, Zeile = Yoni der Braut, Spalte = Yoni des Bräutigams.
     * Spaltenreihenfolge wie die Aufzählung: Pferd, Elefant, Schaf, Schlange, Hund, Katze, Ratte,
     * Kuh, Büffel, Tiger, Hirsch, Affe, Mungo, Löwe.
     */
    private static final int[][] POINTS = {
        /* Horse    */ {4, 2, 2, 3, 2, 2, 2, 1, 0, 1, 3, 3, 2, 1},
        /* Elephant */ {2, 4, 3, 3, 2, 2, 2, 2, 3, 1, 2, 3, 2, 0},
        /* Sheep    */ {2, 3, 4, 2, 1, 2, 1, 3, 3, 1, 2, 0, 3, 1},
        /* Serpent  */ {3, 3, 2, 4, 2, 1, 1, 1, 1, 2, 2, 2, 0, 2},
        /* Dog      */ {2, 2, 1, 2, 4, 2, 1, 2, 2, 1, 0, 2, 1, 1},
        /* Cat      */ {2, 2, 2, 1, 2, 4, 0, 2, 2, 1, 3, 3, 2, 1},
        /* Rat      */ {2, 2, 1, 1, 1, 0, 4, 2, 2, 2, 2, 2, 1, 2},
        /* Cow      */ {1, 2, 3, 1, 2, 2, 2, 4, 3, 0, 3, 2, 2, 1},
        /* Buffalo  */ {0, 3, 3, 1, 2, 2, 2, 3, 4, 1, 2, 2, 2, 1},
        /* Tiger    */ {1, 1, 1, 2, 1, 1, 2, 0, 1, 4, 1, 1, 2, 1},
        /* Deer     */ {1, 2, 2, 2, 0, 3, 2, 3, 2, 1, 4, 2, 2, 1},
        /* Monkey   */ {3, 3, 0, 2, 2, 3, 2, 2, 2, 1, 2, 4, 3, 2},
        /* Mongoose */ {2, 2, 3, 0, 1, 2, 1, 2, 2, 2, 2, 3, 4, 2},
        /* Lion     */ {1, 0, 1, 2, 1, 1, 2, 1, 2, 1, 1, 2, 2, 4},
    };

    /** Das Erzfeind-Tier dieser Yoni — der Eintrag, der in der Matrix 0 ergibt. */
    public Yoni getInimical() {
        switch (this) {
            case HORSE:    return BUFFALO;
            case BUFFALO:  return HORSE;
            case ELEPHANT: return LION;
            case LION:     return ELEPHANT;
            case SHEEP:    return MONKEY;
            case MONKEY:   return SHEEP;
            case SERPENT:  return MONGOOSE;
            case MONGOOSE: return SERPENT;
            case DOG:      return DEER;
            case DEER:     return DOG;
            case CAT:      return RAT;
            case RAT:      return CAT;
            case COW:      return TIGER;
            case TIGER:    return COW;
            default: throw new IllegalStateException("Unmapped yoni: " + this);
        }
    }

    /** Punkte 0…4 für dieses Paar, in der traditionellen Reihenfolge Braut × Bräutigam. */
    public static int points(Yoni bride, Yoni groom) {
        return POINTS[bride.matrixIndex][groom.matrixIndex];
    }
}
