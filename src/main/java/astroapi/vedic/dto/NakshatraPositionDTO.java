package astroapi.vedic.dto;

/**
 * Ein Nakshatra samt Pada und der genauen Lage darin.
 *
 * <p>{@code remainingDegrees} steht hier aus einem praktischen Grund: wer nahe einer Grenze
 * geboren ist, soll das sehen können. Liegt der Mond 0,05° vor dem nächsten Nakshatra, hängen
 * Nadi, Gana, Yoni und Tara an wenigen Minuten Geburtszeit — das gehört in die Oberfläche, nicht
 * ins Kleingedruckte.
 */
public class NakshatraPositionDTO {

    private int index;
    private String key;
    private String display;
    /** 1…4. */
    private int pada;
    private String lord;
    private String yoni;
    private boolean yoniMale;
    private String gana;
    private String nadi;
    /** Ayurvedische Benennung derselben Nadi (vata/pitta/kapha). */
    private String nadiAyurvedic;
    /** Siderische Länge des Körpers in Grad, 0…360. */
    private double siderealLongitude;
    /** Strecke innerhalb des Nakshatra, 0…13,333…°. */
    private double degreeInNakshatra;
    /** Verbleibende Strecke bis zum nächsten Nakshatra, in Grad. */
    private double remainingDegrees;
    /** Verbleibende Strecke bis zum nächsten Pada, in Grad. */
    private double remainingDegreesInPada;

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getDisplay() { return display; }
    public void setDisplay(String display) { this.display = display; }
    public int getPada() { return pada; }
    public void setPada(int pada) { this.pada = pada; }
    public String getLord() { return lord; }
    public void setLord(String lord) { this.lord = lord; }
    public String getYoni() { return yoni; }
    public void setYoni(String yoni) { this.yoni = yoni; }
    public boolean isYoniMale() { return yoniMale; }
    public void setYoniMale(boolean yoniMale) { this.yoniMale = yoniMale; }
    public String getGana() { return gana; }
    public void setGana(String gana) { this.gana = gana; }
    public String getNadi() { return nadi; }
    public void setNadi(String nadi) { this.nadi = nadi; }
    public String getNadiAyurvedic() { return nadiAyurvedic; }
    public void setNadiAyurvedic(String nadiAyurvedic) { this.nadiAyurvedic = nadiAyurvedic; }
    public double getSiderealLongitude() { return siderealLongitude; }
    public void setSiderealLongitude(double siderealLongitude) { this.siderealLongitude = siderealLongitude; }
    public double getDegreeInNakshatra() { return degreeInNakshatra; }
    public void setDegreeInNakshatra(double degreeInNakshatra) { this.degreeInNakshatra = degreeInNakshatra; }
    public double getRemainingDegrees() { return remainingDegrees; }
    public void setRemainingDegrees(double remainingDegrees) { this.remainingDegrees = remainingDegrees; }
    public double getRemainingDegreesInPada() { return remainingDegreesInPada; }
    public void setRemainingDegreesInPada(double remainingDegreesInPada) { this.remainingDegreesInPada = remainingDegreesInPada; }
}
