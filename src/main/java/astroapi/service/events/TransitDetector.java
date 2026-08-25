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
import java.util.Map;

/**
 * Exakte Transite laufender Körper zu den Punkten eines Radix — die persönliche Ebene.
 *
 * <p>Läuft nur, wenn ein Radix mitgeschickt wurde. Anders als beim kollektiven Aspekt ist der
 * Zielpunkt hier <b>fest</b>: die Radixlänge ändert sich nicht, gesucht wird allein der Moment, in
 * dem der laufende Körper den Zielwinkel dazu erreicht. Zusätzlich wird das betroffene Radixhaus
 * bestimmt — das ist der Unterschied zwischen „Mars im Quadrat zu Saturn" und „betrifft dein
 * drittes Haus".
 */
public class TransitDetector implements EventDetector {

    /**
     * Hier <b>mit</b> Mond: zum Radix ist der Mondtransit die kurzlebige, aber sehr konkrete
     * Tagesinformation, die den persönlichen Feed trägt.
     */
    private static final Planet[] TRANSITING = {
            Planet.MOON, Planet.SUN, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER,
            Planet.SATURN, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO, Planet.CHIRON
    };

    private static final double MOON_STEP_DAYS = 0.25;
    private static final double DEFAULT_STEP_DAYS = 0.5;
    private static final double NEAR_TARGET_DEGREES = 90.0;

    @Override
    public String name() {
        return "transit";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        List<AstroEventRawDTO> events = new ArrayList<>();
        if (!ctx.hasNatalChart()) return events;

        EphemerisPort eph = ctx.ephemeris();
        NatalChart natal = new NatalChart(ctx.natalChart());

        for (Planet body : TRANSITING) {
            int id = body.getSweConstValue();
            double step = body == Planet.MOON ? MOON_STEP_DAYS : DEFAULT_STEP_DAYS;

            for (Map.Entry<String, Double> point : natal.points().entrySet()) {
                String pointName = point.getKey();
                Double natalLongitude = point.getValue();
                if (natalLongitude == null || Double.isNaN(natalLongitude)) continue;

                for (MajorAspect aspect : MajorAspect.values()) {
                    for (double target : aspect.signedTargets()) {
                        List<Double> exactTimes = CrossingScan.scan(
                                jd -> AngleMath.signedDelta(eph.lon(jd, id) - natalLongitude, target),
                                ctx.jdFrom(), ctx.jdTo(), step, NEAR_TARGET_DEGREES);

                        for (double exactJd : exactTimes) {
                            events.add(buildEvent(eph, natal, body, pointName, natalLongitude, aspect, exactJd));
                        }
                    }
                }
            }
        }
        return events;
    }

    private static AstroEventRawDTO buildEvent(EphemerisPort eph, NatalChart natal, Planet body,
                                               String natalPoint, double natalLongitude,
                                               MajorAspect aspect, double exactJd) {
        double[] state = eph.lonSpeed(exactJd, body.getSweConstValue());
        ZodiacSign transitSign = ZodiacSign.fromDegree(state[0]);

        AstroEventRawDTO event = AstroEventRawDTO.point(
                EventKeys.transit(body.getName(), aspect.getDisplayName(), natalPoint),
                EventType.TRANSIT.name(),
                JulianTime.toUtc(exactJd));

        event.setSign(transitSign.getName());
        event.setOrb(0.0);
        event.setHouse(natal.houseOf(state[0]));
        event.withPlanets(body.getName(), natalPoint)
             .with("transitingBody", body.getName())
             .with("natalPoint", natalPoint)
             .with("aspect", aspect.getDisplayName())
             .with("angle", aspect.getAngle())
             .with("transitLongitude", round(state[0]))
             .with("natalLongitude", round(natalLongitude))
             .with("transitSign", transitSign.getName())
             .with("natalSign", ZodiacSign.fromDegree(natalLongitude).getName())
             .with("retrograde", state[1] < 0)
             .with("natalHouseOfPoint", natal.houseOf(natalLongitude));
        return event;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
