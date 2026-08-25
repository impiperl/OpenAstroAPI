package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.CrossingScan;
import astroapi.utils.EphemerisPort;
import astroapi.utils.EventType;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.ZodiacSign;

import java.util.ArrayList;
import java.util.List;

/**
 * Neumond, erstes Viertel, Vollmond und letztes Viertel mit exaktem Zeitpunkt.
 *
 * <p>Die Phase ist definiert über die Elongation Mond − Sonne. Gesucht ist jeweils der Moment, in
 * dem sie einen der vier Zielwinkel durchläuft.
 */
public class MoonPhaseDetector implements EventDetector {

    /** Zielwinkel der vier Hauptphasen mit ihrem stabilen Key. */
    private static final double[] TARGET_ANGLES = {0.0, 90.0, 180.0, 270.0};
    private static final String[] TARGET_KEYS = {
            "moon_phase_new", "moon_phase_first_quarter", "moon_phase_full", "moon_phase_last_quarter"
    };

    /**
     * Abtastschritt. Die Elongation wandert rund 12,2°/Tag, bei 6 h sind das gut 3° pro Schritt —
     * eng genug, dass jeder Durchgang sauber eingeklammert wird.
     */
    private static final double STEP_DAYS = 0.25;

    /**
     * Nur Vorzeichenwechsel nahe am Zielwinkel zählen. {@code signedDelta} springt am Gegenpunkt
     * von +180 auf −180 — ohne diese Schranke würde jeder Durchgang durch den Gegenpunkt als
     * Nullstelle missdeutet.
     */
    private static final double NEAR_TARGET_DEGREES = 90.0;

    @Override
    public String name() {
        return "moonPhase";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        List<AstroEventRawDTO> events = new ArrayList<>();
        EphemerisPort eph = ctx.ephemeris();

        for (int i = 0; i < TARGET_ANGLES.length; i++) {
            double target = TARGET_ANGLES[i];
            String key = TARGET_KEYS[i];

            List<Double> exactTimes = CrossingScan.scan(
                    jd -> AngleMath.signedDelta(elongation(eph, jd), target),
                    ctx.jdFrom(), ctx.jdTo(), STEP_DAYS, NEAR_TARGET_DEGREES);

            for (double exactJd : exactTimes) {
                events.add(buildEvent(eph, key, exactJd));
            }
        }
        return events;
    }

    private static double elongation(EphemerisPort eph, double jd) {
        double moon = eph.lon(jd, Planet.MOON.getSweConstValue());
        double sun = eph.lon(jd, Planet.SUN.getSweConstValue());
        if (Double.isNaN(moon) || Double.isNaN(sun)) return Double.NaN;
        return AngleMath.norm360(moon - sun);
    }

    private static AstroEventRawDTO buildEvent(EphemerisPort eph, String key, double exactJd) {
        double moonLon = eph.lon(exactJd, Planet.MOON.getSweConstValue());
        double sunLon = eph.lon(exactJd, Planet.SUN.getSweConstValue());
        ZodiacSign moonSign = ZodiacSign.fromDegree(moonLon);

        AstroEventRawDTO event = AstroEventRawDTO.point(key, EventType.MOON_PHASE.name(), JulianTime.toUtc(exactJd));
        event.setSign(moonSign.getName());
        event.withPlanets(Planet.MOON.getName(), Planet.SUN.getName())
             .with("moonLongitude", round(moonLon))
             .with("sunLongitude", round(sunLon))
             .with("moonSign", moonSign.getName())
             .with("sunSign", ZodiacSign.fromDegree(sunLon).getName())
             .with("degreeInSign", round(AngleMath.degreeInSign(moonLon)));
        return event;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
