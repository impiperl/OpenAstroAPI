package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.EphemerisPort;
import astroapi.utils.EventKeys;
import astroapi.utils.EventType;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.ScanCancelledException;
import astroapi.utils.ZodiacSign;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mehrgliedrige Figuren: Stellium, großes Trigon, T-Quadrat, Grand Cross und Yod.
 *
 * <p>Anders als die übrigen Detektoren sucht dieser hier keinen exakten Zeitpunkt, sondern einen
 * <b>Zeitraum</b>. Eine Figur ist kein Durchgang, sondern ein Zustand, der Tage bis Wochen anhält.
 * Deshalb: tageweise prüfen, gleiche Figuren über aufeinanderfolgende Tage zusammenziehen,
 * {@code exact = false}.
 *
 * <p><b>Ohne Mond.</b> Der Mond zieht in zweieinhalb Tagen durch ein Zeichen und würde ständig
 * kurzlebige Figuren erzeugen, die sich am nächsten Tag wieder auflösen.
 */
public class ConfigurationDetector implements EventDetector {

    private static final Planet[] BODIES = {
            Planet.SUN, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER,
            Planet.SATURN, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO
    };

    private static final double STEP_DAYS = 1.0;

    private static final double ORB_MAJOR = 6.0;     // Trigon, Quadrat, Opposition
    private static final double ORB_SEXTILE = 4.0;
    private static final double ORB_QUINCUNX = 3.0;

    /** Ab wie vielen Körpern im selben Zeichen von einem Stellium gesprochen wird. */
    private static final int STELLIUM_MIN_BODIES = 3;

    @Override
    public String name() {
        return "configuration";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        Map<String, OpenFigure> open = new LinkedHashMap<>();
        List<AstroEventRawDTO> events = new ArrayList<>();

        boolean firstSample = true;
        double lastSampledJd = ctx.jdFrom();

        for (double jd = ctx.jdFrom(); jd <= ctx.jdTo(); jd += STEP_DAYS) {
            ScanCancelledException.throwIfInterrupted("configuration scan");
            double sampleJd = Math.min(jd, ctx.jdTo());
            Map<String, Figure> found = figuresAt(ctx.ephemeris(), sampleJd);

            // Fortbestehende Figuren verlängern, neue eröffnen.
            for (Map.Entry<String, Figure> entry : found.entrySet()) {
                OpenFigure ongoing = open.get(entry.getKey());
                if (ongoing == null) {
                    open.put(entry.getKey(), new OpenFigure(entry.getValue(), sampleJd, firstSample));
                } else {
                    ongoing.lastSeenJd = sampleJd;
                }
            }

            // Was heute fehlt, ist beendet.
            open.entrySet().removeIf(entry -> {
                if (found.containsKey(entry.getKey())) return false;
                events.add(entry.getValue().toEvent(false));
                return true;
            });

            firstSample = false;
            lastSampledJd = sampleJd;
            if (sampleJd >= ctx.jdTo()) break;
        }

        // Am Fensterrand noch offene Figuren: sie laufen weiter, das Ende ist unbekannt.
        for (OpenFigure ongoing : open.values()) {
            ongoing.lastSeenJd = lastSampledJd;
            events.add(ongoing.toEvent(true));
        }
        return events;
    }

    // ── Figurenerkennung auf einem Tagesschnappschuss ─────────────────────────

    private static Map<String, Figure> figuresAt(EphemerisPort eph, double jd) {
        Map<String, Figure> figures = new LinkedHashMap<>();

        double[] longitudes = new double[BODIES.length];
        for (int i = 0; i < BODIES.length; i++) {
            longitudes[i] = eph.lon(jd, BODIES[i].getSweConstValue());
        }

        addStelliums(figures, longitudes);
        addTriangles(figures, longitudes);
        addGrandCrosses(figures, longitudes);
        return figures;
    }

    private static void addStelliums(Map<String, Figure> figures, double[] longitudes) {
        Map<Integer, List<Integer>> bySign = new LinkedHashMap<>();
        for (int i = 0; i < longitudes.length; i++) {
            if (Double.isNaN(longitudes[i])) continue;
            bySign.computeIfAbsent(AngleMath.signIndex(longitudes[i]), k -> new ArrayList<>()).add(i);
        }
        for (Map.Entry<Integer, List<Integer>> entry : bySign.entrySet()) {
            if (entry.getValue().size() >= STELLIUM_MIN_BODIES) {
                Figure figure = new Figure("stellium", entry.getValue());
                figure.sign = ZodiacSign.values()[entry.getKey()].getName();
                figures.put(figure.identity(), figure);
            }
        }
    }

    private static void addTriangles(Map<String, Figure> figures, double[] lon) {
        for (int a = 0; a < lon.length; a++) {
            for (int b = a + 1; b < lon.length; b++) {
                for (int c = b + 1; c < lon.length; c++) {
                    if (Double.isNaN(lon[a]) || Double.isNaN(lon[b]) || Double.isNaN(lon[c])) continue;

                    if (isAspect(lon[a], lon[b], 120, ORB_MAJOR)
                            && isAspect(lon[b], lon[c], 120, ORB_MAJOR)
                            && isAspect(lon[a], lon[c], 120, ORB_MAJOR)) {
                        put(figures, new Figure("grand_trine", List.of(a, b, c)));
                        continue;
                    }

                    Figure tSquare = detectTSquare(lon, a, b, c);
                    if (tSquare != null) {
                        put(figures, tSquare);
                        continue;
                    }

                    Figure yod = detectYod(lon, a, b, c);
                    if (yod != null) put(figures, yod);
                }
            }
        }
    }

    /** Zwei Körper in Opposition, der dritte im Quadrat zu beiden — er ist der Apex. */
    private static Figure detectTSquare(double[] lon, int a, int b, int c) {
        int[][] arrangements = {{a, b, c}, {a, c, b}, {b, c, a}};
        for (int[] arrangement : arrangements) {
            int one = arrangement[0], two = arrangement[1], apex = arrangement[2];
            if (isAspect(lon[one], lon[two], 180, ORB_MAJOR)
                    && isAspect(lon[apex], lon[one], 90, ORB_MAJOR)
                    && isAspect(lon[apex], lon[two], 90, ORB_MAJOR)) {
                Figure figure = new Figure("t_square", List.of(one, two, apex));
                figure.apex = BODIES[apex].getName();
                return figure;
            }
        }
        return null;
    }

    /** Zwei Körper im Sextil, beide im Quincunx zum dritten — dem Apex. */
    private static Figure detectYod(double[] lon, int a, int b, int c) {
        int[][] arrangements = {{a, b, c}, {a, c, b}, {b, c, a}};
        for (int[] arrangement : arrangements) {
            int one = arrangement[0], two = arrangement[1], apex = arrangement[2];
            if (isAspect(lon[one], lon[two], 60, ORB_SEXTILE)
                    && isAspect(lon[apex], lon[one], 150, ORB_QUINCUNX)
                    && isAspect(lon[apex], lon[two], 150, ORB_QUINCUNX)) {
                Figure figure = new Figure("yod", List.of(one, two, apex));
                figure.apex = BODIES[apex].getName();
                return figure;
            }
        }
        return null;
    }

    /** Vier Körper als zwei Oppositionen, die einander im Quadrat stehen. */
    private static void addGrandCrosses(Map<String, Figure> figures, double[] lon) {
        for (int a = 0; a < lon.length; a++) {
            for (int b = a + 1; b < lon.length; b++) {
                if (Double.isNaN(lon[a]) || Double.isNaN(lon[b])) continue;
                if (!isAspect(lon[a], lon[b], 180, ORB_MAJOR)) continue;

                for (int c = 0; c < lon.length; c++) {
                    if (c == a || c == b || Double.isNaN(lon[c])) continue;
                    for (int d = c + 1; d < lon.length; d++) {
                        if (d == a || d == b || Double.isNaN(lon[d])) continue;
                        if (!isAspect(lon[c], lon[d], 180, ORB_MAJOR)) continue;

                        if (isAspect(lon[a], lon[c], 90, ORB_MAJOR) && isAspect(lon[a], lon[d], 90, ORB_MAJOR)
                                && isAspect(lon[b], lon[c], 90, ORB_MAJOR) && isAspect(lon[b], lon[d], 90, ORB_MAJOR)) {
                            put(figures, new Figure("grand_cross", List.of(a, b, c, d)));
                        }
                    }
                }
            }
        }
    }

    private static boolean isAspect(double lonA, double lonB, double angle, double orb) {
        return Math.abs(AngleMath.separation(lonA, lonB) - angle) <= orb;
    }

    private static void put(Map<String, Figure> figures, Figure figure) {
        figures.putIfAbsent(figure.identity(), figure);
    }

    // ── Hilfsstrukturen ───────────────────────────────────────────────────────

    /** Eine an einem Tag erkannte Figur. */
    private static final class Figure {
        final String kind;
        final List<Integer> bodyIndexes;
        String sign;
        String apex;

        Figure(String kind, List<Integer> bodyIndexes) {
            this.kind = kind;
            this.bodyIndexes = new ArrayList<>(bodyIndexes);
            this.bodyIndexes.sort(Integer::compareTo);
        }

        /** Identität über Art und beteiligte Körper — so wird dieselbe Figur über Tage erkannt. */
        String identity() {
            StringBuilder sb = new StringBuilder(kind);
            for (int index : bodyIndexes) sb.append(':').append(BODIES[index].getName());
            if (sign != null) sb.append('@').append(sign);
            return sb.toString();
        }

        List<String> bodyNames() {
            List<String> names = new ArrayList<>();
            for (int index : bodyIndexes) names.add(BODIES[index].getName());
            return names;
        }
    }

    /** Eine Figur, die noch andauert. */
    private static final class OpenFigure {
        final Figure figure;
        final double firstSeenJd;
        final boolean clippedStart;
        double lastSeenJd;

        OpenFigure(Figure figure, double firstSeenJd, boolean clippedStart) {
            this.figure = figure;
            this.firstSeenJd = firstSeenJd;
            this.lastSeenJd = firstSeenJd;
            this.clippedStart = clippedStart;
        }

        AstroEventRawDTO toEvent(boolean clippedEnd) {
            AstroEventRawDTO event = new AstroEventRawDTO(
                    EventKeys.configuration(figure.kind), EventType.CONFIGURATION.name());
            event.setStartsAt(JulianTime.toUtc(firstSeenJd));
            event.setEndsAt(JulianTime.toUtc(lastSeenJd));
            event.setExact(false);
            event.setSign(figure.sign);
            event.setPlanets(figure.bodyNames());
            event.with("figure", figure.kind)
                 .with("apex", figure.apex)
                 // Der Zeitraum ist am Fensterrand abgeschnitten: die Figur begann früher bzw. dauert an.
                 .with("clippedStart", clippedStart ? true : null)
                 .with("clippedEnd", clippedEnd ? true : null);
            return event;
        }
    }
}
