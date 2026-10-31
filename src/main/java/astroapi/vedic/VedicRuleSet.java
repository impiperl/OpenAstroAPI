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

    /**
     * Regelkennung des Panchanga — eigenständig neben {@link #CALCULATION_VERSION}.
     *
     * <p>Warum eine eigene: Die allgemeine Version beschreibt die siderische Rechnung. Das
     * Panchanga hängt zusätzlich an einer Konvention, die nirgends sonst vorkommt — welcher
     * Sonnenaufgang gemeint ist. Hinduistisch heißt: Scheibenmittelpunkt ohne Refraktion, also
     * drei bis fünf Minuten später als der bürgerliche. Ein Wechsel dieser Konvention verschiebt
     * Vara und gelegentlich den Tithi am Tagesbeginn, ohne dass sich an der Ephemeridenrechnung
     * etwas ändert. Ein gespeichertes Ergebnis muss deshalb sagen können, nach welcher
     * Konvention es entstanden ist.
     *
     * <p>{@code hindu-sunrise} ist dabei die Absicht, nicht die vollständige Umsetzung: Der
     * verwendete Java-Port kennt {@code SE_BIT_HINDU_RISING} nicht und lässt dessen dritten
     * Bestandteil ({@code SE_BIT_GEOCTR_NO_ECL_LAT}) weg. Die verbleibende Abweichung liegt im
     * Sekundenbereich und ist in {@code VedicEphemerisService.lastSunriseBefore} beschrieben.
     */
    public static final String PANCHANGA_RULE_SET = "panchanga.lahiri.hindu-sunrise.v1";

    /**
     * Würden der Grahas: BPHS Kap. 3 mit Gradgrenzen für Erhöhung und Moolatrikona, Freund und
     * Feind nach der <b>natürlichen</b> Freundschaft. Rangfolge des stärksten Grahas: D1-Würde,
     * dann D9-Würde, dann natürliche Stärke. Siehe {@link GrahaDignity}, {@link KundliCalculator}.
     */
    public static final String DIGNITY_RULE_SET = "dignity.parashara.natural.v1";

    /** Kaal Sarpa: alle sieben klassischen Grahas in einem Halbkreis der Knotenachse. */
    public static final String KAAL_SARPA_RULE_SET = "kaalsarpa.hemisphere.v1";

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

    /**
     * Der Vertragsschlüssel der Kundli — ein eigener, weil sie an anderen Regelwerken hängt als
     * die Partneranalyse: kein Ashtakoota, dafür Würden und Kaal Sarpa.
     */
    public static String kundliContractKey(Ayanamsa ayanamsa, NodeType nodeType) {
        return CALCULATION_SYSTEM + "|" + CALCULATION_VERSION + "|" + DIGNITY_RULE_SET
                + "|" + MANGLIK_RULE_SET + "|" + KAAL_SARPA_RULE_SET
                + "|" + ayanamsa.getKey() + "|" + nodeType.getKey();
    }
}
