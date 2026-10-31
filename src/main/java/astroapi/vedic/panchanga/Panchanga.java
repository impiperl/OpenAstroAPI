package astroapi.vedic.panchanga;

import astroapi.vedic.Nakshatra;

/**
 * Die fünf Glieder eines vedischen Tages zu einem Zeitpunkt.
 *
 * <p>Ohne Wechselzeiten — die trägt der Ephemeridendienst nach, weil sie eine Suche über die Zeit
 * erfordern. Die {@code *Fraction}-Werte sagen, wie weit der laufende Abschnitt fortgeschritten
 * ist (0…1); daraus wird die erste Schätzung für diese Suche.
 *
 * @param tithiNumber     1…30, gezählt ab dem Neumond
 * @param karanaNumber    1…60, gezählt ab dem Neumond
 * @param yogaNumber      1…27
 * @param pada            1…4 innerhalb des Nakshatras
 */
public record Panchanga(
        int tithiNumber, Tithi tithi, Paksha paksha, double tithiFraction,
        int karanaNumber, Karana karana, double karanaFraction,
        int yogaNumber, NityaYoga yoga, double yogaFraction,
        Nakshatra nakshatra, int pada, double nakshatraFraction,
        Vara vara) {

    /** Der Name der Tithi in ihrer Hälfte — die Fünfzehnte heißt Purnima oder Amavasya. */
    public String tithiDisplayName() {
        return tithi.displayNameIn(paksha);
    }

    /** Der Übersetzungsschlüssel der Tithi in ihrer Hälfte. */
    public String tithiKey() {
        return tithi.keyIn(paksha);
    }
}
