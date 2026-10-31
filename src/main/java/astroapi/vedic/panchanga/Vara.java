package astroapi.vedic.panchanga;

import astroapi.vedic.VedicBody;

/**
 * Der Wochentag als Glied des Panchanga — mit seinem Herrscher.
 *
 * <p>Der vedische Tag läuft von <b>Sonnenaufgang zu Sonnenaufgang</b>, nicht von Mitternacht zu
 * Mitternacht. Zwischen Mitternacht und Sonnenaufgang gilt deshalb noch der Vara des Vortages.
 * Das ist keine Kleinigkeit: Rahu Kala und die meisten Muhurta-Ausschlüsse hängen am Vara, und
 * eine um einen Tag verschobene Zuordnung verschiebt sie alle mit.
 */
public enum Vara {

    RAVIVARA(0, "ravivara", "Ravivara", java.time.DayOfWeek.SUNDAY, VedicBody.SUN),
    SOMAVARA(1, "somavara", "Somavara", java.time.DayOfWeek.MONDAY, VedicBody.MOON),
    MANGALAVARA(2, "mangalavara", "Mangalavara", java.time.DayOfWeek.TUESDAY, VedicBody.MARS),
    BUDHAVARA(3, "budhavara", "Budhavara", java.time.DayOfWeek.WEDNESDAY, VedicBody.MERCURY),
    GURUVARA(4, "guruvara", "Guruvara", java.time.DayOfWeek.THURSDAY, VedicBody.JUPITER),
    SHUKRAVARA(5, "shukravara", "Shukravara", java.time.DayOfWeek.FRIDAY, VedicBody.VENUS),
    SHANIVARA(6, "shanivara", "Shanivara", java.time.DayOfWeek.SATURDAY, VedicBody.SATURN);

    private final int index;
    private final String key;
    private final String displayName;
    private final java.time.DayOfWeek dayOfWeek;
    private final VedicBody ruler;

    Vara(int index, String key, String displayName, java.time.DayOfWeek dayOfWeek, VedicBody ruler) {
        this.index = index;
        this.key = key;
        this.displayName = displayName;
        this.dayOfWeek = dayOfWeek;
        this.ruler = ruler;
    }

    /** 0 = Sonntag … 6 = Samstag. */
    public int getIndex() { return index; }
    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public java.time.DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public VedicBody getRuler() { return ruler; }

    /**
     * Der Vara zu einem Kalendertag.
     *
     * <p>Erwartet wird das Datum des <b>Sonnenaufgangs</b>, der den vedischen Tag eröffnet — nicht
     * das Datum des gefragten Zeitpunkts. Wer um 03:00 Uhr fragt, bekommt den Vortag.
     */
    public static Vara ofSunriseDate(java.time.LocalDate sunriseDate) {
        java.time.DayOfWeek dow = sunriseDate.getDayOfWeek();
        for (Vara vara : values()) {
            if (vara.dayOfWeek == dow) return vara;
        }
        throw new IllegalStateException("unreachable: unknown day of week " + dow);
    }
}
