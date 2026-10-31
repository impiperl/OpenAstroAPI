package astroapi.vedic.dto;

/**
 * Kaal Sarpa: stehen alle sieben klassischen Grahas auf einer Seite der Knotenachse?
 *
 * <p>{@code arc} sagt, auf welcher: {@code rahu_to_ketu} (in Tierkreisrichtung von Rahu bis Ketu)
 * oder {@code ketu_to_rahu}. Manche Schulen nennen nur den ersten Fall Kaal Sarpa und den zweiten
 * Kaal Amrita; andere zählen beide. Die Antwort nennt deshalb die Seite, statt die Schule zu
 * wählen. Ohne Befund ist {@code arc} {@code null}.
 */
public class KaalSarpaDTO {

    private boolean present;
    private String arc;

    public KaalSarpaDTO() {}

    public KaalSarpaDTO(boolean present, String arc) {
        this.present = present;
        this.arc = arc;
    }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
    public String getArc() { return arc; }
    public void setArc(String arc) { this.arc = arc; }
}
