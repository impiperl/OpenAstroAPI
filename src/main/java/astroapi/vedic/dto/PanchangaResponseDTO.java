package astroapi.vedic.dto;

/**
 * Das Panchanga eines Augenblicks an einem Ort.
 *
 * <p>Alle Zeiten sind <b>Ortszeit mit Zonenangabe</b> (ISO-8601 mit Offset). Das ist Absicht: Ein
 * Panchanga ist eine Aussage über einen Ort, und eine Zeit in UTC müsste die Oberfläche
 * zurückrechnen — mit genau der Zeitzone, die dieser Dienst schon kennt.
 *
 * <p><b>Eine Momentaufnahme</b>, kein vollständiges Tages-Panchanga: Hier stehen die Glieder, die
 * zum gefragten Augenblick gelten, samt ihrem Ende. Wechselt eines im Lauf des Tages, taucht das
 * folgende nicht auf.
 *
 * <p>{@link #getVedicDayStart()} ist der Sonnenaufgang, der diesen vedischen Tag eröffnet. Er
 * steht bewusst oben in der Antwort: Er erklärt, warum ein Panchanga um 03:00 Uhr noch den
 * gestrigen Vara trägt.
 */
public class PanchangaResponseDTO {

    /** Berechnungsvertrag, wie bei der Partneranalyse — damit ein gespeichertes Ergebnis zuordenbar bleibt. */
    private String calculationSystem;
    private String calculationVersion;
    private String ayanamsa;

    /**
     * Regelkennung dieses Panchanga, insbesondere der Sonnenaufgangs-Konvention.
     *
     * <p>Eigenständig neben {@code calculationVersion}: Ein Wechsel der Konvention verschiebt
     * Vara und gelegentlich den Tithi am Tagesbeginn, ohne dass sich an der siderischen Rechnung
     * etwas ändert. Ohne diese Kennung ließe sich ein gespeichertes Ergebnis später nicht
     * zuordnen.
     */
    private String panchangaRuleSet;

    /** Der gefragte Zeitpunkt, in Ortszeit mit Zone. */
    private String moment;

    /** Sonnenaufgang, der den vedischen Tag eröffnet. */
    private String vedicDayStart;

    private PanchangaLimbDTO tithi;
    /** Mondhälfte: {@code shukla} (zunehmend) oder {@code krishna} (abnehmend). */
    private String paksha;
    private PanchangaLimbDTO karana;
    private PanchangaLimbDTO yoga;
    private PanchangaLimbDTO nakshatra;
    /** 1…4 innerhalb des Nakshatras. */
    private int pada;
    private PanchangaLimbDTO vara;
    /** Herrscher des Wochentags — Grundlage für Rahu Kala und die Muhurta-Ausschlüsse. */
    private String varaRuler;

    /**
     * Ist das laufende Karana Vishti (Bhadra)?
     *
     * <p>Das für die Terminwahl wichtigste Karana: In dieser Zeit beginnt man traditionell nichts
     * Bedeutendes. Es kommt achtmal im Monat vor.
     */
    private boolean karanaVishti;

    public String getCalculationSystem() { return calculationSystem; }
    public void setCalculationSystem(String v) { this.calculationSystem = v; }

    public String getCalculationVersion() { return calculationVersion; }
    public void setCalculationVersion(String v) { this.calculationVersion = v; }

    public String getAyanamsa() { return ayanamsa; }
    public void setAyanamsa(String v) { this.ayanamsa = v; }

    public String getPanchangaRuleSet() { return panchangaRuleSet; }
    public void setPanchangaRuleSet(String v) { this.panchangaRuleSet = v; }

    public String getMoment() { return moment; }
    public void setMoment(String v) { this.moment = v; }

    public String getVedicDayStart() { return vedicDayStart; }
    public void setVedicDayStart(String v) { this.vedicDayStart = v; }

    public PanchangaLimbDTO getTithi() { return tithi; }
    public void setTithi(PanchangaLimbDTO v) { this.tithi = v; }

    public String getPaksha() { return paksha; }
    public void setPaksha(String v) { this.paksha = v; }

    public PanchangaLimbDTO getKarana() { return karana; }
    public void setKarana(PanchangaLimbDTO v) { this.karana = v; }

    public PanchangaLimbDTO getYoga() { return yoga; }
    public void setYoga(PanchangaLimbDTO v) { this.yoga = v; }

    public PanchangaLimbDTO getNakshatra() { return nakshatra; }
    public void setNakshatra(PanchangaLimbDTO v) { this.nakshatra = v; }

    public int getPada() { return pada; }
    public void setPada(int v) { this.pada = v; }

    public PanchangaLimbDTO getVara() { return vara; }
    public void setVara(PanchangaLimbDTO v) { this.vara = v; }

    public String getVaraRuler() { return varaRuler; }
    public void setVaraRuler(String v) { this.varaRuler = v; }

    public boolean isKaranaVishti() { return karanaVishti; }
    public void setKaranaVishti(boolean v) { this.karanaVishti = v; }
}
