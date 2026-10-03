package astroapi.vedic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft die Tabellen selbst, bevor irgendetwas gerechnet wird.
 *
 * <p>Das ist die wichtigste Testklasse der vedischen Domäne: fast jeder denkbare Fehler in dieser
 * Funktion ist ein Tippfehler in einer Tabelle, und ein Tippfehler in einer Tabelle sieht im
 * Betrieb wie ein plausibles Ergebnis aus. Deshalb werden hier Vollständigkeit, Abdeckung und die
 * strukturellen Eigenschaften geprüft, die aus der Quelle folgen — nicht einzelne Beispielwerte.
 */
class VedicTablesTest {

    @Test
    @DisplayName("27 Nakshatras, lückenlos und in Reihenfolge indiziert")
    void nakshatraIndices() {
        assertEquals(27, Nakshatra.values().length);
        for (int i = 1; i <= 27; i++) {
            assertEquals(i, Nakshatra.byIndex(i).getIndex(), "Index " + i);
        }
        Set<String> keys = new HashSet<>();
        for (Nakshatra n : Nakshatra.values()) {
            assertTrue(keys.add(n.getKey()), "Doppelter Nakshatra-Schlüssel: " + n.getKey());
        }
    }

    @Test
    @DisplayName("Gana: je neun Nakshatras auf Deva, Manushya und Rakshasa")
    void ganaDistribution() {
        Map<Gana, Integer> counts = new EnumMap<>(Gana.class);
        for (Nakshatra n : Nakshatra.values()) counts.merge(n.getGana(), 1, Integer::sum);
        for (Gana g : Gana.values()) assertEquals(9, counts.get(g), "Gana " + g);
    }

    @Test
    @DisplayName("Nadi: je neun Nakshatras auf Adi, Madhya und Antya")
    void nadiDistribution() {
        Map<Nadi, Integer> counts = new EnumMap<>(Nadi.class);
        for (Nakshatra n : Nakshatra.values()) counts.merge(n.getNadi(), 1, Integer::sum);
        for (Nadi nadi : Nadi.values()) assertEquals(9, counts.get(nadi), "Nadi " + nadi);
    }

    @Test
    @DisplayName("Nadi folgt dem klassischen Zickzack über die 27 Nakshatras")
    void nadiZigzag() {
        // Adi: 1,6,7,12,13,18,19,24,25 · Madhya: 2,5,8,11,14,17,20,23,26
        // Antya: 3,4,9,10,15,16,21,22,27
        int[] adi = {1, 6, 7, 12, 13, 18, 19, 24, 25};
        int[] madhya = {2, 5, 8, 11, 14, 17, 20, 23, 26};
        int[] antya = {3, 4, 9, 10, 15, 16, 21, 22, 27};
        for (int i : adi) assertEquals(Nadi.ADI, Nakshatra.byIndex(i).getNadi(), "Nakshatra " + i);
        for (int i : madhya) assertEquals(Nadi.MADHYA, Nakshatra.byIndex(i).getNadi(), "Nakshatra " + i);
        for (int i : antya) assertEquals(Nadi.ANTYA, Nakshatra.byIndex(i).getNadi(), "Nakshatra " + i);
    }

    @Test
    @DisplayName("Alle 14 Yonis kommen vor; nur Mungo hat ein einziges Nakshatra")
    void yoniCoverage() {
        Map<Yoni, Integer> counts = new EnumMap<>(Yoni.class);
        for (Nakshatra n : Nakshatra.values()) counts.merge(n.getYoni(), 1, Integer::sum);
        assertEquals(14, counts.size(), "Es müssen alle 14 Yonis belegt sein");
        for (Yoni y : Yoni.values()) {
            int expected = y == Yoni.MONGOOSE ? 1 : 2;
            assertEquals(expected, counts.get(y),
                    "Yoni " + y + " — Mungo hat im 27er-Schema nur Uttara Ashadha, "
                            + "weil das zweite (Abhijit) hier nicht vorkommt");
        }
    }

    @Test
    @DisplayName("Yoni-Matrix: Diagonale 4, Erzfeinde 0, Werte nur 0…4")
    void yoniMatrixStructure() {
        for (Yoni bride : Yoni.values()) {
            assertEquals(4, Yoni.points(bride, bride), "Gleiche Yoni muss 4 ergeben: " + bride);
            assertEquals(0, Yoni.points(bride, bride.getInimical()),
                    "Erzfeind muss 0 ergeben: " + bride + " × " + bride.getInimical());
            assertEquals(0, Yoni.points(bride.getInimical(), bride),
                    "Auch umgekehrt 0: " + bride.getInimical() + " × " + bride);
            for (Yoni groom : Yoni.values()) {
                int p = Yoni.points(bride, groom);
                assertTrue(p >= 0 && p <= 4, "Wert außerhalb 0…4: " + bride + "×" + groom + "=" + p);
            }
        }
    }

    @Test
    @DisplayName("Erzfeindschaft ist wechselseitig und bildet sieben Paare")
    void inimicalPairsAreMutual() {
        Set<Yoni> seen = EnumSet.noneOf(Yoni.class);
        int pairs = 0;
        for (Yoni y : Yoni.values()) {
            assertEquals(y, y.getInimical().getInimical(), "Nicht wechselseitig: " + y);
            if (seen.add(y)) { seen.add(y.getInimical()); pairs++; }
        }
        assertEquals(7, pairs);
    }

    @Test
    @DisplayName("Yoni-Matrix ist asymmetrisch — das ist Absicht, nicht ein Tippfehler")
    void yoniMatrixIsAsymmetric() {
        // Belegbeispiel aus der Quelle: Braut Pferd × Bräutigam Hirsch = 3, umgekehrt 1.
        assertEquals(3, Yoni.points(Yoni.HORSE, Yoni.DEER));
        assertEquals(1, Yoni.points(Yoni.DEER, Yoni.HORSE));
    }

    @Test
    @DisplayName("Varna: je drei Zeichen, Element-Zuordnung wie dokumentiert")
    void varnaDistribution() {
        Map<VarnaClass, Integer> counts = new EnumMap<>(VarnaClass.class);
        for (Rashi r : Rashi.values()) counts.merge(r.getVarna(), 1, Integer::sum);
        for (VarnaClass v : VarnaClass.values()) assertEquals(3, counts.get(v), "Varna " + v);

        assertEquals(VarnaClass.BRAHMIN, Rashi.KARKA.getVarna());
        assertEquals(VarnaClass.BRAHMIN, Rashi.VRISHCHIKA.getVarna());
        assertEquals(VarnaClass.BRAHMIN, Rashi.MEENA.getVarna());
        assertEquals(VarnaClass.KSHATRIYA, Rashi.MESHA.getVarna());
        assertEquals(VarnaClass.KSHATRIYA, Rashi.SIMHA.getVarna());
        assertEquals(VarnaClass.KSHATRIYA, Rashi.DHANU.getVarna());
        assertEquals(VarnaClass.VAISHYA, Rashi.MITHUNA.getVarna());
        assertEquals(VarnaClass.VAISHYA, Rashi.TULA.getVarna());
        assertEquals(VarnaClass.VAISHYA, Rashi.KUMBHA.getVarna());
        assertEquals(VarnaClass.SHUDRA, Rashi.VRISHABHA.getVarna());
        assertEquals(VarnaClass.SHUDRA, Rashi.KANYA.getVarna());
        assertEquals(VarnaClass.SHUDRA, Rashi.MAKARA.getVarna());
    }

    @Test
    @DisplayName("Vashya: Halbteilung von Steinbock und Schütze greift")
    void vashyaHalves() {
        assertEquals(VashyaClass.QUADRUPED, Rashi.MAKARA.vashya(0.0));
        assertEquals(VashyaClass.QUADRUPED, Rashi.MAKARA.vashya(14.999));
        assertEquals(VashyaClass.JALACHARA, Rashi.MAKARA.vashya(15.0));
        assertEquals(VashyaClass.JALACHARA, Rashi.MAKARA.vashya(29.999));

        assertEquals(VashyaClass.HUMAN, Rashi.DHANU.vashya(0.0));
        assertEquals(VashyaClass.HUMAN, Rashi.DHANU.vashya(14.999));
        assertEquals(VashyaClass.QUADRUPED, Rashi.DHANU.vashya(15.0));

        // Die beiden Einzelzeichen-Klassen hängen nicht am Grad.
        assertEquals(VashyaClass.LEO, Rashi.SIMHA.vashya(0.0));
        assertEquals(VashyaClass.LEO, Rashi.SIMHA.vashya(29.9));
        assertEquals(VashyaClass.SCORPIO, Rashi.VRISHCHIKA.vashya(7.5));
    }

    @Test
    @DisplayName("Jedes Rashi hat einen der sieben klassischen Grahas als Herrscher")
    void rashiLordsAreClassical() {
        Set<VedicBody> classical = EnumSet.of(VedicBody.SUN, VedicBody.MOON, VedicBody.MARS,
                VedicBody.MERCURY, VedicBody.JUPITER, VedicBody.VENUS, VedicBody.SATURN);
        Map<VedicBody, Integer> counts = new EnumMap<>(VedicBody.class);
        for (Rashi r : Rashi.values()) {
            assertTrue(classical.contains(r.getLord()), r + " hat einen Nicht-Graha als Herrscher");
            counts.merge(r.getLord(), 1, Integer::sum);
        }
        // Sonne und Mond herrschen über je ein Zeichen, die übrigen fünf über je zwei.
        assertEquals(1, counts.get(VedicBody.SUN));
        assertEquals(1, counts.get(VedicBody.MOON));
        for (VedicBody b : new VedicBody[]{VedicBody.MARS, VedicBody.MERCURY, VedicBody.JUPITER,
                VedicBody.VENUS, VedicBody.SATURN}) {
            assertEquals(2, counts.get(b), "Herrscher " + b);
        }
    }

    @Test
    @DisplayName("Bewegungsart: je vier beweglich, fest und zweigestaltig, im Wechsel")
    void mobilityPattern() {
        Rashi.Mobility[] expected = {
            Rashi.Mobility.MOVABLE, Rashi.Mobility.FIXED, Rashi.Mobility.DUAL,
            Rashi.Mobility.MOVABLE, Rashi.Mobility.FIXED, Rashi.Mobility.DUAL,
            Rashi.Mobility.MOVABLE, Rashi.Mobility.FIXED, Rashi.Mobility.DUAL,
            Rashi.Mobility.MOVABLE, Rashi.Mobility.FIXED, Rashi.Mobility.DUAL,
        };
        for (int i = 0; i < 12; i++) {
            assertEquals(expected[i], Rashi.byIndex(i).getMobility(), "Rashi " + i);
        }
    }

    @Test
    @DisplayName("Natürliche Freundschaft: der Mond hat keinen Feind, Merkur zählt ihn als einen")
    void friendshipIsAsymmetricByDesign() {
        assertEquals(GrahaFriendship.Relation.NEUTRAL,
                GrahaFriendship.relation(VedicBody.MOON, VedicBody.SATURN));
        assertEquals(GrahaFriendship.Relation.ENEMY,
                GrahaFriendship.relation(VedicBody.SATURN, VedicBody.MOON));
        assertEquals(GrahaFriendship.Relation.FRIEND,
                GrahaFriendship.relation(VedicBody.MOON, VedicBody.MERCURY));
        assertEquals(GrahaFriendship.Relation.ENEMY,
                GrahaFriendship.relation(VedicBody.MERCURY, VedicBody.MOON));
    }

    @Test
    @DisplayName("Derselbe Graha gilt sich selbst als Freund")
    void sameGrahaIsFriend() {
        for (VedicBody b : VedicBody.classicalGrahas()) {
            assertEquals(GrahaFriendship.Relation.FRIEND, GrahaFriendship.relation(b, b), b.toString());
        }
    }

    @Test
    @DisplayName("Für Rahu und Ketu ist keine Freundschaft definiert — sie herrschen über kein Rashi")
    void nodesHaveNoFriendship() {
        assertThrows(IllegalArgumentException.class,
                () -> GrahaFriendship.relation(VedicBody.RAHU, VedicBody.SUN));
    }

    @Test
    @DisplayName("Zeichenabstand ist 1-basiert und inklusiv")
    void rashiCounting() {
        assertEquals(1, Rashi.countFrom(Rashi.MESHA, Rashi.MESHA));
        assertEquals(7, Rashi.countFrom(Rashi.MESHA, Rashi.TULA));
        assertEquals(7, Rashi.countFrom(Rashi.TULA, Rashi.MESHA));
        assertEquals(12, Rashi.countFrom(Rashi.VRISHABHA, Rashi.MESHA));
        assertEquals(2, Rashi.countFrom(Rashi.MESHA, Rashi.VRISHABHA));
        // Bei 1-basiert-inklusiver Zählung ergänzen sich beide Richtungen für verschiedene
        // Zeichen immer zu 14 — daher die klassischen Paarschreibweisen 2/12, 5/9, 6/8, 3/11, 4/10
        // und 7/7. Für dasselbe Zeichen ist es 1 + 1 = 2.
        for (Rashi a : Rashi.values()) {
            for (Rashi b : Rashi.values()) {
                int sum = Rashi.countFrom(a, b) + Rashi.countFrom(b, a);
                assertEquals(a == b ? 2 : 14, sum, a + "/" + b);
            }
        }
    }
}
