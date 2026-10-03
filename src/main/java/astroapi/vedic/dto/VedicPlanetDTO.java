package astroapi.vedic.dto;

/**
 * Ein Graha in D1 (Rashi) — siderische Länge, Zeichen, Whole-Sign-Haus, Nakshatra und das
 * zugehörige D9-Zeichen.
 *
 * <p>Das Navamsa-Zeichen steht mit am Planeten und zusätzlich getrennt im D9-Block. Doppelt, aber
 * mit Absicht: die App zeigt D1 und D9 in eigenen Abschnitten, und eine Tabelle, die für ihre
 * zweite Spalte in ein anderes Objekt greifen muss, lädt zu Verwechslungen ein.
 */
public class VedicPlanetDTO {

    private String body;
    /** Siderische ekliptikale Länge in Grad, 0…360. */
    private double siderealLongitude;
    /** Grad innerhalb des Rashi, 0…30. */
    private double degreeInSign;
    /** Formatiert als {@code 12°34′56″} — nur Anzeige. */
    private String degreeInSignFormatted;
    private int rashiIndex;
    private String rashi;
    private String rashiLord;
    /**
     * Whole-Sign-Haus vom siderischen Aszendenten, 1…12 — oder {@code null}.
     *
     * <p>{@code null}, wenn die Geburtszeit nicht bekannt ist. Das Haus hängt am Aszendenten, und
     * der hängt an der Minute: ohne Zeit wäre jede Hausangabe eine Angabe über Mitternacht.
     * Deshalb steht hier dann nichts, statt eine Zahl, die man für gerechnet hält.
     */
    private Integer house;
    private boolean retrograde;
    /** {@code true} für Rahu/Ketu, die rechnerisch immer rückläufig laufen. */
    private boolean alwaysRetrograde;
    private int navamsaRashiIndex;
    private String navamsaRashi;
    private NakshatraPositionDTO nakshatra;

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public double getSiderealLongitude() { return siderealLongitude; }
    public void setSiderealLongitude(double siderealLongitude) { this.siderealLongitude = siderealLongitude; }
    public double getDegreeInSign() { return degreeInSign; }
    public void setDegreeInSign(double degreeInSign) { this.degreeInSign = degreeInSign; }
    public String getDegreeInSignFormatted() { return degreeInSignFormatted; }
    public void setDegreeInSignFormatted(String v) { this.degreeInSignFormatted = v; }
    public int getRashiIndex() { return rashiIndex; }
    public void setRashiIndex(int rashiIndex) { this.rashiIndex = rashiIndex; }
    public String getRashi() { return rashi; }
    public void setRashi(String rashi) { this.rashi = rashi; }
    public String getRashiLord() { return rashiLord; }
    public void setRashiLord(String rashiLord) { this.rashiLord = rashiLord; }
    public Integer getHouse() { return house; }
    public void setHouse(Integer house) { this.house = house; }
    public boolean isRetrograde() { return retrograde; }
    public void setRetrograde(boolean retrograde) { this.retrograde = retrograde; }
    public boolean isAlwaysRetrograde() { return alwaysRetrograde; }
    public void setAlwaysRetrograde(boolean alwaysRetrograde) { this.alwaysRetrograde = alwaysRetrograde; }
    public int getNavamsaRashiIndex() { return navamsaRashiIndex; }
    public void setNavamsaRashiIndex(int v) { this.navamsaRashiIndex = v; }
    public String getNavamsaRashi() { return navamsaRashi; }
    public void setNavamsaRashi(String navamsaRashi) { this.navamsaRashi = navamsaRashi; }
    public NakshatraPositionDTO getNakshatra() { return nakshatra; }
    public void setNakshatra(NakshatraPositionDTO nakshatra) { this.nakshatra = nakshatra; }
}
