package astroapi.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Merkt sich Ephemeridenwerte für die Dauer <b>eines</b> Range-Scans.
 *
 * <p>Der Grund ist der Transit- und Aspekt-Scan: für einen laufenden Körper wird dasselbe Zeitraster
 * einmal je Zielwinkel und je Radixpunkt durchlaufen. Ohne Zwischenspeicher wären das für einen
 * Monat mit Radix rund 150 000 Aufrufe von {@code swe_calc_ut}; mit Zwischenspeicher bleiben die
 * paar tausend übrig, die tatsächlich verschiedene Zeitpunkte betreffen.
 *
 * <p>Der Schlüssel ist die <b>exakte</b> Bitfolge des Julianischen Datums, nicht ein gerundeter Wert.
 * Gerundet würden die eng beieinanderliegenden Auswertungen der Bisektion zusammenfallen und die
 * Nullstellensuche zum Stillstand bringen.
 *
 * <p>Nicht threadsicher — und das ist Absicht: die Instanz lebt genau so lange wie die eine
 * Executor-Task, in der der Scan läuft.
 */
public class MemoizingEphemerisPort implements EphemerisPort {

    /** Notbremse gegen unbegrenztes Wachstum bei sehr großen Zeiträumen. */
    private static final int MAX_ENTRIES = 500_000;

    private final EphemerisPort delegate;
    private final Map<Integer, Map<Long, double[]>> cache = new HashMap<>();
    private int entries;

    public MemoizingEphemerisPort(EphemerisPort delegate) {
        this.delegate = delegate;
    }

    @Override
    public double[] lonSpeed(double julianDay, int planetId) {
        Map<Long, double[]> perBody = cache.computeIfAbsent(planetId, k -> new HashMap<>());
        long key = Double.doubleToLongBits(julianDay);

        double[] cached = perBody.get(key);
        if (cached != null) return cached;

        double[] value = delegate.lonSpeed(julianDay, planetId);
        if (entries < MAX_ENTRIES) {
            perBody.put(key, value);
            entries++;
        }
        return value;
    }

    @Override
    public double[] nextLunarEclipse(double julianDayStart) {
        return delegate.nextLunarEclipse(julianDayStart);
    }

    @Override
    public double[] nextSolarEclipse(double julianDayStart) {
        return delegate.nextSolarEclipse(julianDayStart);
    }

    /** Anzahl tatsächlich gespeicherter Stützstellen — für Logging und Metriken. */
    public int cachedEntries() {
        return entries;
    }
}
