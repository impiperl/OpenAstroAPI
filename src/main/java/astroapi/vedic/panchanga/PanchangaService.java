package astroapi.vedic.panchanga;

import astroapi.service.TimeZoneService;
import astroapi.vedic.Ayanamsa;
import astroapi.vedic.VedicEphemerisService;
import astroapi.vedic.VedicMath;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;

/**
 * Das Panchanga eines Tages an einem Ort — mit Wechselzeiten.
 *
 * <h2>Der Tag beginnt mit dem Sonnenaufgang</h2>
 * Nicht um Mitternacht. Das ist die folgenreichste Festlegung dieses Dienstes: Wer um 03:00 Uhr
 * fragt, steht noch im vedischen Tag von gestern, und der Vara ist der von gestern. Rahu Kala und
 * die meisten Muhurta-Ausschlüsse hängen am Vara — eine um einen Tag verschobene Zuordnung
 * verschiebt sie alle mit.
 *
 * <h2>Ortsabhängig</h2>
 * Der Sonnenaufgang hängt am Ort, also auch der ganze Tag. Dasselbe Datum ergibt in Hamburg und in
 * Chennai verschiedene Panchangas. Ein Zwischenspeicher über dieses Ergebnis braucht deshalb den
 * Ort im Schlüssel, nicht nur das Datum.
 *
 * <h2>Was „Wechselzeit" heißt</h2>
 * Jedes Glied gilt, bis der zugehörige Winkel die nächste Grenze überschreitet. Weil der Mond
 * ungleichmäßig läuft, lässt sich dieser Zeitpunkt nicht aus dem Fortschritt hochrechnen — er wird
 * gesucht. Gelegentlich findet die Suche nichts: In Polnähe gibt es tagelang keinen Sonnenaufgang.
 * Dann bleibt das Feld leer, statt dass eine Zeit erfunden wird.
 */
@Service
public class PanchangaService {

    /**
     * Wie weit vorausgesucht wird. Ein Glied dauert knapp einen Tag; zwei Tage lassen Luft für
     * den langsamsten Fall, ohne die Suche unnötig zu verlängern.
     */
    private static final double SEARCH_DAYS = 2.0;

    private final VedicEphemerisService ephemeris;
    private final TimeZoneService timeZones;

    public PanchangaService(VedicEphemerisService ephemeris, TimeZoneService timeZones) {
        this.ephemeris = ephemeris;
        this.timeZones = timeZones;
    }

    /**
     * Das Panchanga zu einem Zeitpunkt an einem Ort.
     *
     * @param localDateTime Ortszeit; die Zeitzone wird aus den Koordinaten bestimmt
     */
    public PanchangaDay at(double latitude, double longitude, LocalDateTime localDateTime) {
        return at(latitude, longitude, toInstant(localDateTime, resolveZone(latitude, longitude)));
    }

    /**
     * Dasselbe für einen eindeutig bezeichneten Augenblick.
     *
     * <p>Das ist der ehrlichere Einstieg: Ein Panchanga beschreibt einen Augenblick, und eine
     * Wanduhrzeit bezeichnet an zwei Tagen im Jahr zwei davon oder keinen.
     */
    public PanchangaDay at(double latitude, double longitude, java.time.Instant instant) {
        ZoneId zone = resolveZone(latitude, longitude);
        double julianDay = ephemeris.julianDay(
                java.time.LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));

        OptionalDouble sunriseJd = ephemeris.lastSunriseBefore(julianDay, latitude, longitude, Ayanamsa.LAHIRI);
        // Ohne Sonnenaufgang kein vedischer Tag — der Vara fiele auf das Kalenderdatum zurück und
        // wäre damit in der Hälfte der Fälle falsch. Lieber offen sagen, dass es hier nicht geht.
        if (sunriseJd.isEmpty()) {
            throw new astroapi.exeptions.NoSunriseException(latitude, longitude);
        }
        ZonedDateTime sunrise = toZoned(sunriseJd.getAsDouble(), zone);
        LocalDate sunriseDate = sunrise.toLocalDate();

        double[] sunMoon = ephemeris.siderealSunAndMoon(julianDay, Ayanamsa.LAHIRI);
        double sun = sunMoon[0];
        double moon = sunMoon[1];
        Panchanga limbs = PanchangaCalculator.at(sun, moon, sunriseDate);

        return new PanchangaDay(
                limbs,
                sunrise,
                endOf(julianDay, elongationOf(), boundaryAfter(
                        PanchangaCalculator.elongation(sun, moon), PanchangaCalculator.TITHI_SPAN), zone),
                endOf(julianDay, elongationOf(), boundaryAfter(
                        PanchangaCalculator.elongation(sun, moon), PanchangaCalculator.KARANA_SPAN), zone),
                endOf(julianDay, sumOf(), boundaryAfter(
                        VedicMath.normalizeDegrees(sun + moon), PanchangaCalculator.YOGA_SPAN), zone),
                endOf(julianDay, moonOnly(), boundaryAfter(
                        VedicMath.normalizeDegrees(moon), VedicMath.NAKSHATRA_SPAN), zone));
    }

    /**
     * Panchanga und Antwortform in einem Schritt — was der Endpunkt braucht.
     */
    public astroapi.vedic.dto.PanchangaResponseDTO describe(double latitude, double longitude,
                                                            LocalDateTime localDateTime) {
        return describe(latitude, longitude, toInstant(localDateTime, resolveZone(latitude, longitude)));
    }

    /** Panchanga und Antwortform für einen eindeutigen Augenblick. */
    public astroapi.vedic.dto.PanchangaResponseDTO describe(double latitude, double longitude,
                                                            java.time.Instant instant) {
        ZoneId zone = resolveZone(latitude, longitude);
        return toDto(at(latitude, longitude, instant), instant.atZone(zone));
    }

    /**
     * Wanduhrzeit zu Augenblick — mit <b>festgelegtem</b> Verhalten an den DST-Grenzen.
     *
     * <p>{@code atZone} wählt bei der Herbst-Doppelstunde den früheren Offset und schiebt eine
     * Frühjahrs-Lücke nach vorn. Das ist Javas Standard; hier steht es als bewusste Festlegung,
     * damit es nicht für Zufall gehalten wird. Wer das nicht will, schickt {@code instant}.
     */
    private static java.time.Instant toInstant(LocalDateTime localDateTime, ZoneId zone) {
        return localDateTime.atZone(zone).toInstant();
    }

    private ZoneId resolveZone(double latitude, double longitude) {
        return timeZones.getTimeZone(latitude, longitude)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No time zone found for the provided coordinates."));
    }

    /**
     * Die Antwortform des Endpunkts.
     *
     * <p>Die Umwandlung liegt hier und nicht im Controller, damit die eine Stelle, an der ein
     * Glied zu Nummer, Schlüssel und Name wird, auch die ist, die die Werte berechnet hat.
     */
    public astroapi.vedic.dto.PanchangaResponseDTO toDto(PanchangaDay day, java.time.ZonedDateTime moment) {
        Panchanga l = day.limbs();
        astroapi.vedic.dto.PanchangaResponseDTO dto = new astroapi.vedic.dto.PanchangaResponseDTO();

        dto.setCalculationSystem(astroapi.vedic.VedicRuleSet.CALCULATION_SYSTEM);
        dto.setCalculationVersion(astroapi.vedic.VedicRuleSet.CALCULATION_VERSION);
        dto.setAyanamsa(Ayanamsa.LAHIRI.getKey());
        dto.setPanchangaRuleSet(astroapi.vedic.VedicRuleSet.PANCHANGA_RULE_SET);
        dto.setMoment(format(moment));
        dto.setVedicDayStart(format(day.sunrise()));

        dto.setTithi(new astroapi.vedic.dto.PanchangaLimbDTO(
                l.tithiNumber(), l.tithiKey(), l.tithiDisplayName(), l.tithiFraction(), format(day.tithiEnd())));
        dto.setPaksha(l.paksha().getKey());

        dto.setKarana(new astroapi.vedic.dto.PanchangaLimbDTO(
                l.karanaNumber(), l.karana().getKey(), l.karana().getDisplayName(),
                l.karanaFraction(), format(day.karanaEnd())));
        dto.setKaranaVishti(l.karana() == Karana.VISHTI);

        dto.setYoga(new astroapi.vedic.dto.PanchangaLimbDTO(
                l.yogaNumber(), l.yoga().getKey(), l.yoga().getDisplayName(),
                l.yogaFraction(), format(day.yogaEnd())));

        dto.setNakshatra(new astroapi.vedic.dto.PanchangaLimbDTO(
                l.nakshatra().getIndex(), l.nakshatra().getKey(), l.nakshatra().getDisplay(),
                l.nakshatraFraction(), format(day.nakshatraEnd())));
        dto.setPada(l.pada());

        // Zwei Dinge zum Vara:
        //
        // Er zählt hier ab 1, obwohl das Enum intern ab 0 zählt (dort ist 0 der Sonntag, wie in
        // java.time). In EINER Antwortform darf nicht ein Feld ab 0 und der Rest ab 1 zählen —
        // das ist eine Falle für jeden Aufrufer, der die Zahlen gleich behandelt.
        //
        // Und er kommt ohne Endzeit: Er endet mit dem nächsten Sonnenaufgang. Den gesondert zu
        // suchen wäre ein weiterer Ephemeriden-Lauf für eine Zahl, die die Oberfläche auch aus
        // dem Beginn des Folgetages bekommt.
        dto.setVara(new astroapi.vedic.dto.PanchangaLimbDTO(
                l.vara().getIndex() + 1, l.vara().getKey(), l.vara().getDisplayName(), 0.0, null));
        dto.setVaraRuler(l.vara().getRuler().getKey());

        return dto;
    }

    private static String format(java.time.ZonedDateTime time) {
        return time == null ? null : time.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    // ── Die vier beobachteten Winkel ─────────────────────────────────────────
    //
    // Als Funktionen, damit die Suche eine einzige Routine bleibt. Welcher Winkel ein Glied
    // trägt, steht damit genau an einer Stelle — und die Unterscheidung Differenz/Summe, an der
    // sich Panchanga-Rechner verzählen, ist hier sichtbar statt in vier Kopien verstreut.

    private static java.util.function.DoubleBinaryOperator elongationOf() {
        return PanchangaCalculator::elongation;
    }

    private static java.util.function.DoubleBinaryOperator sumOf() {
        return (sun, moon) -> VedicMath.normalizeDegrees(sun + moon);
    }

    private static java.util.function.DoubleBinaryOperator moonOnly() {
        return (sun, moon) -> VedicMath.normalizeDegrees(moon);
    }

    /** Die nächste Abschnittsgrenze oberhalb des aktuellen Winkels. */
    private static double boundaryAfter(double currentAngle, double span) {
        return VedicMath.normalizeDegrees((Math.floor(currentAngle / span) + 1) * span);
    }

    private ZonedDateTime endOf(double fromJd, java.util.function.DoubleBinaryOperator angleOf,
                                double boundary, ZoneId zone) {
        OptionalDouble crossing = ephemeris.nextAngleCrossing(
                fromJd, SEARCH_DAYS, angleOf, boundary, Ayanamsa.LAHIRI);
        return crossing.isPresent() ? toZoned(crossing.getAsDouble(), zone) : null;
    }

    /**
     * Julianischer Tag (UT) zu einer Ortszeit.
     *
     * <p>Die Rückrechnung geht über ganze Millisekunden: Ein Julianischer Tag trägt mehr
     * Genauigkeit, als eine angezeigte Uhrzeit braucht, und Sekundenbruchteile in einer
     * Wechselzeit wären vorgetäuschte Präzision.
     */
    private ZonedDateTime toZoned(double julianDay, ZoneId zone) {
        // Unix-Epoche: JD 2440587.5 = 1970-01-01T00:00Z
        long epochMillis = Math.round((julianDay - 2440587.5) * 86_400_000.0);
        return java.time.Instant.ofEpochMilli(epochMillis).atZone(zone);
    }
}
