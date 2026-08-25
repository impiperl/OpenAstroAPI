package astroapi.utils;

import swisseph.SweDate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Umrechnung zwischen Julianischem Datum (UT) und {@link OffsetDateTime}.
 *
 * <p>Alle Ephemeridenrechnungen laufen in UT. Deshalb wird an der Systemgrenze genau einmal
 * umgerechnet und innerhalb der Detektoren ausschließlich mit Julianischen Tagen gearbeitet —
 * das hält die Nullstellensuche frei von Zeitzonenlogik.
 */
public final class JulianTime {

    private JulianTime() {}

    /** Julianisches Datum (UT) für einen absoluten Zeitpunkt. */
    public static double toJulianDay(OffsetDateTime instant) {
        LocalDateTime utc = instant.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        double hours = utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0
                + utc.getNano() / 3_600_000_000_000.0;
        return new SweDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(), hours).getJulDay();
    }

    /**
     * Absoluter Zeitpunkt in UTC, auf ganze Sekunden gerundet.
     *
     * <p>Gerundet, nicht abgeschnitten: bei einer Bisektion auf Minutengenauigkeit ist ein
     * Ergebnis wie 20:59:59,7 sonst „20:59:59" statt „21:00:00".
     */
    public static OffsetDateTime toUtc(double julianDay) {
        SweDate sweDate = new SweDate(julianDay);
        double hours = sweDate.getHour();

        long nanosOfDay = Math.round(hours * 3_600_000_000_000L);
        LocalDateTime midnight = LocalDateTime.of(sweDate.getYear(), sweDate.getMonth(), sweDate.getDay(), 0, 0);
        LocalDateTime result = midnight.plusNanos(nanosOfDay);

        // Auf ganze Sekunden runden (halbe Sekunde aufwärts).
        Instant instant = result.toInstant(ZoneOffset.UTC).plusMillis(500);
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC).withNano(0);
    }

    /** Anzahl Tage zwischen zwei Julianischen Daten. */
    public static double days(double fromJd, double toJd) {
        return toJd - fromJd;
    }
}
