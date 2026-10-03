package astroapi.vedic;

import astroapi.vedic.dto.AshtakootaOrientationDTO;
import astroapi.vedic.dto.KootaScoreDTO;
import astroapi.vedic.dto.VedicChartDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ashtakoota / Guna Milan — die acht Kootas, getrennt gerechnet, zusammen höchstens 36 Punkte.
 *
 * <h2>Regelwerk</h2>
 * Umgesetzt ist die in der Saravali-Referenz („Asta Koota", CC BY-SA 4.0) dokumentierte Fassung,
 * wie sie Maitreya rechnet. Jede Tabelle steht im Quelltext, nicht in einer Konfiguration: wer das
 * Ergebnis nachrechnen will, soll die Zahlen sehen, mit denen gerechnet wurde. Die eine Ergänzung
 * gegenüber der Quelle — Schütze in der Vashya-Klassifikation — ist in {@link VashyaClass}
 * benannt und schlägt sich in der Regelversion nieder.
 *
 * <h2>Keine erfundenen Ausgleichsregeln</h2>
 * Es gibt hier keine Aufhebung, keine Gewichtung und keine „Gesamtnote". Jedes Koota liefert seine
 * Punkte nach seiner Tabelle, und die Summe ist die Summe. Die in der Literatur verbreiteten
 * Aufhebungen des Bhakoot- und Nadi-Dosha (gleicher Zeichenherrscher, befreundete Herrscher) sind
 * <b>nicht</b> eingerechnet; sie werden als {@code noteKeys} <i>gemeldet</i>, wo ihre Bedingung
 * erfüllt ist, damit die Oberfläche sie erklären kann, ohne dass eine Zahl heimlich steigt.
 *
 * <h2>Richtungsabhängigkeit</h2>
 * Varna, Vashya, Yoni und Gana haben asymmetrische Tabellen. Diese Klasse rechnet immer für eine
 * festgelegte Reihenfolge Braut × Bräutigam; welche Person welche Rolle einnimmt, entscheidet
 * ausschließlich {@link TraditionalRole}.
 */
public final class AshtakootaCalculator {

    private AshtakootaCalculator() {}

    /** Reihenfolge der Kootas in der Antwort — aufsteigend nach Höchstpunktzahl, wie klassisch. */
    public static final List<String> KOOTA_ORDER = List.of(
            "varna", "vashya", "dina", "yoni", "graha_maitri", "gana", "bhakoot", "nadi");

    /** Die Kootas, deren Tabelle von der Reihenfolge der beiden Menschen abhängt. */
    public static final List<String> ROLE_DEPENDENT_KOOTAS = List.of(
            "varna", "vashya", "yoni", "gana");

    /**
     * Rechnet das Ashtakoota für eine Richtung.
     *
     * @param bride das Chart der Person in der Braut-Rolle der Tabellen
     * @param groom das Chart der Person in der Bräutigam-Rolle
     * @param role  {@link TraditionalRole#A_AS_BRIDE} oder {@link TraditionalRole#B_AS_BRIDE}
     */
    public static AshtakootaOrientationDTO calculate(VedicChartDTO bride, VedicChartDTO groom,
                                                     TraditionalRole role) {
        Rashi brideMoonRashi = Rashi.byIndex(bride.getMoonRashiIndex());
        Rashi groomMoonRashi = Rashi.byIndex(groom.getMoonRashiIndex());
        Nakshatra brideNakshatra = Nakshatra.byIndex(bride.getMoonNakshatra().getIndex());
        Nakshatra groomNakshatra = Nakshatra.byIndex(groom.getMoonNakshatra().getIndex());
        VashyaClass brideVashya = VashyaClass.valueOf(bride.getVashya().toUpperCase(java.util.Locale.ROOT));
        VashyaClass groomVashya = VashyaClass.valueOf(groom.getVashya().toUpperCase(java.util.Locale.ROOT));

        List<KootaScoreDTO> kootas = new ArrayList<>(8);
        kootas.add(varna(brideMoonRashi, groomMoonRashi));
        kootas.add(vashya(brideVashya, groomVashya));
        kootas.add(dina(brideNakshatra, groomNakshatra));
        kootas.add(yoni(brideNakshatra, groomNakshatra));
        kootas.add(grahaMaitri(brideMoonRashi, groomMoonRashi));
        kootas.add(gana(brideNakshatra, groomNakshatra));
        kootas.add(bhakoot(brideMoonRashi, groomMoonRashi));
        kootas.add(nadi(brideNakshatra, groomNakshatra, brideMoonRashi, groomMoonRashi));

        double total = kootas.stream().mapToDouble(KootaScoreDTO::getPoints).sum();

        AshtakootaOrientationDTO dto = new AshtakootaOrientationDTO();
        dto.setRole(role.getKey());
        dto.setBrideLabel(bride.getLabel());
        dto.setGroomLabel(groom.getLabel());
        dto.setKootas(kootas);
        dto.setTotalPoints(round(total));
        dto.setMaxPoints(VedicRuleSet.ASHTAKOOTA_MAX_POINTS);
        dto.setRoleDependent(ROLE_DEPENDENT_KOOTAS);
        return dto;
    }

    // ── Varna (max 1) ────────────────────────────────────────────────────────

    /**
     * Varna-Koota. Regel der Quelle: „Die Varna des Bräutigams soll gleich oder höher sein als die
     * der Braut. Günstige Kombinationen erhalten 1 Punkt, sonst 0."
     */
    private static KootaScoreDTO varna(Rashi brideMoon, Rashi groomMoon) {
        VarnaClass brideVarna = brideMoon.getVarna();
        VarnaClass groomVarna = groomMoon.getVarna();
        boolean favourable = groomVarna.getRank() >= brideVarna.getRank();

        KootaScoreDTO k = base("varna", 1);
        k.setPoints(favourable ? 1 : 0);
        k.setInputs(inputs(
                "brideVarna", brideVarna.getKey(),
                "groomVarna", groomVarna.getKey(),
                "brideMoonRashi", brideMoon.getKey(),
                "groomMoonRashi", groomMoon.getKey()));
        k.setDecision(favourable ? "varna.groom_equal_or_higher" : "varna.groom_lower");
        k.setExplanationKey("vedic.koota.varna.explain." + (favourable ? "favourable" : "unfavourable"));
        k.setExplanationArgs(inputs("brideVarna", brideVarna.getKey(), "groomVarna", groomVarna.getKey()));
        return k;
    }

    // ── Vashya (max 2) ───────────────────────────────────────────────────────

    /**
     * Bewertungsmatrix aus der Quelle, Zeile = Braut, Spalte = Bräutigam, Klassenreihenfolge
     * Quadruped, Human, Jalachara, Leo, Scorpio.
     */
    private static final double[][] VASHYA_POINTS = {
        /* Quadruped */ {2,   0,   0,   0.5, 0},
        /* Human     */ {1,   2,   1,   0.5, 1},
        /* Jalachara */ {0.5, 1,   2,   1,   1},
        /* Leo       */ {0,   0,   0,   2,   0},
        /* Scorpio   */ {1,   1,   1,   0,   2},
    };

    private static KootaScoreDTO vashya(VashyaClass bride, VashyaClass groom) {
        double points = VASHYA_POINTS[bride.ordinal()][groom.ordinal()];

        KootaScoreDTO k = base("vashya", 2);
        k.setPoints(points);
        k.setInputs(inputs("brideVashya", bride.getKey(), "groomVashya", groom.getKey()));
        k.setDecision(bride == groom ? "vashya.same_class" : "vashya.matrix");
        k.setExplanationKey("vedic.koota.vashya.explain");
        k.setExplanationArgs(inputs(
                "brideVashya", bride.getKey(), "groomVashya", groom.getKey(), "points", points));
        if (bride == VashyaClass.LEO || groom == VashyaClass.LEO
                || bride == VashyaClass.SCORPIO || groom == VashyaClass.SCORPIO) {
            k.setNoteKeys(List.of("vedic.koota.vashya.note.single_sign_class"));
        }
        return k;
    }

    // ── Dina / Tara (max 3) ──────────────────────────────────────────────────

    /** Die als ungünstig gewerteten Taras: Vipat (3), Pratyak (5), Vadha (7). */
    private static final java.util.Set<Integer> MALEFIC_TARA_REMAINDERS = java.util.Set.of(3, 5, 7);

    /**
     * Dina-Koota. Regel der Quelle: vom Bräutigam zur Braut zählen, durch 9 teilen; ist der Rest
     * 3, 5 oder 7, gibt es 0 Punkte, sonst 1,5. Dasselbe in umgekehrter Richtung. Also 0, 1,5
     * oder 3.
     *
     * <p>Beide Richtungen zählen symmetrisch zu je 1,5 Punkten — deshalb steht Dina nicht in
     * {@link #ROLE_DEPENDENT_KOOTAS}, auch wenn die Regel von „Bräutigam" und „Braut" spricht.
     *
     * <p><b>Folge dieser Regel, die man kennen sollte:</b> 0 Punkte sind hier nicht erreichbar.
     * Bei 1-basiert-inklusiver Zählung ergänzen sich die beiden Abstände immer zu 29, also zu
     * 2 modulo 9; zwei Reste aus {@code {3, 5, 7}} ergeben modulo 9 aber nie 2. Also ist höchstens
     * eine Richtung ungünstig, und Dina liefert stets 1,5 oder 3. Das ist eine Eigenschaft der
     * gewählten Tradition und keine Nachsicht der Umsetzung — Rechner anderer Schulen, die auch
     * das erste Tara (Janma) als ungünstig werten, kommen hier auf 0. Wer die Liste der
     * ungünstigen Taras ändert, ändert damit das Regelwerk und muss
     * {@link VedicRuleSet#ASHTAKOOTA_RULE_SET} erhöhen.
     */
    private static KootaScoreDTO dina(Nakshatra bride, Nakshatra groom) {
        int countGroomToBride = VedicMath.countNakshatras(groom.getIndex(), bride.getIndex());
        int countBrideToGroom = VedicMath.countNakshatras(bride.getIndex(), groom.getIndex());
        int remainderGroomToBride = countGroomToBride % 9;
        int remainderBrideToGroom = countBrideToGroom % 9;
        boolean forwardOk = !MALEFIC_TARA_REMAINDERS.contains(remainderGroomToBride);
        boolean reverseOk = !MALEFIC_TARA_REMAINDERS.contains(remainderBrideToGroom);
        double points = (forwardOk ? 1.5 : 0) + (reverseOk ? 1.5 : 0);

        KootaScoreDTO k = base("dina", 3);
        k.setPoints(points);
        k.setInputs(inputs(
                "brideNakshatra", bride.getKey(),
                "groomNakshatra", groom.getKey(),
                "countGroomToBride", countGroomToBride,
                "countBrideToGroom", countBrideToGroom,
                "remainderGroomToBride", remainderGroomToBride,
                "remainderBrideToGroom", remainderBrideToGroom));
        k.setDecision(forwardOk && reverseOk ? "dina.both_benefic"
                : (forwardOk || reverseOk) ? "dina.one_benefic" : "dina.both_malefic");
        k.setExplanationKey("vedic.koota.dina.explain");
        k.setExplanationArgs(inputs("points", points, "max", 3));
        return k;
    }

    // ── Yoni (max 4) ─────────────────────────────────────────────────────────

    private static KootaScoreDTO yoni(Nakshatra bride, Nakshatra groom) {
        Yoni brideYoni = bride.getYoni();
        Yoni groomYoni = groom.getYoni();
        int points = Yoni.points(brideYoni, groomYoni);
        boolean inimical = brideYoni.getInimical() == groomYoni;

        KootaScoreDTO k = base("yoni", 4);
        k.setPoints(points);
        k.setInputs(inputs(
                "brideYoni", brideYoni.getKey(),
                "groomYoni", groomYoni.getKey(),
                "brideYoniMale", bride.isYoniMale(),
                "groomYoniMale", groom.isYoniMale()));
        k.setDecision(brideYoni == groomYoni ? "yoni.same_yoni"
                : inimical ? "yoni.inimical_pair" : "yoni.matrix");
        k.setExplanationKey("vedic.koota.yoni.explain");
        k.setExplanationArgs(inputs(
                "brideYoni", brideYoni.getKey(), "groomYoni", groomYoni.getKey(), "points", points));
        if (inimical) k.setNoteKeys(List.of("vedic.koota.yoni.note.inimical"));
        return k;
    }

    // ── Graha Maitri (max 5) ─────────────────────────────────────────────────

    /**
     * Graha Maitri nach der Punktetabelle der Quelle: gegenseitige Freundschaft 5, Freund und
     * Neutral 4, beide neutral 3, Freund und Feind 2, Neutral und Feind 1, gegenseitige
     * Feindschaft 0. Beurteilt werden die Herrscher der beiden Mondzeichen.
     */
    private static KootaScoreDTO grahaMaitri(Rashi brideMoon, Rashi groomMoon) {
        VedicBody brideLord = brideMoon.getLord();
        VedicBody groomLord = groomMoon.getLord();
        GrahaFriendship.Relation brideToGroom = GrahaFriendship.relation(brideLord, groomLord);
        GrahaFriendship.Relation groomToBride = GrahaFriendship.relation(groomLord, brideLord);

        int friends = count(brideToGroom, groomToBride, GrahaFriendship.Relation.FRIEND);
        int enemies = count(brideToGroom, groomToBride, GrahaFriendship.Relation.ENEMY);
        int neutrals = count(brideToGroom, groomToBride, GrahaFriendship.Relation.NEUTRAL);

        int points;
        String decision;
        if (friends == 2)                      { points = 5; decision = "graha_maitri.mutual_friends"; }
        else if (friends == 1 && neutrals == 1){ points = 4; decision = "graha_maitri.friend_neutral"; }
        else if (neutrals == 2)                { points = 3; decision = "graha_maitri.both_neutral"; }
        else if (friends == 1 && enemies == 1) { points = 2; decision = "graha_maitri.friend_enemy"; }
        else if (neutrals == 1 && enemies == 1){ points = 1; decision = "graha_maitri.neutral_enemy"; }
        else                                   { points = 0; decision = "graha_maitri.mutual_enemies"; }

        KootaScoreDTO k = base("graha_maitri", 5);
        k.setPoints(points);
        k.setInputs(inputs(
                "brideMoonRashi", brideMoon.getKey(),
                "groomMoonRashi", groomMoon.getKey(),
                "brideLord", brideLord.getKey(),
                "groomLord", groomLord.getKey(),
                "brideLordToGroomLord", brideToGroom.name().toLowerCase(java.util.Locale.ROOT),
                "groomLordToBrideLord", groomToBride.name().toLowerCase(java.util.Locale.ROOT)));
        k.setDecision(decision);
        k.setExplanationKey("vedic.koota.graha_maitri.explain");
        k.setExplanationArgs(inputs(
                "brideLord", brideLord.getKey(), "groomLord", groomLord.getKey(), "points", points));
        if (brideLord == groomLord) {
            k.setNoteKeys(List.of("vedic.koota.graha_maitri.note.same_lord"));
        }
        return k;
    }

    private static int count(GrahaFriendship.Relation a, GrahaFriendship.Relation b,
                             GrahaFriendship.Relation wanted) {
        return (a == wanted ? 1 : 0) + (b == wanted ? 1 : 0);
    }

    // ── Gana (max 6) ─────────────────────────────────────────────────────────

    /** Matrix der Quelle, Zeile = Braut, Spalte = Bräutigam, Reihenfolge Deva, Manushya, Rakshasa. */
    private static final int[][] GANA_POINTS = {
        /* Deva     */ {6, 6, 0},
        /* Manushya */ {5, 6, 0},
        /* Rakshasa */ {1, 0, 6},
    };

    private static KootaScoreDTO gana(Nakshatra bride, Nakshatra groom) {
        Gana brideGana = bride.getGana();
        Gana groomGana = groom.getGana();
        int points = GANA_POINTS[brideGana.ordinal()][groomGana.ordinal()];

        KootaScoreDTO k = base("gana", 6);
        k.setPoints(points);
        k.setInputs(inputs("brideGana", brideGana.getKey(), "groomGana", groomGana.getKey()));
        k.setDecision(brideGana == groomGana ? "gana.same_gana" : "gana.matrix");
        k.setExplanationKey("vedic.koota.gana.explain");
        k.setExplanationArgs(inputs(
                "brideGana", brideGana.getKey(), "groomGana", groomGana.getKey(), "points", points));
        return k;
    }

    // ── Bhakoot / Rasi (max 7) ───────────────────────────────────────────────

    /** Günstige Zeichenabstände nach der Quelle: 1., 3., 4., 7., 10., 11. */
    private static final java.util.Set<Integer> BHAKOOT_BENEFIC = java.util.Set.of(1, 3, 4, 7, 10, 11);

    /**
     * Bhakoot-Koota. Steht das eine Mondzeichen vom anderen im Verhältnis 2/12, 5/9 oder 6/8, gibt
     * es keine Punkte, sonst 7. Beide Zählrichtungen ergeben zwangsläufig dasselbe Urteil — bei
     * 1-basiert-inklusiver Zählung ergänzen sie sich für verschiedene Zeichen immer zu 14, daher
     * die übliche Schreibweise „2/12", „5/9", „6/8" —, werden aber beide ausgegeben, damit die
     * Erklärung genau diese Schreibweise benennen kann.
     *
     * <p>Die verbreiteten Aufhebungen (gleicher Zeichenherrscher, befreundete Herrscher) werden
     * nur als Hinweis gemeldet und <b>nicht</b> eingerechnet.
     */
    private static KootaScoreDTO bhakoot(Rashi brideMoon, Rashi groomMoon) {
        int brideToGroom = Rashi.countFrom(brideMoon, groomMoon);
        int groomToBride = Rashi.countFrom(groomMoon, brideMoon);
        boolean benefic = BHAKOOT_BENEFIC.contains(brideToGroom) && BHAKOOT_BENEFIC.contains(groomToBride);

        KootaScoreDTO k = base("bhakoot", 7);
        k.setPoints(benefic ? 7 : 0);
        k.setInputs(inputs(
                "brideMoonRashi", brideMoon.getKey(),
                "groomMoonRashi", groomMoon.getKey(),
                "countBrideToGroom", brideToGroom,
                "countGroomToBride", groomToBride,
                "pair", Math.min(brideToGroom, groomToBride) + "/" + Math.max(brideToGroom, groomToBride)));
        k.setDecision(benefic ? "bhakoot.benefic_position" : "bhakoot.malefic_position");
        k.setExplanationKey("vedic.koota.bhakoot.explain." + (benefic ? "benefic" : "malefic"));
        k.setExplanationArgs(inputs(
                "pair", Math.min(brideToGroom, groomToBride) + "/" + Math.max(brideToGroom, groomToBride)));
        if (!benefic) {
            List<String> notes = new ArrayList<>();
            if (brideMoon.getLord() == groomMoon.getLord()) {
                notes.add("vedic.koota.bhakoot.note.same_lord_not_applied");
            } else if (GrahaFriendship.relation(brideMoon.getLord(), groomMoon.getLord())
                            == GrahaFriendship.Relation.FRIEND
                    && GrahaFriendship.relation(groomMoon.getLord(), brideMoon.getLord())
                            == GrahaFriendship.Relation.FRIEND) {
                notes.add("vedic.koota.bhakoot.note.friendly_lords_not_applied");
            }
            if (!notes.isEmpty()) k.setNoteKeys(notes);
        }
        return k;
    }

    // ── Nadi (max 8) ─────────────────────────────────────────────────────────

    /**
     * Nadi-Koota: verschiedene Nadi → 8 Punkte, gleiche Nadi → 0.
     *
     * <p>Wie bei Bhakoot werden die verbreiteten Aufhebungen nur gemeldet, nicht eingerechnet.
     */
    private static KootaScoreDTO nadi(Nakshatra bride, Nakshatra groom,
                                      Rashi brideMoon, Rashi groomMoon) {
        boolean different = bride.getNadi() != groom.getNadi();

        KootaScoreDTO k = base("nadi", 8);
        k.setPoints(different ? 8 : 0);
        k.setInputs(inputs(
                "brideNadi", bride.getNadi().getKey(),
                "groomNadi", groom.getNadi().getKey(),
                "brideNadiAyurvedic", bride.getNadi().getAyurvedicKey(),
                "groomNadiAyurvedic", groom.getNadi().getAyurvedicKey()));
        k.setDecision(different ? "nadi.different" : "nadi.same");
        k.setExplanationKey("vedic.koota.nadi.explain." + (different ? "different" : "same"));
        k.setExplanationArgs(inputs("brideNadi", bride.getNadi().getKey(),
                "groomNadi", groom.getNadi().getKey()));
        if (!different) {
            List<String> notes = new ArrayList<>();
            if (brideMoon.getLord() == groomMoon.getLord()) {
                notes.add("vedic.koota.nadi.note.same_lord_not_applied");
            }
            if (bride == groom) {
                notes.add("vedic.koota.nadi.note.same_nakshatra");
            }
            if (!notes.isEmpty()) k.setNoteKeys(notes);
        }
        return k;
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────

    private static KootaScoreDTO base(String key, double maxPoints) {
        KootaScoreDTO k = new KootaScoreDTO();
        k.setKey(key);
        k.setNameKey("vedic.koota." + key);
        k.setMaxPoints(maxPoints);
        k.setNoteKeys(List.of());
        return k;
    }

    private static Map<String, Object> inputs(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    /** Halbe Punkte kommen vor (Vashya, Dina); Fließkomma-Rauschen in einer Summe nicht. */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
