package astroapi.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

/**
 * Benannte Koordinaten.
 *
 * <p>Die älteren Endpunkte nehmen {@code double[2]} entgegen, wo die Reihenfolge nur im Kommentar
 * steht. Für den neuen Event-Endpunkt sind die Felder benannt — ein vertauschtes Paar ist sonst
 * nicht als Fehler erkennbar, sondern liefert stillschweigend ein Horoskop für den falschen Ort.
 */
public class GeoCoordinatesDTO {

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private double longitude;

    public GeoCoordinatesDTO() {}

    public GeoCoordinatesDTO(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    /** Für die Weitergabe an die bestehenden Helper, die {@code double[2]} erwarten. */
    public double[] toArray() {
        return new double[]{latitude, longitude};
    }
}
