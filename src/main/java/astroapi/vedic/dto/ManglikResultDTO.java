package astroapi.vedic.dto;

import java.util.List;

/**
 * Manglik-/Kuja-Dosha-Befund einer Person.
 *
 * <p>Ausdrücklich <b>kein</b> Boolean allein. Ausgegeben werden: die Mars-Stellung, jeder der drei
 * Bezugspunkte einzeln, die geprüften Häuser, jede greifende Aufhebungsregel mit ihrer Kennung und
 * ein Schweregrad. {@code severity} ist eine Zusammenfassung der Einzelbefunde dieses Regelwerks,
 * keine klassische Größe — sie ist in der Dokumentation als solche benannt.
 *
 * <p>Was hier bewusst fehlt: jede Aussage über Verlauf oder Ausgang einer Ehe. Das Ergebnis
 * benennt Stellungen und Regeln, nicht Schicksale.
 */
public class ManglikResultDTO {

    private String label;
    /**
     * Stand der Lagna-Bezug zur Verfügung?
     *
     * <p>{@code false} ohne Geburtszeit. Dann fehlt der Bezugspunkt {@code lagna} in
     * {@link #getReferences()}, die drei hausgebundenen Aufhebungen greifen nicht, und
     * {@link #getSeverity()} kann höchstens {@code mild} werden.
     */
    private boolean lagnaAvailable = true;
    /** Liegt nach diesem Regelwerk ein Befund vor — vor Anwendung der Aufhebungsregeln? */
    private boolean manglik;
    /** Bleibt nach den Aufhebungsregeln ein Befund? */
    private boolean manglikAfterCancellation;
    /** {@code none}, {@code mild}, {@code moderate}, {@code strong} — siehe Klassendoku. */
    private String severity;

    private String marsRashi;
    private double marsSiderealLongitude;
    private String marsDegreeInSignFormatted;
    private String marsNavamsaRashi;
    private NakshatraPositionDTO marsNakshatra;

    /** Die geprüften Häuser dieses Regelwerks — immer mitgeliefert, damit nichts implizit bleibt. */
    private List<Integer> checkedHouses;
    private List<ManglikReferenceDTO> references;
    /** Kennungen der greifenden Aufhebungsregeln, z. B. {@code mars_own_sign}. */
    private List<String> cancellations;
    /** Kennungen der Aufhebungsregeln, die dieses Regelwerk kennt, aber hier nicht greifen. */
    private List<String> cancellationsChecked;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isLagnaAvailable() { return lagnaAvailable; }
    public void setLagnaAvailable(boolean v) { this.lagnaAvailable = v; }
    public boolean isManglik() { return manglik; }
    public void setManglik(boolean manglik) { this.manglik = manglik; }
    public boolean isManglikAfterCancellation() { return manglikAfterCancellation; }
    public void setManglikAfterCancellation(boolean v) { this.manglikAfterCancellation = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getMarsRashi() { return marsRashi; }
    public void setMarsRashi(String marsRashi) { this.marsRashi = marsRashi; }
    public double getMarsSiderealLongitude() { return marsSiderealLongitude; }
    public void setMarsSiderealLongitude(double v) { this.marsSiderealLongitude = v; }
    public String getMarsDegreeInSignFormatted() { return marsDegreeInSignFormatted; }
    public void setMarsDegreeInSignFormatted(String v) { this.marsDegreeInSignFormatted = v; }
    public String getMarsNavamsaRashi() { return marsNavamsaRashi; }
    public void setMarsNavamsaRashi(String v) { this.marsNavamsaRashi = v; }
    public NakshatraPositionDTO getMarsNakshatra() { return marsNakshatra; }
    public void setMarsNakshatra(NakshatraPositionDTO v) { this.marsNakshatra = v; }
    public List<Integer> getCheckedHouses() { return checkedHouses; }
    public void setCheckedHouses(List<Integer> checkedHouses) { this.checkedHouses = checkedHouses; }
    public List<ManglikReferenceDTO> getReferences() { return references; }
    public void setReferences(List<ManglikReferenceDTO> references) { this.references = references; }
    public List<String> getCancellations() { return cancellations; }
    public void setCancellations(List<String> cancellations) { this.cancellations = cancellations; }
    public List<String> getCancellationsChecked() { return cancellationsChecked; }
    public void setCancellationsChecked(List<String> v) { this.cancellationsChecked = v; }
}
