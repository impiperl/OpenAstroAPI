package astroapi.vedic.dto;

/**
 * Ein einzelner Ehefaktor als strukturierte Tatsache — kein Deutungstext.
 *
 * <p>Die KI darf diese Fakten erklären; sie darf sie nicht herstellen. Deshalb enthält jeder
 * Faktor nur Positionsangaben und technische Schlüssel, nie eine Wertung.
 */
public class MarriageFactorDTO {

    /** {@code ascendant}, {@code seventh_house}, {@code seventh_lord}, {@code venus}, … */
    private String key;
    private String rashi;
    /**
     * Whole-Sign-Haus, 1…12 — oder {@code null}, wenn der Faktor kein Haus hat oder die
     * Geburtszeit fehlt.
     */
    private Integer house;
    private double siderealLongitude;
    private String degreeInSignFormatted;
    private String navamsaRashi;
    private NakshatraPositionDTO nakshatra;
    /** Bei {@code seventh_lord}: welcher Graha das ist. */
    private String body;
    private boolean retrograde;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getRashi() { return rashi; }
    public void setRashi(String rashi) { this.rashi = rashi; }
    public Integer getHouse() { return house; }
    public void setHouse(Integer house) { this.house = house; }
    public double getSiderealLongitude() { return siderealLongitude; }
    public void setSiderealLongitude(double v) { this.siderealLongitude = v; }
    public String getDegreeInSignFormatted() { return degreeInSignFormatted; }
    public void setDegreeInSignFormatted(String v) { this.degreeInSignFormatted = v; }
    public String getNavamsaRashi() { return navamsaRashi; }
    public void setNavamsaRashi(String navamsaRashi) { this.navamsaRashi = navamsaRashi; }
    public NakshatraPositionDTO getNakshatra() { return nakshatra; }
    public void setNakshatra(NakshatraPositionDTO nakshatra) { this.nakshatra = nakshatra; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isRetrograde() { return retrograde; }
    public void setRetrograde(boolean retrograde) { this.retrograde = retrograde; }
}
