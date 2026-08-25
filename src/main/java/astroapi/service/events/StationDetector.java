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
 * Stationen: der Moment, in dem ein Planet rückläufig oder wieder direktläufig wird.
 *
 * <p>Erkannt wird das am <b>Vorzeichenwechsel der Längengeschwindigkeit</b>, nicht an einem
 * Vorher-Nachher-Vergleich zweier Tagespositionen. Das ist der Unterschied zwischen „irgendwann
 * gestern oder heute" und einem exakten Zeitpunkt.
 */
public class StationDetector implements EventDetector {

    /** Sonne und Mond laufen nie rückläufig; die Mondknoten sind es fast immer — beides wäre Rauschen. */
    private static final Planet[] BODIES = {
            Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN,
            Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO, Planet.CHIRON
    };

    /** Ein halber Tag reicht: selbst Merkur wechselt sein Vorzeichen nicht zweimal darin. */
    private static final double STEP_DAYS = 0.5;

    @Override
    public String name() {
        return "station";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        List<AstroEventRawDTO> events = new ArrayList<>();
        EphemerisPort eph = ctx.ephemeris();

        for (Planet body : BODIES) {
            int id = body.getSweConstValue();
            java.util.function.DoubleUnaryOperator speed = jd -> eph.lonSpeed(jd, id)[1];

            for (double exactJd : CrossingScan.scan(speed, ctx.jdFrom(), ctx.jdTo(), STEP_DAYS)) {
                // Kurz vor der Station zeigt das Vorzeichen der Geschwindigkeit, wohin es geht:
                // vorwärts vor der Nullstelle → der Planet wird rückläufig, und umgekehrt.
                double speedBefore = speed.applyAsDouble(exactJd - STEP_DAYS);
                boolean turnsRetrograde = speedBefore > 0;
                events.add(buildEvent(eph, body, exactJd, turnsRetrograde));
            }
        }
        return events;
    }

    private static AstroEventRawDTO buildEvent(EphemerisPort eph, Planet body, double exactJd, boolean retrograde) {
        double longitude = eph.lon(exactJd, body.getSweConstValue());
        ZodiacSign sign = ZodiacSign.fromDegree(longitude);

        AstroEventRawDTO event = AstroEventRawDTO.point(
                EventKeys.station(body.getName(), retrograde),
                EventType.STATION.name(),
                JulianTime.toUtc(exactJd));
        event.setSign(sign.getName());
        event.withPlanets(body.getName())
             .with("direction", retrograde ? "RETROGRADE" : "DIRECT")
             .with("longitude", Math.round(longitude * 10000.0) / 10000.0)
             .with("degreeInSign", Math.round(AngleMath.degreeInSign(longitude) * 10000.0) / 10000.0);
        return event;
    }
}
