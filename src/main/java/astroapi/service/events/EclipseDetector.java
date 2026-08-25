package astroapi.service.events;

import astroapi.dto.AstroEventRawDTO;
import astroapi.utils.AngleMath;
import astroapi.utils.EphemerisPort;
import astroapi.utils.EventType;
import astroapi.utils.JulianTime;
import astroapi.utils.Planet;
import astroapi.utils.ScanCancelledException;
import astroapi.utils.ZodiacSign;
import swisseph.SweConst;

import java.util.ArrayList;
import java.util.List;

/**
 * Sonnen- und Mondfinsternisse im Zeitraum.
 *
 * <p>Anders als die übrigen Detektoren wird hier nicht abgetastet: die Swiss Ephemeris liefert den
 * nächsten Finsternistermin direkt. Vom Beginn des Zeitraums aus wird so lange weitergesprungen,
 * bis der nächste Termin hinter dem Ende liegt — bei einem 30-Tage-Fenster sind das meist null oder
 * ein Aufruf je Art.
 */
public class EclipseDetector implements EventDetector {

    /** Sicherung gegen eine Endlosschleife, falls die Bibliothek keinen Fortschritt meldet. */
    private static final int MAX_STEPS = 24;

    @Override
    public String name() {
        return "eclipse";
    }

    @Override
    public List<AstroEventRawDTO> detect(EventScanContext ctx) {
        List<AstroEventRawDTO> events = new ArrayList<>();
        collect(ctx, true, events);
        collect(ctx, false, events);
        return events;
    }

    private void collect(EventScanContext ctx, boolean lunar, List<AstroEventRawDTO> target) {
        EphemerisPort eph = ctx.ephemeris();
        double cursor = ctx.jdFrom();

        for (int step = 0; step < MAX_STEPS; step++) {
            ScanCancelledException.throwIfInterrupted("eclipse search");
            double[] result = lunar ? eph.nextLunarEclipse(cursor) : eph.nextSolarEclipse(cursor);
            if (result == null || Double.isNaN(result[0])) return;

            double maximumJd = result[0];
            if (maximumJd > ctx.jdTo()) return;

            if (ctx.contains(maximumJd)) {
                target.add(buildEvent(eph, lunar, maximumJd, (int) result[1]));
            }

            // Eine Stunde weiter, damit derselbe Termin nicht erneut gefunden wird.
            double next = maximumJd + 1.0 / 24.0;
            if (next <= cursor) return;
            cursor = next;
        }
    }

    private static AstroEventRawDTO buildEvent(EphemerisPort eph, boolean lunar, double maximumJd, int flags) {
        String kind = lunar ? lunarType(flags) : solarType(flags);
        String key = "eclipse_" + (lunar ? "lunar_" : "solar_") + kind.toLowerCase();

        // Bei einer Sonnenfinsternis stehen Sonne und Mond zusammen, bei einer Mondfinsternis
        // gegenüber — für die Zeichenangabe zählt der verfinsterte Körper.
        int bodyId = lunar ? Planet.MOON.getSweConstValue() : Planet.SUN.getSweConstValue();
        double longitude = eph.lon(maximumJd, bodyId);
        ZodiacSign sign = ZodiacSign.fromDegree(longitude);

        AstroEventRawDTO event = AstroEventRawDTO.point(key, EventType.ECLIPSE.name(), JulianTime.toUtc(maximumJd));
        event.setSign(sign.getName());
        event.withPlanets(Planet.SUN.getName(), Planet.MOON.getName())
             .with("eclipseKind", lunar ? "LUNAR" : "SOLAR")
             .with("eclipseType", kind)
             .with("longitude", Math.round(longitude * 10000.0) / 10000.0)
             .with("degreeInSign", Math.round(AngleMath.degreeInSign(longitude) * 10000.0) / 10000.0);
        return event;
    }

    private static String lunarType(int flags) {
        if ((flags & SweConst.SE_ECL_TOTAL) != 0)     return "TOTAL";
        if ((flags & SweConst.SE_ECL_PARTIAL) != 0)   return "PARTIAL";
        if ((flags & SweConst.SE_ECL_PENUMBRAL) != 0) return "PENUMBRAL";
        return "UNKNOWN";
    }

    private static String solarType(int flags) {
        if ((flags & SweConst.SE_ECL_TOTAL) != 0)      return "TOTAL";
        if ((flags & SweConst.SE_ECL_ANNULAR) != 0)    return "ANNULAR";
        if ((flags & SweConst.SE_ECL_ANNULAR_TOTAL) != 0) return "HYBRID";
        if ((flags & SweConst.SE_ECL_PARTIAL) != 0)    return "PARTIAL";
        return "UNKNOWN";
    }
}
