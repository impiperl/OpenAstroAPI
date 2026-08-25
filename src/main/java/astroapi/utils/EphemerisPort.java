package astroapi.utils;

/**
 * Schmaler Zugang zur Swiss Ephemeris für die Event-Detektoren.
 *
 * <p>Der Port existiert aus zwei Gründen:
 *
 * <ul>
 *   <li><b>Ein Thread-Hop statt tausender.</b> Ein Range-Scan tastet den Zeitraum hundertfach ab.
 *       Ginge jeder Einzelaufruf über {@code runWithTimeout}, wäre der Overhead größer als die
 *       Rechnung selbst. Stattdessen läuft ein kompletter Scan in einer Executor-Task und bekommt
 *       diesen Port auf die {@code SwissEph}-Instanz des Worker-Threads.</li>
 *   <li><b>Testbarkeit.</b> Detektoren sind damit reine Funktionen über einer Ephemeride und lassen
 *       sich mit einem analytischen Fake prüfen, ohne Ephemeridendateien.</li>
 * </ul>
 */
public interface EphemerisPort {

    /**
     * Länge und Geschwindigkeit eines Körpers.
     *
     * @param julianDay Julianisches Datum in UT
     * @param planetId  Swiss-Ephemeris-Konstante (siehe {@link Planet})
     * @return {@code {Länge in Grad, Geschwindigkeit in Grad/Tag}}; beide {@code NaN} bei Fehler.
     *         Negative Geschwindigkeit bedeutet rückläufig.
     */
    double[] lonSpeed(double julianDay, int planetId);

    /** Nur die Länge — Bequemlichkeit für Detektoren, die keine Geschwindigkeit brauchen. */
    default double lon(double julianDay, int planetId) {
        return lonSpeed(julianDay, planetId)[0];
    }

    /**
     * Nächste Mondfinsternis ab dem Zeitpunkt.
     *
     * @return {@code {JD des Maximums, Typ-Flags}} oder {@code null}, wenn keine gefunden wurde
     */
    double[] nextLunarEclipse(double julianDayStart);

    /**
     * Nächste Sonnenfinsternis (global) ab dem Zeitpunkt.
     *
     * @return {@code {JD des Maximums, Typ-Flags}} oder {@code null}, wenn keine gefunden wurde
     */
    double[] nextSolarEclipse(double julianDayStart);
}
