package astroapi.dto;

import java.time.OffsetDateTime;

/** Ein Körper an einer Stützstelle: wo er steht und wie schnell er läuft. */
public class EphemerisSampleDTO {

    private OffsetDateTime at;

    /** Name wie in {@code Planet} — stabil und nie übersetzt. */
    private String body;

    /** Ekliptikale Länge in Grad, 0–360. */
    private double longitude;

    /** Grad pro Tag; negativ heißt rückläufig. */
    private double speed;

    private boolean retrograde;

    private String sign;

    /** Grad innerhalb des Zeichens, 0–30. */
    private double degreeInSign;

    public EphemerisSampleDTO() {}

    public EphemerisSampleDTO(OffsetDateTime at, String body, double longitude, double speed,
                              String sign, double degreeInSign) {
        this.at = at;
        this.body = body;
        this.longitude = longitude;
        this.speed = speed;
        this.retrograde = speed < 0;
        this.sign = sign;
        this.degreeInSign = degreeInSign;
    }

    public OffsetDateTime getAt() { return at; }
    public void setAt(OffsetDateTime at) { this.at = at; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public boolean isRetrograde() { return retrograde; }
    public void setRetrograde(boolean retrograde) { this.retrograde = retrograde; }
    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
    public double getDegreeInSign() { return degreeInSign; }
    public void setDegreeInSign(double degreeInSign) { this.degreeInSign = degreeInSign; }
}
