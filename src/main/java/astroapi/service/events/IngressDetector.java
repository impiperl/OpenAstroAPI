package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.CrossingFinder;
import astroapi.utils.EphemerisPort;
import astroapi.utils.EventKeys;
import astroapi.utils.EventType;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.ScanCancelledException;
import astroapi.utils.ZodiacSign;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeichenwechsel eines Körpers, mit exakter Übertrittszeit.
 *
 * <p>Rückläufige Rückkehrer sind ausdrücklich mitgemeint: wechselt ein Planet zurück in das
 * vorherige Zeichen, ist auch das ein Ingress. Deshalb wird die Nullstelle auf der jeweils
 * überschrittenen Zeichengrenze gesucht statt „vorwärts" angenommen.
 */
public class IngressDetector implements EventDetector {

    private static final Planet[] BODIES = {
            Planet.MOON, Planet.SUN, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER,
            Planet.SATURN, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO, Planet.CHIRON, Planet.TRUE_NODE
    };

    /**
     * Der Mond läuft rund 13°/Tag und wechselt alle zweieinhalb Tage das Zeichen — er braucht einen
     * feineren Raster als alles andere.
     */
    private static final double MOON_STEP_DAYS = 0.25;
    private static final double DEFAULT_STEP_DAYS = 0.5;

    /**
     * Sonnenübertritte in Widder, Krebs, Waage und Steinbock <b>sind</b> die Tagundnachtgleichen und
     * Sonnenwenden. Sie liefert der {@link SolarPointDetector} — hier würden sie nur denselben
     * Zeitpunkt ein zweites Mal unter anderem Namen erzeugen.
     */
    private static final int[] SOLAR_POINT_SIGNS = {0, 3, 6, 9};

    @Override
    public String name() {
        return "ingress";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        List<AstroEventRawDTO> events = new ArrayList<>();
        EphemerisPort eph = ctx.ephemeris();

        for (Planet body : BODIES) {
            int id = body.getSweConstValue();
            double step = body == Planet.MOON ? MOON_STEP_DAYS : DEFAULT_STEP_DAYS;

            double previousJd = ctx.jdFrom();
            double previousLon = eph.lon(previousJd, id);

            for (double jd = ctx.jdFrom() + step; jd <= ctx.jdTo() + step; jd += step) {
                ScanCancelledException.throwIfInterrupted("ingress scan");

                double current = Math.min(jd, ctx.jdTo());
                double currentLon = eph.lon(current, id);

                if (!Double.isNaN(previousLon) && !Double.isNaN(currentLon)) {
                    int previousSign = AngleMath.signIndex(previousLon);
                    int currentSign = AngleMath.signIndex(currentLon);

                    if (previousSign != currentSign) {
                        double boundary = crossedBoundary(previousLon, currentLon);
                        java.util.function.DoubleUnaryOperator f =
                                t -> AngleMath.signedDelta(eph.lon(t, id), boundary);

                        double exactJd = CrossingFinder.findRoot(f, previousJd, current, CrossingFinder.ONE_MINUTE);
                        if (ctx.contains(exactJd)) {
                            ZodiacSign entered = ZodiacSign.fromDegree(currentLon);
                            boolean isSolarPoint = body == Planet.SUN && isSolarPointSign(currentSign);
                            if (!isSolarPoint) {
                                boolean forward = AngleMath.signedDelta(currentLon, previousLon) > 0;
                                events.add(buildEvent(body, entered, previousLon, exactJd, forward));
                            }
                        }
                    }
                }

                previousJd = current;
                previousLon = currentLon;
                if (current >= ctx.jdTo()) break;
            }
        }
        return events;
    }

    /**
     * Die tatsächlich überschrittene Zeichengrenze.
     *
     * <p>Vorwärts ist es der Anfang des neuen Zeichens, rückläufig der Anfang des alten. Ohne diese
     * Unterscheidung würde die Nullstellensuche bei einer Rückkehr auf der falschen Grenze suchen
     * und nichts finden.
     */
    private static double crossedBoundary(double fromLon, double toLon) {
        boolean forward = AngleMath.signedDelta(toLon, fromLon) > 0;
        return forward ? AngleMath.signStart(toLon) : AngleMath.signStart(fromLon);
    }

    private static boolean isSolarPointSign(int signIndex) {
        for (int s : SOLAR_POINT_SIGNS) if (s == signIndex) return true;
        return false;
    }

    private static AstroEventRawDTO buildEvent(Planet body, ZodiacSign entered, double previousLon,
                                               double exactJd, boolean forward) {
        AstroEventRawDTO event = AstroEventRawDTO.point(
                EventKeys.ingress(body.getName(), entered.getName()),
                EventType.INGRESS.name(),
                JulianTime.toUtc(exactJd));
        event.setSign(entered.getName());
        event.withPlanets(body.getName())
             .with("enters", entered.getName())
             .with("leaves", ZodiacSign.fromDegree(previousLon).getName())
             .with("direction", forward ? "DIRECT" : "RETROGRADE");
        return event;
    }
}
