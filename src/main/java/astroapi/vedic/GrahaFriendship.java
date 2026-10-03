package astroapi.vedic;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Natürliche (dauerhafte) Freundschaft der sieben klassischen Grahas — Grundlage des
 * Graha-Maitri-Koota.
 *
 * <p>Quelle: Saravali-Referenz „Dignities of Planets → Natural Planetary Friendships"
 * (CC BY-SA 4.0), die klassische Parashara-Tabelle.
 *
 * <p>Die Tabelle ist bewusst <b>asymmetrisch</b>, und das ist kein Tippfehler: der Mond hat keinen
 * Feind, Merkur zählt den Mond aber als Feind. Genau daraus entstehen die Mischfälle
 * („Freund und Feind") der Graha-Maitri-Wertung. Wer die Tabelle symmetrisch macht, verliert sie.
 *
 * <p>Die <b>zeitweilige</b> Freundschaft (nach Hausabstand) geht hier nicht ein: Graha Maitri
 * bewertet die dauerhafte Beziehung der beiden Mondzeichen-Herrscher.
 */
public final class GrahaFriendship {

    private GrahaFriendship() {}

    /** Beziehung von A zu B. */
    public enum Relation { FRIEND, NEUTRAL, ENEMY }

    private static final Map<VedicBody, Set<VedicBody>> FRIENDS = new EnumMap<>(VedicBody.class);
    private static final Map<VedicBody, Set<VedicBody>> ENEMIES = new EnumMap<>(VedicBody.class);

    static {
        FRIENDS.put(VedicBody.SUN,     EnumSet.of(VedicBody.MOON, VedicBody.JUPITER, VedicBody.MARS));
        ENEMIES.put(VedicBody.SUN,     EnumSet.of(VedicBody.VENUS, VedicBody.SATURN));

        FRIENDS.put(VedicBody.MOON,    EnumSet.of(VedicBody.SUN, VedicBody.MERCURY));
        ENEMIES.put(VedicBody.MOON,    EnumSet.noneOf(VedicBody.class));

        FRIENDS.put(VedicBody.MARS,    EnumSet.of(VedicBody.SUN, VedicBody.MOON, VedicBody.JUPITER));
        ENEMIES.put(VedicBody.MARS,    EnumSet.of(VedicBody.MERCURY));

        FRIENDS.put(VedicBody.MERCURY, EnumSet.of(VedicBody.SUN, VedicBody.VENUS));
        ENEMIES.put(VedicBody.MERCURY, EnumSet.of(VedicBody.MOON));

        FRIENDS.put(VedicBody.JUPITER, EnumSet.of(VedicBody.SUN, VedicBody.MOON, VedicBody.MARS));
        ENEMIES.put(VedicBody.JUPITER, EnumSet.of(VedicBody.MERCURY, VedicBody.VENUS));

        FRIENDS.put(VedicBody.VENUS,   EnumSet.of(VedicBody.MERCURY, VedicBody.SATURN));
        ENEMIES.put(VedicBody.VENUS,   EnumSet.of(VedicBody.SUN, VedicBody.MOON));

        FRIENDS.put(VedicBody.SATURN,  EnumSet.of(VedicBody.MERCURY, VedicBody.VENUS));
        ENEMIES.put(VedicBody.SATURN,  EnumSet.of(VedicBody.SUN, VedicBody.MOON, VedicBody.MARS));
    }

    /**
     * Wie {@code from} zu {@code to} steht. Derselbe Graha gilt als Freund — ein Zeichenherrscher
     * ist sich selbst nicht fremd, und die Regel „beide Rashis haben denselben Herrscher" soll die
     * volle Punktzahl ergeben.
     */
    public static Relation relation(VedicBody from, VedicBody to) {
        if (from == to) return Relation.FRIEND;
        Set<VedicBody> friends = FRIENDS.get(from);
        Set<VedicBody> enemies = ENEMIES.get(from);
        if (friends == null || enemies == null) {
            throw new IllegalArgumentException("No natural friendship defined for " + from
                    + " — only the seven classical grahas rule rashis.");
        }
        if (friends.contains(to)) return Relation.FRIEND;
        if (enemies.contains(to)) return Relation.ENEMY;
        return Relation.NEUTRAL;
    }
}
