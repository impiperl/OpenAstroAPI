package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.CrossingScan;
import astroapi.utils.EphemerisPort;
import astroapi.utils.EventType;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;

import java.util.ArrayList;
import java.util.List;

/**
 * Tagundnachtgleichen und Sonnenwenden — der Durchgang der Sonne durch 0°, 90°, 180° und 270°.
 *
 * <p>Die Keys sind nach <b>Monaten</b> benannt, nicht nach Jahreszeiten: „Frühlingsanfang" wäre auf
 * der Südhalbkugel schlicht falsch. Wie das Ereignis benannt wird, entscheidet die Textebene in
 * gAIster, nicht die Rechnung.
 */
public class SolarPointDetector implements EventDetector {

    private static final double[] TARGET_LONGITUDES = {0.0, 90.0, 180.0, 270.0};
    private static final String[] KEYS = {
            "equinox_march", "solstice_june", "equinox_september", "solstice_december"
    };
    private static final String[] KINDS = {"EQUINOX", "SOLSTICE", "EQUINOX", "SOLSTICE"};

    /** Die Sonne läuft knapp 1°/Tag; ein Tagesraster klammert jeden Durchgang sicher ein. */
    private static final double STEP_DAYS = 1.0;

    /** Schutz gegen den Sprung von {@code signedDelta} am Gegenpunkt. */
    private static final double NEAR_TARGET_DEGREES = 90.0;

    @Override
    public String name() {
        return "solarPoint";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        List<AstroEventRawDTO> events = new ArrayList<>();
        EphemerisPort eph = ctx.ephemeris();
        int sunId = Planet.SUN.getSweConstValue();

        for (int i = 0; i < TARGET_LONGITUDES.length; i++) {
            double target = TARGET_LONGITUDES[i];

            List<Double> exactTimes = CrossingScan.scan(
                    jd -> AngleMath.signedDelta(eph.lon(jd, sunId), target),
                    ctx.jdFrom(), ctx.jdTo(), STEP_DAYS, NEAR_TARGET_DEGREES);

            for (double exactJd : exactTimes) {
                AstroEventRawDTO event = AstroEventRawDTO.point(
                        KEYS[i], EventType.SOLAR_POINT.name(), JulianTime.toUtc(exactJd));
                event.withPlanets(Planet.SUN.getName())
                     .with("kind", KINDS[i])
                     .with("sunLongitude", target);
                events.add(event);
            }
        }
        return events;
    }
}
