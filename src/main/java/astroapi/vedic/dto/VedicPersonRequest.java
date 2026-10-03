package astroapi.vedic.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Geburtsdaten einer Person für die vedische Berechnung.
 *
 * <p>Absichtlich feldgleich mit {@code astroapi.dto.AstroRequest} (lokale Wandzeit +
 * [Breite, Länge]), damit keine zweite Zeit- und Koordinatenlogik entsteht: die Zeitzone wird
 * genauso über {@code TimeZoneService} aus den Koordinaten bestimmt und die Wandzeit genauso nach
 * UT umgerechnet. Was fehlt, ist das Häusersystem — die vedische Berechnung verwendet immer
 * Whole Sign, eine Auswahl gäbe es hier nur zum Verwechseln.
 *
 * <p>{@link #isBirthTimeKnown()} ist kein Komfortfeld: ohne Geburtszeit sind Aszendent, Häuser
 * und Manglik-Bezug auf das Lagna nicht belastbar, und der Mond wandert bis zu 13° am Tag — also
 * durch bis zu ein ganzes Nakshatra. Steht das Flag auf {@code false}, liefert die Antwort
 * entsprechende Warnungen, statt Genauigkeit vorzutäuschen.
 */
public class VedicPersonRequest {

    /** Freier Bezeichner (z. B. „A"), nur zur Zuordnung in der Antwort. Keine Deutungsgrundlage. */
    private String label;

    @NotNull(message = "dateTime cannot be null")
    private LocalDateTime dateTime;

    @NotNull(message = "coordinates cannot be null")
    @Size(min = 2, max = 2, message = "coordinates must contain exactly two values: [latitude, longitude]")
    private double[] coordinates;

    /** Ist die Geburtszeit bekannt und verlässlich? Vorgabe {@code true}. */
    private boolean birthTimeKnown = true;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public double[] getCoordinates() { return coordinates; }
    public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }

    public boolean isBirthTimeKnown() { return birthTimeKnown; }
    public void setBirthTimeKnown(boolean birthTimeKnown) { this.birthTimeKnown = birthTimeKnown; }
}
