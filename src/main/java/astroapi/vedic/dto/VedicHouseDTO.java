package astroapi.vedic.dto;

import java.util.List;

/**
 * Ein Whole-Sign-Haus: ein Zeichen, ein Haus, keine Zwischengrade.
 *
 * <p>Whole Sign heißt, dass das Zeichen des Aszendenten vollständig das erste Haus ist, das
 * nächste Zeichen vollständig das zweite und so weiter. Es gibt deshalb keine Spitzen (Cusps) in
 * Grad — der Aszendentengrad entscheidet nur, welches Zeichen das erste Haus ist. Das ist der
 * Grund, weshalb die vedische Berechnung kein Häusersystem-Auswahlfeld hat.
 */
public class VedicHouseDTO {

    /** 1…12. */
    private int house;
    private int rashiIndex;
    private String rashi;
    private String lord;
    /** Schlüssel der Grahas in diesem Haus, in Zeichenreihenfolge. */
    private List<String> bodies;

    public int getHouse() { return house; }
    public void setHouse(int house) { this.house = house; }
    public int getRashiIndex() { return rashiIndex; }
    public void setRashiIndex(int rashiIndex) { this.rashiIndex = rashiIndex; }
    public String getRashi() { return rashi; }
    public void setRashi(String rashi) { this.rashi = rashi; }
    public String getLord() { return lord; }
    public void setLord(String lord) { this.lord = lord; }
    public List<String> getBodies() { return bodies; }
    public void setBodies(List<String> bodies) { this.bodies = bodies; }
}
