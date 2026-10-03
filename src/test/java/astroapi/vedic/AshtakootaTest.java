package astroapi.vedic;

import astroapi.vedic.dto.AshtakootaOrientationDTO;
import astroapi.vedic.dto.KootaScoreDTO;
import astroapi.vedic.dto.NakshatraPositionDTO;
import astroapi.vedic.dto.VedicChartDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ashtakoota: Höchstwerte, Teilwerte, Richtungsabhängigkeit und die Zusicherung, dass nie mehr als
 * 36 Punkte entstehen.
 *
 * <p>Gerechnet wird gegen <b>synthetische Charts</b>, nicht gegen Ephemeridenwerte: die Regeln
 * hängen nur an Mondzeichen und Mond-Nakshatra. So lässt sich jede Tabellenzelle gezielt treffen,
 * statt Geburtsdaten zu suchen, die zufällig dort landen — und ein fehlgeschlagener Test benennt
 * die Regel, nicht ein Datum.
 */
class AshtakootaTest {

    // ── Hilfen ───────────────────────────────────────────────────────────────

    /** Ein Chart, von dem nur Mondzeichen und Mond-Nakshatra belegt sind. */
    private static VedicChartDTO chart(String label, Rashi moonRashi, double degreeInMoonSign,
                                       Nakshatra moonNakshatra) {
        VedicChartDTO c = new VedicChartDTO();
        c.setLabel(label);
        c.setMoonRashiIndex(moonRashi.getIndex());
        c.setMoonRashi(moonRashi.getKey());
        c.setVarna(moonRashi.getVarna().getKey());
        c.setVashya(moonRashi.vashya(degreeInMoonSign).getKey());
        NakshatraPositionDTO n = new NakshatraPositionDTO();
        n.setIndex(moonNakshatra.getIndex());
        n.setKey(moonNakshatra.getKey());
        c.setMoonNakshatra(n);
        return c;
    }

    /** Chart mit dem ersten Nakshatra, das im gewünschten Zeichen liegt — reicht für Zeichenregeln. */
    private static VedicChartDTO chartIn(String label, Rashi moonRashi) {
        double mid = 15.0;
        double longitude = moonRashi.getIndex() * 30.0 + mid;
        return chart(label, moonRashi, mid, Nakshatra.ofLongitude(longitude));
    }

    private static KootaScoreDTO koota(AshtakootaOrientationDTO r, String key) {
        return r.getKootas().stream().filter(k -> key.equals(k.getKey())).findFirst()
                .orElseThrow(() -> new AssertionError("Koota fehlt: " + key));
    }

    private static AshtakootaOrientationDTO calc(VedicChartDTO bride, VedicChartDTO groom) {
        return AshtakootaCalculator.calculate(bride, groom, TraditionalRole.A_AS_BRIDE);
    }

    // ── Struktur ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Acht Kootas, Höchstwerte 1+2+3+4+5+6+7+8 = 36")
    void structureAndMaxima() {
        AshtakootaOrientationDTO r = calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.MESHA));
        assertEquals(8, r.getKootas().size());
        assertEquals(AshtakootaCalculator.KOOTA_ORDER,
                r.getKootas().stream().map(KootaScoreDTO::getKey).collect(Collectors.toList()));

        Map<String, Double> maxima = Map.of(
                "varna", 1.0, "vashya", 2.0, "dina", 3.0, "yoni", 4.0,
                "graha_maitri", 5.0, "gana", 6.0, "bhakoot", 7.0, "nadi", 8.0);
        double sum = 0;
        for (KootaScoreDTO k : r.getKootas()) {
            assertEquals(maxima.get(k.getKey()), k.getMaxPoints(), "Höchstwert " + k.getKey());
            sum += k.getMaxPoints();
        }
        assertEquals(36.0, sum);
        assertEquals(VedicRuleSet.ASHTAKOOTA_MAX_POINTS, r.getMaxPoints());
    }

    @Test
    @DisplayName("Jedes Koota liefert Schlüssel, Namensschlüssel, Eingangsdaten und Regelentscheidung")
    void everyKootaIsSelfExplaining() {
        AshtakootaOrientationDTO r = calc(chartIn("A", Rashi.KARKA), chartIn("B", Rashi.VRISHCHIKA));
        for (KootaScoreDTO k : r.getKootas()) {
            assertNotNull(k.getKey(), "key");
            assertEquals("vedic.koota." + k.getKey(), k.getNameKey(), "nameKey " + k.getKey());
            assertNotNull(k.getDecision(), "decision " + k.getKey());
            assertFalse(k.getDecision().isBlank(), "decision leer " + k.getKey());
            assertNotNull(k.getExplanationKey(), "explanationKey " + k.getKey());
            assertNotNull(k.getInputs(), "inputs " + k.getKey());
            assertFalse(k.getInputs().isEmpty(), "inputs leer " + k.getKey());
            assertNotNull(k.getNoteKeys(), "noteKeys " + k.getKey());
            assertTrue(k.getPoints() >= 0 && k.getPoints() <= k.getMaxPoints(),
                    k.getKey() + ": " + k.getPoints() + " von " + k.getMaxPoints());
        }
    }

    @Test
    @DisplayName("Keine Kombination überschreitet 36 Punkte oder fällt unter 0")
    void totalAlwaysWithinBounds() {
        for (Nakshatra bn : Nakshatra.values()) {
            for (Nakshatra gn : Nakshatra.values()) {
                // Mondzeichen aus der Nakshatra-Mitte ableiten, damit die Paare zusammenpassen.
                double bl = (bn.getIndex() - 1) * VedicMath.NAKSHATRA_SPAN + VedicMath.NAKSHATRA_SPAN / 2;
                double gl = (gn.getIndex() - 1) * VedicMath.NAKSHATRA_SPAN + VedicMath.NAKSHATRA_SPAN / 2;
                VedicChartDTO b = chart("A", Rashi.ofLongitude(bl), VedicMath.degreeInSign(bl), bn);
                VedicChartDTO g = chart("B", Rashi.ofLongitude(gl), VedicMath.degreeInSign(gl), gn);
                double total = calc(b, g).getTotalPoints();
                assertTrue(total >= 0 && total <= 36,
                        bn + " × " + gn + " ergab " + total);
            }
        }
    }

    @Test
    @DisplayName("Identische Mondstellungen: 36 Punkte sind nicht erreichbar — Bhakoot und Nadi fallen aus")
    void identicalMoonsCannotScoreFull() {
        // Lehrreich und ein guter Wächter: gleiches Zeichen heißt Bhakoot 1/1 (7 Punkte), aber
        // gleiches Nakshatra heißt zwangsläufig gleiche Nadi — also 0 von 8.
        AshtakootaOrientationDTO r = calc(
                chart("A", Rashi.MESHA, 5.0, Nakshatra.ASHWINI),
                chart("B", Rashi.MESHA, 5.0, Nakshatra.ASHWINI));
        assertEquals(7.0, koota(r, "bhakoot").getPoints());
        assertEquals(0.0, koota(r, "nadi").getPoints());
        assertEquals(28.0, r.getTotalPoints());
        assertTrue(koota(r, "nadi").getNoteKeys().contains("vedic.koota.nadi.note.same_nakshatra"));
    }

    // ── Varna ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Varna: Bräutigam gleich oder höher gibt den Punkt, niedriger nicht")
    void varna() {
        // Braut Shudra (Stier), Bräutigam Brahmin (Krebs) → günstig
        assertEquals(1.0, koota(calc(chartIn("A", Rashi.VRISHABHA), chartIn("B", Rashi.KARKA)), "varna").getPoints());
        // Braut Brahmin (Krebs), Bräutigam Shudra (Stier) → 0
        assertEquals(0.0, koota(calc(chartIn("A", Rashi.KARKA), chartIn("B", Rashi.VRISHABHA)), "varna").getPoints());
        // Gleiche Varna → günstig
        assertEquals(1.0, koota(calc(chartIn("A", Rashi.KARKA), chartIn("B", Rashi.MEENA)), "varna").getPoints());
    }

    // ── Vashya ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Vashya: gleiche Klasse 2, Löwe-Spalte streng, halbe Punkte kommen vor")
    void vashya() {
        // Beide Human (Zwillinge × Waage) → 2
        assertEquals(2.0, koota(calc(chartIn("A", Rashi.MITHUNA), chartIn("B", Rashi.TULA)), "vashya").getPoints());
        // Braut Quadruped (Widder) × Bräutigam Human (Zwillinge) → 0 nach der Tabelle
        assertEquals(0.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.MITHUNA)), "vashya").getPoints());
        // Umgekehrt: Human × Quadruped → 1 — die Tabelle ist asymmetrisch
        assertEquals(1.0, koota(calc(chartIn("A", Rashi.MITHUNA), chartIn("B", Rashi.MESHA)), "vashya").getPoints());
        // Braut Jalachara (Krebs) × Bräutigam Quadruped (Widder) → 0,5
        assertEquals(0.5, koota(calc(chartIn("A", Rashi.KARKA), chartIn("B", Rashi.MESHA)), "vashya").getPoints());
        // Löwe als Braut gibt nur gegen Löwe Punkte
        assertEquals(0.0, koota(calc(chartIn("A", Rashi.SIMHA), chartIn("B", Rashi.KARKA)), "vashya").getPoints());
        assertEquals(2.0, koota(calc(chartIn("A", Rashi.SIMHA), chartIn("B", Rashi.SIMHA)), "vashya").getPoints());
    }

    @Test
    @DisplayName("Vashya: die Halbteilung von Steinbock schlägt in den Punkten durch")
    void vashyaCapricornHalves() {
        VedicChartDTO brideEarlyCapricorn = chart("A", Rashi.MAKARA, 5.0,
                Nakshatra.ofLongitude(9 * 30.0 + 5.0));
        VedicChartDTO brideLateCapricorn = chart("A", Rashi.MAKARA, 20.0,
                Nakshatra.ofLongitude(9 * 30.0 + 20.0));
        VedicChartDTO groomAries = chartIn("B", Rashi.MESHA);

        // 1. Hälfte = Quadruped × Quadruped → 2
        assertEquals(2.0, koota(calc(brideEarlyCapricorn, groomAries), "vashya").getPoints());
        // 2. Hälfte = Jalachara × Quadruped → 0,5
        assertEquals(0.5, koota(calc(brideLateCapricorn, groomAries), "vashya").getPoints());
    }

    // ── Dina / Tara ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Dina: Teilwerte 1,5 und 3 — 0 ist in diesem Regelwerk nicht erreichbar")
    void dina() {
        boolean sawHalf = false;
        boolean sawFull = false;
        for (Nakshatra b : Nakshatra.values()) {
            for (Nakshatra g : Nakshatra.values()) {
                double points = koota(calc(
                        chart("A", Rashi.MESHA, 5, b), chart("B", Rashi.MESHA, 5, g)), "dina").getPoints();
                // Strukturelle Eigenschaft des gewählten Regelwerks, und kein Zufall: bei
                // 1-basiert-inklusiver Zählung ergänzen sich die beiden Abstände immer zu 29, also
                // zu 2 modulo 9. Zwei Reste aus {3, 5, 7} können aber niemals 2 modulo 9 ergeben.
                // Deshalb ist höchstens eine der beiden Richtungen ungünstig und Dina liefert
                // immer 1,5 oder 3. Dieser Test ist der Wächter dafür: fällt er, hat sich die
                // Zählweise oder die Liste der ungünstigen Taras geändert.
                assertTrue(points == 1.5 || points == 3.0,
                        b + " × " + g + " ergab " + points);
                if (points == 1.5) sawHalf = true;
                if (points == 3.0) sawFull = true;
            }
        }
        assertTrue(sawHalf, "Der Teilwert 1,5 muss vorkommen");
        assertTrue(sawFull, "Der Höchstwert 3 muss vorkommen");

        // Gleiches Nakshatra: Abstand 1 in beide Richtungen, Rest 1 → beide günstig → 3
        assertEquals(3.0, koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.ASHWINI),
                chart("B", Rashi.MESHA, 5, Nakshatra.ASHWINI)), "dina").getPoints());
        // Braut Krittika (3), Bräutigam Ashwini (1): vom Bräutigam zur Braut sind es 3 Nakshatras,
        // Rest 3 → ungünstig. Zurück sind es 26, Rest 8 → günstig. Also 1,5.
        KootaScoreDTO half = koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.KRITTIKA),
                chart("B", Rashi.MESHA, 5, Nakshatra.ASHWINI)), "dina");
        assertEquals(1.5, half.getPoints());
        assertEquals("dina.one_benefic", half.getDecision());
        assertEquals(3, half.getInputs().get("countGroomToBride"));
        assertEquals(26, half.getInputs().get("countBrideToGroom"));
    }

    @Test
    @DisplayName("Dina ist richtungsunabhängig — beide Zählrichtungen gehen gleichberechtigt ein")
    void dinaIsSymmetric() {
        for (Nakshatra b : Nakshatra.values()) {
            for (Nakshatra g : Nakshatra.values()) {
                VedicChartDTO cb = chart("A", Rashi.MESHA, 5, b);
                VedicChartDTO cg = chart("B", Rashi.MESHA, 5, g);
                assertEquals(
                        koota(AshtakootaCalculator.calculate(cb, cg, TraditionalRole.A_AS_BRIDE), "dina").getPoints(),
                        koota(AshtakootaCalculator.calculate(cg, cb, TraditionalRole.B_AS_BRIDE), "dina").getPoints(),
                        b + " × " + g);
            }
        }
        assertFalse(AshtakootaCalculator.ROLE_DEPENDENT_KOOTAS.contains("dina"));
    }

    // ── Yoni ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Yoni: gleiche Yoni 4, Erzfeinde 0, asymmetrische Zelle belegt")
    void yoni() {
        // Ashwini und Shatabhisha sind beide Pferd → 4
        assertEquals(4.0, koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.ASHWINI),
                chart("B", Rashi.MESHA, 5, Nakshatra.SHATABHISHA)), "yoni").getPoints());
        // Pferd × Büffel sind Erzfeinde → 0, mit Hinweis
        KootaScoreDTO enemy = koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.ASHWINI),
                chart("B", Rashi.MESHA, 5, Nakshatra.SWATI)), "yoni");
        assertEquals(0.0, enemy.getPoints());
        assertEquals("yoni.inimical_pair", enemy.getDecision());
        assertTrue(enemy.getNoteKeys().contains("vedic.koota.yoni.note.inimical"));
        // Braut Pferd × Bräutigam Hirsch = 3, umgekehrt 1
        assertEquals(3.0, koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.ASHWINI),
                chart("B", Rashi.MESHA, 5, Nakshatra.JYESHTHA)), "yoni").getPoints());
        assertEquals(1.0, koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.JYESHTHA),
                chart("B", Rashi.MESHA, 5, Nakshatra.ASHWINI)), "yoni").getPoints());
    }

    // ── Graha Maitri ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Graha Maitri: alle sechs Stufen sind erreichbar und richtig zugeordnet")
    void grahaMaitri() {
        // Derselbe Herrscher (Widder und Skorpion → Mars) → 5
        assertEquals(5.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.VRISHCHIKA)), "graha_maitri").getPoints());
        // Mond (Krebs) × Sonne (Löwe): beide Freunde → 5
        assertEquals(5.0, koota(calc(chartIn("A", Rashi.KARKA), chartIn("B", Rashi.SIMHA)), "graha_maitri").getPoints());
        // Mond (Krebs) × Saturn (Steinbock): Mond neutral zu Saturn, Saturn feind dem Mond → 1
        KootaScoreDTO mixed = koota(calc(chartIn("A", Rashi.KARKA), chartIn("B", Rashi.MAKARA)), "graha_maitri");
        assertEquals(1.0, mixed.getPoints());
        assertEquals("graha_maitri.neutral_enemy", mixed.getDecision());
        // Sonne (Löwe) × Venus (Stier): gegenseitige Feindschaft → 0
        assertEquals(0.0, koota(calc(chartIn("A", Rashi.SIMHA), chartIn("B", Rashi.VRISHABHA)), "graha_maitri").getPoints());
        // Mars (Widder) × Saturn (Steinbock): Mars neutral zu Saturn, Saturn feind dem Mars → 1
        assertEquals(1.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.MAKARA)), "graha_maitri").getPoints());
        // Jupiter (Schütze) × Saturn (Steinbock): beide neutral → 3
        assertEquals(3.0, koota(calc(chartIn("A", Rashi.DHANU), chartIn("B", Rashi.MAKARA)), "graha_maitri").getPoints());
        // Mars (Widder) × Merkur (Zwillinge): Mars feind Merkur, Merkur neutral Mars → 1
        assertEquals(1.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.MITHUNA)), "graha_maitri").getPoints());
        // Sonne (Löwe) × Merkur (Zwillinge): Sonne neutral zu Merkur, Merkur Freund der Sonne → 4
        assertEquals(4.0, koota(calc(chartIn("A", Rashi.SIMHA), chartIn("B", Rashi.MITHUNA)), "graha_maitri").getPoints());
        // Jupiter (Schütze) × Venus (Stier): Jupiter feind Venus, Venus neutral Jupiter → 1
        assertEquals(1.0, koota(calc(chartIn("A", Rashi.DHANU), chartIn("B", Rashi.VRISHABHA)), "graha_maitri").getPoints());
        // Mond (Krebs) × Merkur (Zwillinge): Mond Freund, Merkur Feind → 2
        assertEquals(2.0, koota(calc(chartIn("A", Rashi.KARKA), chartIn("B", Rashi.MITHUNA)), "graha_maitri").getPoints());
    }

    @Test
    @DisplayName("Graha Maitri ist richtungsunabhängig, auch bei asymmetrischer Freundschaft")
    void grahaMaitriIsSymmetric() {
        for (Rashi a : Rashi.values()) {
            for (Rashi b : Rashi.values()) {
                double forward = koota(calc(chartIn("A", a), chartIn("B", b)), "graha_maitri").getPoints();
                double reverse = koota(calc(chartIn("A", b), chartIn("B", a)), "graha_maitri").getPoints();
                assertEquals(forward, reverse, a + " × " + b);
            }
        }
    }

    // ── Gana ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Gana: Matrix der Quelle, einschließlich der beiden asymmetrischen Zellen")
    void gana() {
        VedicChartDTO deva = chart("x", Rashi.MESHA, 5, Nakshatra.ASHWINI);          // Deva
        VedicChartDTO manushya = chart("x", Rashi.MESHA, 5, Nakshatra.BHARANI);      // Manushya
        VedicChartDTO rakshasa = chart("x", Rashi.MESHA, 5, Nakshatra.KRITTIKA);     // Rakshasa

        assertEquals(6.0, koota(calc(deva, deva), "gana").getPoints());
        assertEquals(6.0, koota(calc(deva, manushya), "gana").getPoints());
        assertEquals(0.0, koota(calc(deva, rakshasa), "gana").getPoints());
        assertEquals(5.0, koota(calc(manushya, deva), "gana").getPoints());
        assertEquals(6.0, koota(calc(manushya, manushya), "gana").getPoints());
        assertEquals(0.0, koota(calc(manushya, rakshasa), "gana").getPoints());
        assertEquals(1.0, koota(calc(rakshasa, deva), "gana").getPoints());
        assertEquals(0.0, koota(calc(rakshasa, manushya), "gana").getPoints());
        assertEquals(6.0, koota(calc(rakshasa, rakshasa), "gana").getPoints());
    }

    // ── Bhakoot ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bhakoot: 1/1, 3/11, 4/10 und 7/7 geben 7 Punkte, 2/12, 5/9 und 6/8 keinen")
    void bhakoot() {
        // Abstand 1 (gleiches Zeichen)
        assertEquals(7.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.MESHA)), "bhakoot").getPoints());
        // Abstand 7 (Opposition)
        assertEquals(7.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.TULA)), "bhakoot").getPoints());
        // Abstand 3 / 11
        assertEquals(7.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.MITHUNA)), "bhakoot").getPoints());
        // Abstand 4 / 10
        assertEquals(7.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.KARKA)), "bhakoot").getPoints());
        // Abstand 2 / 12
        assertEquals(0.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.VRISHABHA)), "bhakoot").getPoints());
        // Abstand 5 / 9
        assertEquals(0.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.SIMHA)), "bhakoot").getPoints());
        // Abstand 6 / 8
        assertEquals(0.0, koota(calc(chartIn("A", Rashi.MESHA), chartIn("B", Rashi.KANYA)), "bhakoot").getPoints());
    }

    @Test
    @DisplayName("Bhakoot: die bekannte Aufhebung wird gemeldet, aber nicht eingerechnet")
    void bhakootCancellationIsReportedOnly() {
        // Widder (Mars) und Jungfrau (Merkur) stehen 6/8 — Mars ist Merkur feind, also greift
        // keine der beiden Aufhebungsbedingungen; nur ein Gegenbeispiel wäre aussagelos.
        // Steinbock (Saturn) und Wassermann (Saturn) stehen 2/12 bei gleichem Herrscher.
        KootaScoreDTO k = koota(calc(chartIn("A", Rashi.MAKARA), chartIn("B", Rashi.KUMBHA)), "bhakoot");
        assertEquals(0.0, k.getPoints(), "Die Aufhebung darf die Punktzahl nicht verändern");
        assertTrue(k.getNoteKeys().contains("vedic.koota.bhakoot.note.same_lord_not_applied"),
                "Die Bedingung muss gemeldet werden: " + k.getNoteKeys());
    }

    // ── Nadi ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Nadi: verschiedene Nadi 8, gleiche 0")
    void nadi() {
        // Ashwini = Adi, Bharani = Madhya
        assertEquals(8.0, koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.ASHWINI),
                chart("B", Rashi.MESHA, 5, Nakshatra.BHARANI)), "nadi").getPoints());
        // Ashwini und Ardra sind beide Adi
        assertEquals(0.0, koota(calc(
                chart("A", Rashi.MESHA, 5, Nakshatra.ASHWINI),
                chart("B", Rashi.MESHA, 5, Nakshatra.ARDRA)), "nadi").getPoints());
    }

    // ── Rollenabhängigkeit ───────────────────────────────────────────────────

    @Test
    @DisplayName("Nur Varna, Vashya, Yoni und Gana hängen von der Reihenfolge ab")
    void onlyDeclaredKootasAreRoleDependent() {
        // Ein Paar suchen, bei dem sich etwas unterscheidet, und prüfen, dass es nur die
        // angekündigten Kootas sind.
        for (Rashi ra : Rashi.values()) {
            for (Rashi rb : Rashi.values()) {
                VedicChartDTO a = chartIn("A", ra);
                VedicChartDTO b = chartIn("B", rb);
                AshtakootaOrientationDTO forward = AshtakootaCalculator.calculate(a, b, TraditionalRole.A_AS_BRIDE);
                AshtakootaOrientationDTO reverse = AshtakootaCalculator.calculate(b, a, TraditionalRole.B_AS_BRIDE);
                for (int i = 0; i < 8; i++) {
                    KootaScoreDTO f = forward.getKootas().get(i);
                    KootaScoreDTO r = reverse.getKootas().get(i);
                    if (f.getPoints() != r.getPoints()) {
                        assertTrue(AshtakootaCalculator.ROLE_DEPENDENT_KOOTAS.contains(f.getKey()),
                                "Unerwartet richtungsabhängig: " + f.getKey()
                                        + " (" + ra + " / " + rb + ")");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Die Rollenwahl kann die Gesamtpunktzahl verändern — das muss sichtbar bleiben")
    void roleOrderCanChangeTheTotal() {
        VedicChartDTO a = chart("A", Rashi.KARKA, 5, Nakshatra.ofLongitude(3 * 30.0 + 5.0));
        VedicChartDTO b = chart("B", Rashi.VRISHABHA, 5, Nakshatra.ofLongitude(1 * 30.0 + 5.0));
        double aAsBride = AshtakootaCalculator.calculate(a, b, TraditionalRole.A_AS_BRIDE).getTotalPoints();
        double bAsBride = AshtakootaCalculator.calculate(b, a, TraditionalRole.B_AS_BRIDE).getTotalPoints();
        assertNotEquals(aAsBride, bAsBride,
                "Braut Krebs/Bräutigam Stier und umgekehrt müssen sich im Varna unterscheiden");
        assertEquals(List.of("varna", "vashya", "yoni", "gana"),
                AshtakootaCalculator.calculate(a, b, TraditionalRole.A_AS_BRIDE).getRoleDependent());
    }

    @Test
    @DisplayName("Die Rolle entscheidet die Beschriftung, nicht ein Geschlecht")
    void labelsFollowTheRoleOnly() {
        VedicChartDTO a = chartIn("Anna", Rashi.MESHA);
        VedicChartDTO b = chartIn("Ben", Rashi.KARKA);
        AshtakootaOrientationDTO r = AshtakootaCalculator.calculate(b, a, TraditionalRole.B_AS_BRIDE);
        assertEquals("Ben", r.getBrideLabel());
        assertEquals("Anna", r.getGroomLabel());
        assertEquals("b_as_bride", r.getRole());
    }

    @Test
    @DisplayName("Die Summe stimmt mit der Summe der Teilwerte überein")
    void totalMatchesParts() {
        AshtakootaOrientationDTO r = calc(
                chart("A", Rashi.KARKA, 5, Nakshatra.PUSHYA),
                chart("B", Rashi.MAKARA, 20, Nakshatra.SHRAVANA));
        double sum = r.getKootas().stream().mapToDouble(KootaScoreDTO::getPoints).sum();
        assertEquals(sum, r.getTotalPoints(), 1e-9);
    }
}
