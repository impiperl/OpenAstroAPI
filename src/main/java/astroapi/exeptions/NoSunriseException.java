package astroapi.exeptions;

/**
 * An diesem Ort gibt es im gesuchten Fenster keinen Sonnenaufgang.
 *
 * <p>Jenseits der Polarkreise kann die Sonne wochenlang nicht aufgehen. Der vedische Tag beginnt
 * aber mit dem Sonnenaufgang — ohne ihn gibt es kein Panchanga, und auf das Kalenderdatum
 * auszuweichen wäre keine Näherung, sondern ein anderer Vara.
 *
 * <p>Das ist ein <b>erwartbarer, nicht berechenbarer Zustand</b> und kein interner Fehler.
 * Deshalb eine eigene Ausnahme mit eigenem Status (422) und maschinenlesbarem Code: Eine
 * Oberfläche soll den Fall benennen können, statt einen Serverfehler anzuzeigen.
 */
public class NoSunriseException extends RuntimeException {

    private final double latitude;
    private final double longitude;

    public NoSunriseException(double latitude, double longitude) {
        super("No sunrise within the search window at "
                + latitude + ", " + longitude + " — polar day or night.");
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
