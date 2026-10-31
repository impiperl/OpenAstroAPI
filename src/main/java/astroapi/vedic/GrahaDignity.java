package astroapi.vedic;

import java.util.EnumMap;
import java.util.Map;

/**
 * Die Würde (Avastha nach Zeichen) der sieben klassischen Grahas — Grundlage des Kernthemas
 * „stärkster Graha" der Kundli.
 *
 * <p>Quelle: Brihat Parashara Hora Shastra, Kap. 3, in der Fassung der Saravali-Referenz
 * „Dignities of Planets" (CC BY-SA 4.0) — dieselbe Referenz wie {@link GrahaFriendship}.
 *
 * <h2>Die Stufen, von oben nach unten</h2>
 * Erhöhung · Moolatrikona · eigenes Zeichen · Zeichen eines Freundes · neutral · Zeichen eines
 * Feindes · Fall. Freund und Feind meinen die <b>natürliche</b> Freundschaft
 * ({@link GrahaFriendship}) zum Herrscher des Zeichens. Die zeitweilige Freundschaft und die daraus
 * zusammengesetzte fünfstufige Beziehung gehen nicht ein: Sie wären ein zweites Regelwerk, und die
 * Kennung {@link VedicRuleSet#DIGNITY_RULE_SET} müsste das sagen.
 *
 * <h2>Grade, wo die Quelle sie nennt</h2>
 * Zwei Zeichen tragen zwei Würden zugleich, und BPHS teilt sie nach Grad: Der Mond ist im Stier
 * bis 3° erhöht und danach in seinem Moolatrikona; Merkur in der Jungfrau bis 15° erhöht, bis 20°
 * Moolatrikona und danach im eigenen Zeichen. Ebenso enden die Moolatrikona-Bereiche von Sonne,
 * Mars, Jupiter, Venus und Saturn innerhalb des Zeichens; der Rest ist eigenes Zeichen.
 *
 * <p>Im Navamsa (D9) gibt es keine Grade, die eine solche Teilung tragen könnten — dort zählt nur
 * das Zeichen ({@link #ofSign}), und Moolatrikona fällt mit dem eigenen Zeichen zusammen.
 *
 * <h2>Rahu und Ketu</h2>
 * Für die Knoten nennen die Schulen verschiedene Erhöhungszeichen (Stier oder Zwillinge für Rahu,
 * Skorpion oder Schütze für Ketu). Statt eine davon still zu wählen, bewertet diese Klasse die
 * Knoten nicht.
 */
public final class GrahaDignity {

    private GrahaDignity() {}

    /** Die sieben Stufen; {@link #rank()} ordnet sie, höher ist würdiger. */
    public enum Level {
        EXALTED("exalted", 7),
        MOOLATRIKONA("moolatrikona", 6),
        OWN_SIGN("own_sign", 5),
        FRIEND_SIGN("friend_sign", 4),
        NEUTRAL_SIGN("neutral_sign", 3),
        ENEMY_SIGN("enemy_sign", 2),
        DEBILITATED("debilitated", 1);

        private final String key;
        private final int rank;

        Level(String key, int rank) {
            this.key = key;
            this.rank = rank;
        }

        /** Stabiler technischer Schlüssel ({@code exalted} …) — Basis der Übersetzungsschlüssel. */
        public String getKey() { return key; }

        public int rank() { return rank; }
    }

    /** Erhöhungszeichen; der Fall liegt im gegenüberliegenden Zeichen. */
    private static final Map<VedicBody, Rashi> EXALTATION = new EnumMap<>(VedicBody.class);

    /** Moolatrikona: Zeichen und Gradbereich [von, bis). */
    private static final Map<VedicBody, Rashi> MOOLATRIKONA_SIGN = new EnumMap<>(VedicBody.class);
    private static final Map<VedicBody, double[]> MOOLATRIKONA_RANGE = new EnumMap<>(VedicBody.class);

    static {
        EXALTATION.put(VedicBody.SUN, Rashi.MESHA);
        EXALTATION.put(VedicBody.MOON, Rashi.VRISHABHA);
        EXALTATION.put(VedicBody.MARS, Rashi.MAKARA);
        EXALTATION.put(VedicBody.MERCURY, Rashi.KANYA);
        EXALTATION.put(VedicBody.JUPITER, Rashi.KARKA);
        EXALTATION.put(VedicBody.VENUS, Rashi.MEENA);
        EXALTATION.put(VedicBody.SATURN, Rashi.TULA);

        moolatrikona(VedicBody.SUN, Rashi.SIMHA, 0, 20);
        moolatrikona(VedicBody.MOON, Rashi.VRISHABHA, 3, 30);
        moolatrikona(VedicBody.MARS, Rashi.MESHA, 0, 12);
        moolatrikona(VedicBody.MERCURY, Rashi.KANYA, 15, 20);
        moolatrikona(VedicBody.JUPITER, Rashi.DHANU, 0, 10);
        moolatrikona(VedicBody.VENUS, Rashi.TULA, 0, 15);
        moolatrikona(VedicBody.SATURN, Rashi.KUMBHA, 0, 20);
    }

    private static void moolatrikona(VedicBody body, Rashi sign, double from, double to) {
        MOOLATRIKONA_SIGN.put(body, sign);
        MOOLATRIKONA_RANGE.put(body, new double[]{from, to});
    }

    /** Erhöhungszeichen eines klassischen Grahas. */
    public static Rashi exaltationSign(VedicBody body) {
        return requireClassical(body, EXALTATION.get(body));
    }

    /** Fallzeichen — gegenüber der Erhöhung. */
    public static Rashi debilitationSign(VedicBody body) {
        return Rashi.byIndex(exaltationSign(body).getIndex() + 6);
    }

    /**
     * Würde im Rashi-Chart (D1), mit den Gradgrenzen aus BPHS.
     *
     * @param degreeInSign 0 (einschließlich) bis 30 (ausschließlich)
     */
    public static Level ofPosition(VedicBody body, Rashi sign, double degreeInSign) {
        Rashi exaltation = exaltationSign(body);
        Rashi mtSign = MOOLATRIKONA_SIGN.get(body);
        double[] mt = MOOLATRIKONA_RANGE.get(body);
        boolean inMoolatrikona = sign == mtSign && degreeInSign >= mt[0] && degreeInSign < mt[1];

        // Wo Erhöhung und Moolatrikona dasselbe Zeichen teilen (Mond, Merkur), entscheidet der
        // Grad; sonst ist das ganze Erhöhungszeichen Erhöhung.
        if (sign == exaltation && !inMoolatrikona) {
            if (sign != mtSign || degreeInSign < mt[0]) return Level.EXALTED;
        }
        if (inMoolatrikona) return Level.MOOLATRIKONA;
        if (sign == debilitationSign(body)) return Level.DEBILITATED;
        return bySignLord(body, sign);
    }

    /**
     * Würde nur nach Zeichen — für das Navamsa (D9), das keine verwertbaren Grade hat.
     * Moolatrikona zählt dort als eigenes Zeichen.
     */
    public static Level ofSign(VedicBody body, Rashi sign) {
        if (sign == exaltationSign(body)) return Level.EXALTED;
        if (sign == debilitationSign(body)) return Level.DEBILITATED;
        return bySignLord(body, sign);
    }

    private static Level bySignLord(VedicBody body, Rashi sign) {
        VedicBody lord = sign.getLord();
        if (lord == body) return Level.OWN_SIGN;
        switch (GrahaFriendship.relation(body, lord)) {
            case FRIEND: return Level.FRIEND_SIGN;
            case ENEMY: return Level.ENEMY_SIGN;
            default: return Level.NEUTRAL_SIGN;
        }
    }

    private static <T> T requireClassical(VedicBody body, T value) {
        if (value == null) {
            throw new IllegalArgumentException("No dignity defined for " + body
                    + " — only the seven classical grahas are assessed.");
        }
        return value;
    }
}
