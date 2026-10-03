package astroapi.vedic;

/**
 * Versionsnummern des fachlichen Regelwerks — getrennt von der Software-Version.
 *
 * <p>Warum getrennt: ein Fehlerbehebung im Code darf gespeicherte Ergebnisse nicht ungültig
 * machen, eine geänderte Koota-Tabelle muss es. gAIster hängt diese Kennungen an jeden Cache- und
 * Persistenzschlüssel; ändert sich eine, rechnet der nächste Aufruf neu statt eine alte Antwort
 * weiterzugeben, die nach anderen Regeln entstanden ist.
 *
 * <p><b>Regel:</b> jede Änderung an einer Tabelle, einem Grenzwert oder einer Aufhebungsregel
 * erhöht die betroffene Kennung. Reine Umbenennungen von Anzeigetexten nicht.
 */
public final class VedicRuleSet {

    private VedicRuleSet() {}

    /** Berechnungssystem — unterscheidet die vedische Strecke von der westlichen. */
    public static final String CALCULATION_SYSTEM = "vedic-sidereal";

    /**
     * Version des Berechnungswegs: siderische Positionen, Whole-Sign-Häuser, Nakshatra/Pada,
     * Navamsa-Ableitung, Ketu-Herleitung.
     */
    public static final String CALCULATION_VERSION = "1.0.0";

    /**
     * Ashtakoota-Tabellenwerk. {@code saravali} benennt die dokumentierte Fassung (Asta Koota wie
     * im Maitreya/Saravali-Referenzwerk, CC BY-SA 4.0), {@code +dhanu} die eine ausdrücklich
     * ergänzte Zuordnung: die Quelltabelle lässt Schütze in der Vashya-Klassifikation aus.
     */
    public static final String ASHTAKOOTA_RULE_SET = "ashtakoota.saravali+dhanu.v1";

    /** Manglik/Kuja-Dosha-Regelwerk samt der implementierten Aufhebungsregeln. */
    public static final String MANGLIK_RULE_SET = "manglik.classical.v1";

    /** Höchstpunktzahl des Ashtakoota — fest, Teil des Vertrags mit App und Radix. */
    public static final int ASHTAKOOTA_MAX_POINTS = 36;

    /**
     * Ein Schlüsselbestandteil, der alles zusammenfasst, was ein Ergebnis bestimmt.
     * gAIster baut seine Cache-Schlüssel darauf auf.
     */
    public static String contractKey(Ayanamsa ayanamsa, NodeType nodeType) {
        return CALCULATION_SYSTEM + "|" + CALCULATION_VERSION + "|" + ASHTAKOOTA_RULE_SET
                + "|" + MANGLIK_RULE_SET + "|" + ayanamsa.getKey() + "|" + nodeType.getKey();
    }
}
