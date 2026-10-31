package astroapi.vedic.dto;

/**
 * Ein Glied des Panchanga in der Antwort.
 *
 * <p>Immer dreiteilig: die laufende Nummer zum Rechnen, der Schlüssel zum Übersetzen und der
 * Name zum Anzeigen, falls keine Übersetzung vorliegt. Dazu der Fortschritt und — soweit
 * bestimmbar — das Ende.
 *
 * <p>{@code endsAt} darf fehlen. Das ist eine ehrliche Antwort und kein Fehler: Findet die Suche
 * im Fenster keinen Übergang, wird keine Zeit erfunden.
 */
public class PanchangaLimbDTO {

    private int number;
    private String key;
    private String name;
    /** Fortschritt innerhalb des laufenden Abschnitts, 0…1. */
    private double fraction;
    /** Ortszeit des Wechsels; {@code null}, wenn im Suchfenster keiner liegt. */
    private String endsAt;

    public PanchangaLimbDTO() { }

    public PanchangaLimbDTO(int number, String key, String name, double fraction, String endsAt) {
        this.number = number;
        this.key = key;
        this.name = name;
        this.fraction = fraction;
        this.endsAt = endsAt;
    }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getFraction() { return fraction; }
    public void setFraction(double fraction) { this.fraction = fraction; }

    public String getEndsAt() { return endsAt; }
    public void setEndsAt(String endsAt) { this.endsAt = endsAt; }
}
