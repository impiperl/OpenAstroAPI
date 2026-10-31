package astroapi.vedic.dto;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Zeitpunkt und Ort für ein Panchanga.
 *
 * <h2>Zwei Wege, den Zeitpunkt zu nennen</h2>
 * <b>{@code instant}</b> (ISO-8601 mit Offset, etwa {@code 2026-10-04T12:00:00+02:00}) bezeichnet
 * einen Augenblick eindeutig. Das ist der empfohlene Weg.
 *
 * <p><b>{@code dateTime}</b> ist örtliche Wanduhrzeit und damit an zwei Tagen im Jahr mehrdeutig:
 * Bei der Rückstellung im Herbst gibt es jede Stunde zweimal, bei der Vorstellung im Frühjahr
 * fehlt eine. Weil ein Panchanga einen Augenblick beschreibt und nicht eine Beschriftung, ist das
 * kein akademischer Fall — eine um eine Stunde verschobene Auflösung kann an einer Glied-Grenze
 * einen anderen Tithi ergeben. Damit das Verhalten wenigstens <b>festgelegt</b> statt zufällig
 * ist, gilt: bei Doppeldeutigkeit der <b>frühere</b> Offset (noch Sommerzeit), bei einer Lücke
 * wird um die Lückenlänge <b>nach vorn</b> geschoben. Das ist Javas Standardverhalten; hier steht
 * es ausdrücklich, damit niemand es für Zufall hält.
 *
 * <p>Genau eines von beiden muss gesetzt sein.
 *
 * <h2>Der Ort ist Pflicht</h2>
 * Nicht aus Bequemlichkeit: Der vedische Tag beginnt mit dem Sonnenaufgang, und der hängt am Ort.
 * Dasselbe Datum ergibt in Hamburg und in Chennai verschiedene Panchangas.
 */
public class PanchangaRequest {

    /** Örtliche Wanduhrzeit. Mehrdeutig an DST-Grenzen — siehe Klassendoku. */
    private LocalDateTime dateTime;

    /** Eindeutiger Augenblick mit Offset. Vorzuziehen. */
    private OffsetDateTime instant;

    @NotNull(message = "coordinates cannot be null")
    @Size(min = 2, max = 2, message = "coordinates must contain exactly two values: [latitude, longitude]")
    private double[] coordinates;

    /**
     * Ein Zeitpunkt, nicht zwei und nicht keiner.
     *
     * <p>Beides zugleich anzunehmen hieße, sich für eines entscheiden zu müssen — und jede solche
     * Entscheidung wäre für die Hälfte der Aufrufer die falsche.
     */
    @AssertTrue(message = "exactly one of dateTime or instant must be set")
    public boolean isExactlyOneMomentGiven() {
        return (dateTime == null) != (instant == null);
    }

    /**
     * Breite −90…90, Länge −180…180, beide endlich.
     *
     * <p>Ohne diese Prüfung landet eine vertauschte Reihenfolge (Länge zuerst) oder ein
     * {@code NaN} ungebremst in der Ephemeride und kommt als Serverfehler zurück statt als
     * Hinweis auf die fehlerhafte Eingabe.
     */
    @AssertTrue(message = "coordinates must be [latitude -90..90, longitude -180..180] and finite")
    public boolean isCoordinatesInRange() {
        if (coordinates == null || coordinates.length != 2) return true;   // meldet @Size
        double latitude = coordinates[0];
        double longitude = coordinates[1];
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && Math.abs(latitude) <= 90.0 && Math.abs(longitude) <= 180.0;
    }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public OffsetDateTime getInstant() { return instant; }
    public void setInstant(OffsetDateTime instant) { this.instant = instant; }

    public double[] getCoordinates() { return coordinates; }
    public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }
}
