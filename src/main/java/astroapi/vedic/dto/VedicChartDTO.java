package astroapi.vedic.dto;

import java.util.List;

/**
 * Das vedische Chart einer Person: D1 (Rashi) und D9 (Navamsa), streng getrennt.
 *
 * <p>{@code planets} ist D1. {@code navamsaPlanets} ist D9 — dieselben Körper, aber mit der
 * Navamsa-Länge. Zwei Listen statt einer mit Zusatzspalte, damit in keiner Deutung und in keiner
 * Tabelle eine D9-Position als D1-Position gelesen werden kann.
 *
 * <p>{@code ayanamsaDegrees} ist der <b>tatsächlich angewandte</b> Wert, gemessen als Differenz
 * zwischen tropischer und siderischer Sonnenlänge derselben Berechnung.
 * {@code ayanamsaApiDegrees} ist, was {@code swe_get_ayanamsa_ut} meldet. Die beiden weichen um
 * bis zu etwa 20 Bogensekunden ab, weil die Ephemeridenbibliothek beim Rechnen eine
 * Rahmenkorrektur anwendet, die die Abfragefunktion nicht enthält. Beide stehen hier, weil sonst
 * niemand nachrechnen kann, warum ein grenznaher Fall so und nicht anders ausfällt.
 */
public class VedicChartDTO {

    private String label;
    /** Lokale Wandzeit der Geburt, wie angefragt. */
    private String localDateTime;
    /** Dieselbe Zeit in UT — die Grundlage der Ephemeridenabfrage. */
    private String utDateTime;
    /** Aufgelöste IANA-Zeitzone des Geburtsorts. */
    private String zoneId;
    private double julianDayUT;
    private double[] coordinates;
    private boolean birthTimeKnown;

    private double ayanamsaDegrees;
    private String ayanamsaDegreesFormatted;
    private double ayanamsaApiDegrees;

    /**
     * Steht die zeitabhängige Hälfte des Charts überhaupt zur Verfügung?
     *
     * <p>{@code false}, wenn die Geburtszeit fehlt. Dann sind <b>alle</b> Aszendenten-Felder,
     * {@link #houses} und das Haus jedes Grahas {@code null} beziehungsweise leer — und zwar
     * nicht aus Vorsicht, sondern weil es sie nicht gibt: ohne Uhrzeit wird Mitternacht
     * angenommen, und ein Aszendent auf Mitternacht ist keine Angabe über die Geburt. Eine
     * Warnung daneben hätte das nicht verhindert; sie steht neben einer Zahl, die trotzdem
     * aussieht wie ein Ergebnis.
     *
     * <p>Mond-Rashi, Mond-Nakshatra und damit das ganze Ashtakoota bleiben erhalten — der Mond
     * hat eine Position, auch wenn sie um bis zu 13° unsicher ist. Darauf weist die Warnung hin.
     */
    private boolean timeDependentAvailable = true;

    /** Siderische Länge des Aszendenten in Grad; {@code null} ohne Geburtszeit. */
    private Double ascendantLongitude;
    private Double ascendantDegreeInSign;
    private String ascendantDegreeInSignFormatted;
    private Integer ascendantRashiIndex;
    private String ascendantRashi;
    private String ascendantRashiLord;
    private NakshatraPositionDTO ascendantNakshatra;

    /** Navamsa-Zeichen des Aszendenten; {@code null} ohne Geburtszeit. */
    private Integer ascendantNavamsaRashiIndex;
    private String ascendantNavamsaRashi;

    /** Mond-Rashi und -Nakshatra — die Grundlage sämtlicher Ashtakoota-Regeln. */
    private int moonRashiIndex;
    private String moonRashi;
    private NakshatraPositionDTO moonNakshatra;
    private String varna;
    private String vashya;

    private List<VedicPlanetDTO> planets;
    private List<VedicPlanetDTO> navamsaPlanets;
    private List<VedicHouseDTO> houses;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getLocalDateTime() { return localDateTime; }
    public void setLocalDateTime(String v) { this.localDateTime = v; }
    public String getUtDateTime() { return utDateTime; }
    public void setUtDateTime(String v) { this.utDateTime = v; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public double getJulianDayUT() { return julianDayUT; }
    public void setJulianDayUT(double v) { this.julianDayUT = v; }
    public double[] getCoordinates() { return coordinates; }
    public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }
    public boolean isBirthTimeKnown() { return birthTimeKnown; }
    public void setBirthTimeKnown(boolean v) { this.birthTimeKnown = v; }
    public double getAyanamsaDegrees() { return ayanamsaDegrees; }
    public void setAyanamsaDegrees(double v) { this.ayanamsaDegrees = v; }
    public String getAyanamsaDegreesFormatted() { return ayanamsaDegreesFormatted; }
    public void setAyanamsaDegreesFormatted(String v) { this.ayanamsaDegreesFormatted = v; }
    public double getAyanamsaApiDegrees() { return ayanamsaApiDegrees; }
    public void setAyanamsaApiDegrees(double v) { this.ayanamsaApiDegrees = v; }
    public boolean isTimeDependentAvailable() { return timeDependentAvailable; }
    public void setTimeDependentAvailable(boolean v) { this.timeDependentAvailable = v; }
    public Double getAscendantLongitude() { return ascendantLongitude; }
    public void setAscendantLongitude(Double v) { this.ascendantLongitude = v; }
    public Double getAscendantDegreeInSign() { return ascendantDegreeInSign; }
    public void setAscendantDegreeInSign(Double v) { this.ascendantDegreeInSign = v; }
    public String getAscendantDegreeInSignFormatted() { return ascendantDegreeInSignFormatted; }
    public void setAscendantDegreeInSignFormatted(String v) { this.ascendantDegreeInSignFormatted = v; }
    public Integer getAscendantRashiIndex() { return ascendantRashiIndex; }
    public void setAscendantRashiIndex(Integer v) { this.ascendantRashiIndex = v; }
    public String getAscendantRashi() { return ascendantRashi; }
    public void setAscendantRashi(String v) { this.ascendantRashi = v; }
    public String getAscendantRashiLord() { return ascendantRashiLord; }
    public void setAscendantRashiLord(String v) { this.ascendantRashiLord = v; }
    public NakshatraPositionDTO getAscendantNakshatra() { return ascendantNakshatra; }
    public void setAscendantNakshatra(NakshatraPositionDTO v) { this.ascendantNakshatra = v; }
    public Integer getAscendantNavamsaRashiIndex() { return ascendantNavamsaRashiIndex; }
    public void setAscendantNavamsaRashiIndex(Integer v) { this.ascendantNavamsaRashiIndex = v; }
    public String getAscendantNavamsaRashi() { return ascendantNavamsaRashi; }
    public void setAscendantNavamsaRashi(String v) { this.ascendantNavamsaRashi = v; }
    public int getMoonRashiIndex() { return moonRashiIndex; }
    public void setMoonRashiIndex(int v) { this.moonRashiIndex = v; }
    public String getMoonRashi() { return moonRashi; }
    public void setMoonRashi(String v) { this.moonRashi = v; }
    public NakshatraPositionDTO getMoonNakshatra() { return moonNakshatra; }
    public void setMoonNakshatra(NakshatraPositionDTO v) { this.moonNakshatra = v; }
    public String getVarna() { return varna; }
    public void setVarna(String varna) { this.varna = varna; }
    public String getVashya() { return vashya; }
    public void setVashya(String vashya) { this.vashya = vashya; }
    public List<VedicPlanetDTO> getPlanets() { return planets; }
    public void setPlanets(List<VedicPlanetDTO> planets) { this.planets = planets; }
    public List<VedicPlanetDTO> getNavamsaPlanets() { return navamsaPlanets; }
    public void setNavamsaPlanets(List<VedicPlanetDTO> v) { this.navamsaPlanets = v; }
    public List<VedicHouseDTO> getHouses() { return houses; }
    public void setHouses(List<VedicHouseDTO> houses) { this.houses = houses; }
}
