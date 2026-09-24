package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.Bodies;
import astroapi.utils.CrossingScan;
import astroapi.utils.EphemerisPort;
import astroapi.utils.EventKeys;
import astroapi.utils.EventType;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.ZodiacSign;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exakte Hauptaspekte zwischen zwei laufenden Körpern — die kollektive Wetterlage.
 *
 * <p><b>Ohne Mond.</b> Der Mond bildet rund fünf exakte Aspekte pro Tag; als kollektives Ereignis
 * wäre das eine Flut, die jede Priorisierung erschlägt. Mondaspekte sind ihrer Natur nach
 * persönlich und tagesbezogen — sie erscheinen deshalb im {@link TransitDetector} als Transit zum
 * Radix und im Tageshoroskop, nicht hier.
 */
public class AspectDetector implements EventDetector {

    /** Mond und Mondknoten bleiben draußen — siehe Klassenkommentar. */
    private static final Set<Planet> EXCLUDED = EnumSet.of(Planet.MOON, Planet.TRUE_NODE, Planet.MEAN_NODE);

    private static List<Planet> bodies(EventScanContext ctx) {
        return ctx.bodies().stream().filter(body -> !EXCLUDED.contains(body)).collect(Collectors.toList());
    }

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

        List<Planet> bodies = bodies(ctx);
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i + 1; j < bodies.size(); j++) {
                Planet a = bodies.get(i);
                Planet b = bodies.get(j);

                for (MajorAspect aspect : MajorAspect.values()) {
                    // Ein erweiterter Körper darf kollektiv nur einen persönlichen Planeten
                    // berühren; Ceres gegen Neptun stünde monatelang und wäre keine Nachricht.
                    if (!Bodies.aspectAllowed(a.getName(), b.getName(), aspect.getDisplayName(), 0.0)) {
                        continue;
                    }
                    for (double target : aspect.signedTargets()) {
                        // Die Rasterweite richtet sich nach dem SCHNELLEREN der beiden: er
                        // bestimmt, wie rasch sich der Winkel ändert. Zwei erweiterte Körper
                        // kommen hier ohnehin nicht vor, die Paarung ist also stets Planet
                        // gegen Planet oder Planet gegen Asteroid — und damit bleibt es beim
                        // halben Tag.
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
