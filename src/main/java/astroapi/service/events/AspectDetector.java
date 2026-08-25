package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.CrossingScan;
import astroapi.utils.EphemerisPort;
import astroapi.utils.EventKeys;
import astroapi.utils.EventType;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.ZodiacSign;

import java.util.ArrayList;
import java.util.List;

/**
 * Exakte Hauptaspekte zwischen zwei laufenden Körpern — die kollektive Wetterlage.
 *
 * <p><b>Ohne Mond.</b> Der Mond bildet rund fünf exakte Aspekte pro Tag; als kollektives Ereignis
 * wäre das eine Flut, die jede Priorisierung erschlägt. Mondaspekte sind ihrer Natur nach
 * persönlich und tagesbezogen — sie erscheinen deshalb im {@link TransitDetector} als Transit zum
 * Radix und im Tageshoroskop, nicht hier.
 */
public class AspectDetector implements EventDetector {

    private static final Planet[] BODIES = {
            Planet.SUN, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER,
            Planet.SATURN, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO, Planet.CHIRON
    };

    /**
     * Die schnellste hier vorkommende Relativbewegung ist Merkur gegen einen langsamen Planeten mit
     * rund 2°/Tag. Ein halber Tag hält den Raster deutlich unter jedem Aspektdurchgang.
     */
    private static final double STEP_DAYS = 0.5;

    /** Schutz gegen den Sprung von {@code signedDelta} am Gegenpunkt. */
    private static final double NEAR_TARGET_DEGREES = 90.0;

    @Override
    public String name() {
        return "aspect";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        List<AstroEventRawDTO> events = new ArrayList<>();
        EphemerisPort eph = ctx.ephemeris();

        for (int i = 0; i < BODIES.length; i++) {
            for (int j = i + 1; j < BODIES.length; j++) {
                Planet a = BODIES[i];
                Planet b = BODIES[j];

                for (MajorAspect aspect : MajorAspect.values()) {
                    for (double target : aspect.signedTargets()) {
                        List<Double> exactTimes = CrossingScan.scan(
                                jd -> AngleMath.signedDelta(
                                        eph.lon(jd, a.getSweConstValue()) - eph.lon(jd, b.getSweConstValue()),
                                        target),
                                ctx.jdFrom(), ctx.jdTo(), STEP_DAYS, NEAR_TARGET_DEGREES);

                        for (double exactJd : exactTimes) {
                            events.add(buildEvent(eph, a, b, aspect, exactJd));
                        }
                    }
                }
            }
        }
        return events;
    }

    private static AstroEventRawDTO buildEvent(EphemerisPort eph, Planet a, Planet b,
                                               MajorAspect aspect, double exactJd) {
        double[] aState = eph.lonSpeed(exactJd, a.getSweConstValue());
        double[] bState = eph.lonSpeed(exactJd, b.getSweConstValue());

        AstroEventRawDTO event = AstroEventRawDTO.point(
                EventKeys.aspect(a.getName(), aspect.getDisplayName(), b.getName()),
                EventType.ASPECT.name(),
                JulianTime.toUtc(exactJd));

        event.setSign(ZodiacSign.fromDegree(aState[0]).getName());
        event.setOrb(0.0); // exakter Durchgang — die Abweichung ist per Definition null
        event.withPlanets(a.getName(), b.getName())
             .with("aspect", aspect.getDisplayName())
             .with("angle", aspect.getAngle())
             .with("firstLongitude", round(aState[0]))
             .with("secondLongitude", round(bState[0]))
             .with("firstSign", ZodiacSign.fromDegree(aState[0]).getName())
             .with("secondSign", ZodiacSign.fromDegree(bState[0]).getName())
             .with("firstRetrograde", aState[1] < 0)
             .with("secondRetrograde", bState[1] < 0);
        return event;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
