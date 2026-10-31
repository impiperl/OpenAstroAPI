package astroapi.vedic;

import astroapi.service.SwissEphemerisExecutor;
import astroapi.service.TimeZoneService;
import astroapi.vedic.dto.NakshatraPositionDTO;
import astroapi.vedic.dto.VedicChartDTO;
import astroapi.vedic.dto.VedicHouseDTO;
import astroapi.vedic.dto.VedicPlanetDTO;
import astroapi.vedic.dto.VedicPersonRequest;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import swisseph.SweConst;
import swisseph.SweDate;
import swisseph.SwissEph;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Siderische Chartberechnung — die einzige Stelle, an der die vedische Strecke die Ephemeride
 * anfasst.
 *
 * <h2>Warum ein zweiter Instanzpool</h2>
 * Die siderische Betriebsart ist Zustand der {@link SwissEph}-Instanz, nicht ein Parameter des
 * Aufrufs: {@code swe_set_sid_mode} schreibt in {@code swed.sidd.sid_mode}. Dieser Dienst hält
 * deshalb einen <b>eigenen</b> {@link SwissEphemerisExecutor}, dessen Worker-Threads je eine
 * Instanz besitzen, auf der der Modus einmal beim Bau gesetzt wird. Die tropische Strecke
 * ({@code SwissEphemerisHelper}) benutzt ihren eigenen Pool und sieht diese Instanzen nie. Eine
 * Vermischung ist damit nicht unwahrscheinlich, sondern unmöglich — und das ist der einzige
 * Zustand, bei dem man das über Jahre glauben kann.
 *
 * <p>Weil der Modus je Instanz gilt, wird er <b>zusätzlich</b> am Anfang jeder Aufgabe gesetzt.
 * Das kostet nichts und deckt den Fall ab, dass eine Instanz je für eine andere Ayanamsha benutzt
 * wird.
 *
 * <h2>Die angewandte Ayanamsha</h2>
 * {@code swe_get_ayanamsa_ut} und das, was {@code swe_calc_ut} mit {@code SEFLG_SIDEREAL}
 * tatsächlich abzieht, sind <b>nicht</b> derselbe Wert — gemessen am 1.1.2000 liegen 13,9″
 * dazwischen (23°51′25,3″ gegenüber 23°51′11,4″; der zweite ist der publizierte Lahiri-Wert).
 * Deshalb wird der angewandte Wert hier aus der Rechnung selbst gewonnen (tropische minus
 * siderische Sonnenlänge) und ausgegeben. Der Aszendent kommt aus
 * {@code swe_houses} mit {@code SEFLG_SIDEREAL}, also aus derselben Korrektur wie die Planeten —
 * sonst lägen Achse und Planeten um jene Bogensekunden in verschiedenen Tierkreisen.
 *
 * <h2>Ein Fallstrick der Java-Portierung</h2>
 * {@code swe_houses} mit {@code SEFLG_SIDEREAL} schreibt in diesem Port über das 13. Element des
 * Cusp-Arrays hinaus und wirft mit einem {@code double[13]} eine
 * {@link ArrayIndexOutOfBoundsException} in {@code SweHouse.sidereal_houses_trad}. Das Array muss
 * {@value #CUSP_ARRAY_SIZE} Elemente haben. Nicht verkleinern.
 */
@Service
public class VedicEphemerisService {

    private static final Logger logger = LoggerFactory.getLogger(VedicEphemerisService.class);

    /**
     * Sonnenaufgang nach hinduistischer Konvention, soweit dieser Port ihn hergibt:
     * Scheibenmittelpunkt ohne Refraktion.
     *
     * <p>Neuere Fassungen von Swiss Ephemeris bündeln das als {@code SE_BIT_HINDU_RISING} und
     * nehmen zusätzlich {@code SE_BIT_GEOCTR_NO_ECL_LAT} hinzu. Beides fehlt hier — siehe
     * {@link #lastSunriseBefore}. Die Konstante steht trotzdem benannt da, damit die Absicht an
     * der Aufrufstelle lesbar ist und ein späterer Bibliotheks-Wechsel genau eine Zeile trifft.
     */
    private static final int HINDU_RISING =
            SweConst.SE_CALC_RISE | SweConst.SE_BIT_DISC_CENTER | SweConst.SE_BIT_NO_REFRACTION;

    /** Pflichtgröße des Cusp-Arrays für {@code swe_houses} — siehe Klassendoku. */
    private static final int CUSP_ARRAY_SIZE = 37;

    /** Whole Sign. Festgelegt, nicht wählbar: die vedische Berechnung kennt kein anderes. */
    private static final char WHOLE_SIGN = 'W';

    private static final int CALC_FLAGS =
            SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED | SweConst.SEFLG_SIDEREAL;
    private static final int CALC_FLAGS_TROPICAL =
            SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED;

    private static final long CALC_TIMEOUT_SECONDS = 5;

    /**
     * Ein grenznaher Mond ist der häufigste Grund, weshalb zwei Rechner verschiedene Ergebnisse
     * liefern: innerhalb dieses Abstands zur Nakshatra-Grenze hängen Nadi, Gana, Yoni und Tara an
     * Minuten der Geburtszeit. Ab hier wird gewarnt.
     */
    public static final double NAKSHATRA_BOUNDARY_WARN_DEGREES = 0.5;

    private final TimeZoneService timeZoneService;
    private final SwissEphemerisExecutor executor;

    /**
     * Chart-Cache. Der Schlüssel enthält <b>Berechnungssystem, Regelversionen, Ayanamsha und
     * Knotentyp</b> — ohne die würde ein Umschalten der Ayanamsha ein altes Ergebnis
     * zurückliefern, das unter anderen Zeichengrenzen entstanden ist.
     */
    private final Cache<String, VedicChartDTO> chartCache;

    public VedicEphemerisService(TimeZoneService timeZoneService,
                                 @Value("${astro.vedic.ephePath:ephe/}") String ephePath,
                                 @Value("${astro.vedic.threads:2}") int threads) {
        this.timeZoneService = timeZoneService;
        // Der Pool ist klein: die vedische Strecke ist ein Zusatzangebot, nicht der Hauptlastpfad,
        // und jede Instanz hält eigene Dateizeiger auf die Ephemeridendateien.
        this.executor = new SwissEphemerisExecutor(ephePath, threads,
                eph -> eph.swe_set_sid_mode(Ayanamsa.LAHIRI.getSidMode(), 0, 0));
        this.chartCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(8, TimeUnit.HOURS)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    /** Nur für Tests: lässt den Cache außen vor, damit eine Messung nicht an einem Treffer hängt. */
    public void clearCache() {
        chartCache.invalidateAll();
    }

    // ── Öffentlich ───────────────────────────────────────────────────────────

    /**
     * Berechnet das vedische Chart einer Person.
     *
     * @param person    Geburtsdaten (lokale Wandzeit + Koordinaten)
     * @param ayanamsa  gewählte Ayanamsha
     * @param nodeType  gewählter Knotentyp
     */
    public VedicChartDTO chartOf(VedicPersonRequest person, Ayanamsa ayanamsa, NodeType nodeType) {
        validate(person);
        ZoneId zone = resolveZone(person.getCoordinates());
        LocalDateTime ut = ZonedDateTime.of(person.getDateTime(), zone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        double julianDay = julianDay(ut);

        String key = cacheKey(julianDay, person.getCoordinates(), ayanamsa, nodeType,
                person.isBirthTimeKnown());
        VedicChartDTO cached = chartCache.get(key, k ->
                compute(person, zone, ut, julianDay, ayanamsa, nodeType));
        // Nur das Label ist eine Eigenschaft der Anfrage und nicht des Charts: zwei Personen mit
        // identischen Geburtsdaten teilen den Cache-Eintrag, aber nicht ihr Label.
        return withRequestFields(cached, person);
    }

    /**
     * Tropische Länge eines Körpers — <b>ausschließlich</b> für den Vergleichstest, der beweist,
     * dass siderische und tropische Rechnung auf demselben Pool nicht ineinanderlaufen. Im
     * Betrieb rechnet die westliche Strecke über ihren eigenen Helper.
     */
    public double tropicalLongitudeForTest(double julianDay, int sweBody) {
        return run(eph -> {
            double[] xx = new double[6];
            StringBuffer err = new StringBuffer();
            int ret = eph.swe_calc_ut(julianDay, sweBody, CALC_FLAGS_TROPICAL, xx, err);
            if (ret == SweConst.ERR) throw new IllegalStateException("swe_calc_ut failed: " + err);
            return xx[0];
        }, Ayanamsa.LAHIRI);
    }

    /**
     * Siderische Längen von Sonne und Mond zu einem Zeitpunkt — die Grundlage des Panchanga.
     *
     * <p>Beide aus <b>einem</b> Aufruf-Paar auf derselben Instanz, damit sie garantiert denselben
     * Ayanamsha-Zustand sehen. Getrennte Aufrufe über den Pool könnten auf verschiedenen
     * {@link SwissEph}-Instanzen landen; das wäre heute harmlos, weil alle Lahiri tragen, aber
     * genau die Art stiller Annahme, die beim nächsten Ayanamsha bricht.
     *
     * @return {@code [Sonne, Mond]} in Grad
     */
    public double[] siderealSunAndMoon(double julianDay, Ayanamsa ayanamsa) {
        return run(eph -> {
            double[] sun = new double[6];
            double[] moon = new double[6];
            StringBuffer err = new StringBuffer();
            if (eph.swe_calc_ut(julianDay, SweConst.SE_SUN, CALC_FLAGS, sun, err) == SweConst.ERR) {
                throw new IllegalStateException("swe_calc_ut (sun) failed: " + err);
            }
            if (eph.swe_calc_ut(julianDay, SweConst.SE_MOON, CALC_FLAGS, moon, err) == SweConst.ERR) {
                throw new IllegalStateException("swe_calc_ut (moon) failed: " + err);
            }
            return new double[]{ sun[0], moon[0] };
        }, ayanamsa);
    }

    /**
     * Der letzte Sonnenaufgang vor einem Zeitpunkt — die Grenze des vedischen Tages.
     *
     * <p>Der vedische Tag läuft von Sonnenaufgang zu Sonnenaufgang. Wer um 03:00 Uhr fragt, steht
     * noch im Tag von gestern; der zugehörige Vara ist der von gestern, und daran hängen Rahu Kala
     * und die Muhurta-Ausschlüsse.
     *
     * <h2>Welcher Sonnenaufgang</h2>
     * Nicht der bürgerliche. Die hinduistische Kalenderrechnung nimmt den <b>Mittelpunkt</b> der
     * Sonnenscheibe <b>ohne Refraktion</b> — Swiss Ephemeris fasst das in neueren Fassungen als
     * {@code SE_BIT_HINDU_RISING} zusammen, was zusätzlich eine geozentrische Betrachtung ohne
     * ekliptikale Breite einschließt. Der bürgerliche Aufgang (oberer Rand, mit Refraktion) liegt
     * rund drei bis fünf Minuten früher; nahe einer Glied-Grenze ergibt das einen anderen Tithi
     * oder Vara am Tagesbeginn und verschiebt damit alle daran hängenden Muhurta-Ausschlüsse.
     *
     * <p><b>Der hier verwendete Java-Port kennt {@code SE_BIT_HINDU_RISING} nicht.</b> Verfügbar
     * sind nur {@code SE_BIT_DISC_CENTER} und {@code SE_BIT_NO_REFRACTION}; der dritte Bestandteil
     * {@code SE_BIT_GEOCTR_NO_ECL_LAT} fehlt. Beide verfügbaren Bits sind gesetzt — das ist die
     * erreichbare Annäherung, nicht die Konvention selbst. Der verbleibende Unterschied liegt im
     * Sekundenbereich, ist aber eine bekannte Abweichung und keine Gleichwertigkeit.
     *
     * <p>Zweite Falle: {@code geopos} erwartet die Reihenfolge <b>Länge, Breite, Höhe</b> — nicht
     * Breite zuerst, wie überall sonst in diesem Dienst.
     *
     * <p>In Polnähe kann es tagelang keinen Aufgang geben. Dann liefert der Port keinen Treffer,
     * und dieser Dienst sagt das offen, statt eine Zeit zu erfinden.
     *
     * @param searchStartJd Zeitpunkt, von dem aus rückwärts gesucht wird
     * @param latitude      geografische Breite in Grad
     * @param longitude     geografische Länge in Grad
     */
    /**
     * Sonnenaufgang mit frei gewähltem Modus — <b>ausschließlich</b> für den Test, der nachweist,
     * dass beide Bits der hinduistischen Konvention tatsächlich wirken.
     *
     * <p>Ein einzelner Vergleich mit dem bürgerlichen Aufgang zeigt nur, dass sich <em>etwas</em>
     * geändert hat: Auch „oberer Rand ohne Refraktion" läge in derselben Größenordnung. Erst der
     * Vergleich aller vier Modi untereinander trennt die Wirkung von {@code SE_BIT_DISC_CENTER}
     * und {@code SE_BIT_NO_REFRACTION} voneinander.
     *
     * <p>Im Betrieb gibt es diese Wahl nicht: Dort gilt {@link #HINDU_RISING}, und eine
     * Umschaltmöglichkeit wäre genau die Art Schalter, die ein Ergebnis unbemerkt verändert.
     */
    public java.util.OptionalDouble sunriseForTest(double searchStartJd, double latitude,
                                                   double longitude, int risingMode) {
        return run(eph -> {
            double[] geopos = { longitude, latitude, 0.0 };
            swisseph.DblObj found = new swisseph.DblObj();
            StringBuffer err = new StringBuffer();
            int ret = eph.swe_rise_trans(searchStartJd - 1.0, SweConst.SE_SUN, null,
                    SweConst.SEFLG_SWIEPH, risingMode, geopos, 0.0, 0.0, found, err);
            return ret != 0 ? java.util.OptionalDouble.empty()
                    : java.util.OptionalDouble.of(found.val);
        }, Ayanamsa.LAHIRI);
    }

    public java.util.OptionalDouble lastSunriseBefore(double searchStartJd, double latitude,
                                                      double longitude, Ayanamsa ayanamsa) {
        return run(eph -> {
            // Zwei Tage zurück reicht überall außerhalb der Polarkreise mit großem Abstand.
            double from = searchStartJd - 2.0;
            double[] geopos = { longitude, latitude, 0.0 };
            double best = Double.NaN;
            double cursor = from;
            while (cursor < searchStartJd) {
                swisseph.DblObj found = new swisseph.DblObj();
                StringBuffer err = new StringBuffer();
                int ret = eph.swe_rise_trans(cursor, SweConst.SE_SUN, null,
                        SweConst.SEFLG_SWIEPH, HINDU_RISING, geopos, 0.0, 0.0, found, err);
                if (ret != 0 || found.val <= cursor || found.val > searchStartJd) break;
                best = found.val;
                cursor = found.val + 1e-4;   // knapp dahinter weitersuchen, sonst Endlosschleife
            }
            return Double.isNaN(best) ? java.util.OptionalDouble.empty()
                    : java.util.OptionalDouble.of(best);
        }, ayanamsa);
    }

    /**
     * Wann überschreitet ein aus Sonne und Mond gebildeter Winkel die nächste Grenze?
     *
     * <p>Das ist die Wechselzeit eines Panchanga-Glieds. Welcher Winkel gemeint ist, bestimmt
     * {@code angleOf}: Mond − Sonne für Tithi und Karana, Mond + Sonne für Yoga, der Mond allein
     * für das Nakshatra. Dadurch braucht es nur eine Suchroutine statt vier.
     *
     * <h2>Warum der ganze Scan in einem Durchlauf liegt</h2>
     * Eine Nullstellensuche wertet die Funktion dutzende Male aus. Liefe jede Auswertung über
     * {@link #run}, wären es ebenso viele Übergaben an den Pool — mit Zeitlimit, Thread-Wechsel
     * und der Möglichkeit, dabei auf einer anderen {@link SwissEph}-Instanz zu landen. Der Scan
     * läuft deshalb vollständig auf einem Thread und einer Instanz.
     *
     * @param boundaryDegrees der Zielwinkel in Grad
     * @param maxDays         wie weit vorausgeschaut wird; ein Glied dauert knapp einen Tag
     * @return der exakte Zeitpunkt, oder leer, wenn im Fenster keiner liegt
     */
    public java.util.OptionalDouble nextAngleCrossing(
            double fromJd, double maxDays,
            java.util.function.DoubleBinaryOperator angleOf,
            double boundaryDegrees, Ayanamsa ayanamsa) {

        return run(eph -> {
            java.util.function.DoubleUnaryOperator distanceToBoundary = jd -> {
                double[] sun = new double[6];
                double[] moon = new double[6];
                StringBuffer err = new StringBuffer();
                if (eph.swe_calc_ut(jd, SweConst.SE_SUN, CALC_FLAGS, sun, err) == SweConst.ERR
                        || eph.swe_calc_ut(jd, SweConst.SE_MOON, CALC_FLAGS, moon, err) == SweConst.ERR) {
                    throw new IllegalStateException("swe_calc_ut failed during crossing scan: " + err);
                }
                return astroapi.utils.AngleMath.signedDelta(
                        angleOf.applyAsDouble(sun[0], moon[0]), boundaryDegrees);
            };

            // Rasterweite 0,05 Tage (72 min): Die schnellste hier gesuchte Größe ist die Summe
            // Sonne + Mond mit rund 14°/Tag, also knapp 0,7° je Schritt — weit genug entfernt
            // davon, eine Grenze zwischen zwei Punkten zu überspringen.
            // nearLimit 90°: verhindert, dass der Sprung von +180 auf −180 am Gegenpunkt als
            // Nullstelle gilt.
            java.util.List<Double> roots = astroapi.utils.CrossingScan.scan(
                    distanceToBoundary, fromJd, fromJd + maxDays, 0.05, 90.0);
            return roots.isEmpty() ? java.util.OptionalDouble.empty()
                    : java.util.OptionalDouble.of(roots.get(0));
        }, ayanamsa);
    }

    public double julianDay(LocalDateTime utDateTime) {
        double hours = utDateTime.getHour()
                + utDateTime.getMinute() / 60.0
                + utDateTime.getSecond() / 3600.0;
        return new SweDate(utDateTime.getYear(), utDateTime.getMonthValue(),
                utDateTime.getDayOfMonth(), hours).getJulDay();
    }

    // ── Berechnung ───────────────────────────────────────────────────────────

    private VedicChartDTO compute(VedicPersonRequest person, ZoneId zone, LocalDateTime ut,
                                  double julianDay, Ayanamsa ayanamsa, NodeType nodeType) {
        double latitude = person.getCoordinates()[0];
        double longitude = person.getCoordinates()[1];

        RawChart raw = run(eph -> {
            RawChart r = new RawChart();

            // Siderische Längen der sieben Grahas plus Rahu.
            for (VedicBody body : VedicBody.classicalGrahas()) {
                double[] xx = new double[6];
                StringBuffer err = new StringBuffer();
                int ret = eph.swe_calc_ut(julianDay, body.getSweConst(), CALC_FLAGS, xx, err);
                if (ret == SweConst.ERR) {
                    throw new IllegalStateException(
                            "Sidereal calculation failed for " + body.getKey() + ": " + err);
                }
                r.longitudes.put(body, VedicMath.normalizeDegrees(xx[0]));
                r.speeds.put(body, xx[3]);
            }

            double[] nodePos = new double[6];
            StringBuffer nodeErr = new StringBuffer();
            int nodeRet = eph.swe_calc_ut(julianDay, nodeType.getSweConst(), CALC_FLAGS, nodePos, nodeErr);
            if (nodeRet == SweConst.ERR) {
                throw new IllegalStateException("Sidereal node calculation failed: " + nodeErr);
            }
            double rahu = VedicMath.normalizeDegrees(nodePos[0]);
            r.longitudes.put(VedicBody.RAHU, rahu);
            r.speeds.put(VedicBody.RAHU, nodePos[3]);
            // Ketu wird nicht gerechnet, sondern abgeleitet — exakt gegenüber.
            r.longitudes.put(VedicBody.KETU, VedicMath.normalizeDegrees(rahu + 180.0));
            r.speeds.put(VedicBody.KETU, nodePos[3]);

            // Die tatsächlich angewandte Ayanamsha: dieselbe Sonne einmal tropisch.
            double[] tropicalSun = new double[6];
            StringBuffer sunErr = new StringBuffer();
            int sunRet = eph.swe_calc_ut(julianDay, SweConst.SE_SUN, CALC_FLAGS_TROPICAL, tropicalSun, sunErr);
            if (sunRet == SweConst.ERR) {
                throw new IllegalStateException("Tropical reference calculation failed: " + sunErr);
            }
            r.appliedAyanamsa = VedicMath.normalizeDegrees(
                    tropicalSun[0] - r.longitudes.get(VedicBody.SUN));
            r.apiAyanamsa = eph.swe_get_ayanamsa_ut(julianDay);

            // Aszendent siderisch — gleiche Rahmenkorrektur wie die Planeten.
            double[] cusps = new double[CUSP_ARRAY_SIZE];
            double[] ascmc = new double[10];
            int houseRet = eph.swe_houses(julianDay, CALC_FLAGS & ~SweConst.SEFLG_SPEED,
                    latitude, longitude, WHOLE_SIGN, cusps, ascmc);
            if (houseRet == SweConst.ERR) {
                throw new IllegalArgumentException("Sidereal ascendant could not be calculated.");
            }
            r.ascendant = VedicMath.normalizeDegrees(ascmc[0]);
            return r;
        }, ayanamsa);

        return assemble(person, zone, ut, julianDay, raw);
    }

    /**
     * Baut das DTO.
     *
     * <p><b>Der Schalter {@code timeKnown} ist hier das Wesentliche.</b> Ohne Geburtszeit wird
     * Mitternacht angenommen — und dann ist der Aszendent keine Angabe über die Geburt, sondern
     * über Mitternacht. Die erste Fassung hat ihn trotzdem ausgegeben und nur gewarnt; eine
     * Warnung neben einer Gradzahl verhindert aber nicht, dass die Gradzahl als Ergebnis gelesen
     * wird. Deshalb fehlt die zeitabhängige Hälfte jetzt ganz: Aszendent, Häuser und das Haus
     * jedes Grahas bleiben {@code null} beziehungsweise leer.
     *
     * <p>Der Mond bleibt — er hat eine Position, auch wenn sie um bis zu 13° unsicher ist. Das
     * ganze Ashtakoota hängt daran und wäre ohne ihn nicht zu rechnen; die Unsicherheit steht als
     * Warnung daneben.
     */
    private VedicChartDTO assemble(VedicPersonRequest person, ZoneId zone, LocalDateTime ut,
                                   double julianDay, RawChart raw) {
        boolean timeKnown = person.isBirthTimeKnown();
        VedicChartDTO chart = new VedicChartDTO();
        chart.setTimeDependentAvailable(timeKnown);
        chart.setBirthTimeKnown(timeKnown);
        chart.setLocalDateTime(person.getDateTime().toString());
        chart.setUtDateTime(ut.toString());
        chart.setZoneId(zone.getId());
        chart.setJulianDayUT(julianDay);
        chart.setCoordinates(new double[]{person.getCoordinates()[0], person.getCoordinates()[1]});
        chart.setAyanamsaDegrees(raw.appliedAyanamsa);
        chart.setAyanamsaDegreesFormatted(VedicMath.formatDegreeMinuteSecond(raw.appliedAyanamsa));
        chart.setAyanamsaApiDegrees(raw.apiAyanamsa);

        Rashi ascRashi = timeKnown ? Rashi.ofLongitude(raw.ascendant) : null;
        Rashi ascNavamsa = timeKnown ? Navamsa.signOf(raw.ascendant) : null;
        if (timeKnown) {
            chart.setAscendantLongitude(raw.ascendant);
            chart.setAscendantDegreeInSign(VedicMath.degreeInSign(raw.ascendant));
            chart.setAscendantDegreeInSignFormatted(
                    VedicMath.formatDegreeMinuteSecond(VedicMath.degreeInSign(raw.ascendant)));
            chart.setAscendantRashiIndex(ascRashi.getIndex());
            chart.setAscendantRashi(ascRashi.getKey());
            chart.setAscendantRashiLord(ascRashi.getLord().getKey());
            chart.setAscendantNakshatra(nakshatraPosition(raw.ascendant));
            chart.setAscendantNavamsaRashiIndex(ascNavamsa.getIndex());
            chart.setAscendantNavamsaRashi(ascNavamsa.getKey());
        }

        List<VedicPlanetDTO> d1 = new ArrayList<>();
        List<VedicPlanetDTO> d9 = new ArrayList<>();
        for (VedicBody body : VedicBody.values()) {
            double lon = raw.longitudes.get(body);
            d1.add(planetDto(body, lon, raw, ascRashi));
            // D9: dasselbe Gestirn, aber im Navamsa-Zeichen. Der Hausbezug in D9 wird vom
            // Navamsa-Aszendenten aus gezählt, nicht vom D1-Aszendenten — alles andere wäre eine
            // Mischung zweier Charts.
            d9.add(navamsaDto(body, lon, raw, ascNavamsa));
        }
        chart.setPlanets(d1);
        chart.setNavamsaPlanets(d9);
        chart.setHouses(timeKnown ? houses(ascRashi, raw) : List.of());

        double moonLon = raw.longitudes.get(VedicBody.MOON);
        Rashi moonRashi = Rashi.ofLongitude(moonLon);
        chart.setMoonRashiIndex(moonRashi.getIndex());
        chart.setMoonRashi(moonRashi.getKey());
        chart.setMoonNakshatra(nakshatraPosition(moonLon));
        chart.setVarna(moonRashi.getVarna().getKey());
        chart.setVashya(moonRashi.vashya(VedicMath.degreeInSign(moonLon)).getKey());
        return chart;
    }

    private VedicPlanetDTO planetDto(VedicBody body, double lon, RawChart raw,
                                     Rashi ascendantRashi) {
        Rashi rashi = Rashi.ofLongitude(lon);
        VedicPlanetDTO dto = new VedicPlanetDTO();
        dto.setBody(body.getKey());
        dto.setSiderealLongitude(lon);
        dto.setDegreeInSign(VedicMath.degreeInSign(lon));
        dto.setDegreeInSignFormatted(VedicMath.formatDegreeMinuteSecond(VedicMath.degreeInSign(lon)));
        dto.setRashiIndex(rashi.getIndex());
        dto.setRashi(rashi.getKey());
        dto.setRashiLord(rashi.getLord().getKey());
        // Ohne Aszendenten gibt es kein Haus — und kein Haus ist die richtige Antwort, nicht 1.
        dto.setHouse(ascendantRashi == null ? null : Rashi.countFrom(ascendantRashi, rashi));
        boolean node = body == VedicBody.RAHU || body == VedicBody.KETU;
        dto.setAlwaysRetrograde(node);
        Double speed = raw.speeds.get(body);
        dto.setRetrograde(node || (speed != null && speed < 0));
        Rashi navamsaRashi = Navamsa.signOf(lon);
        dto.setNavamsaRashiIndex(navamsaRashi.getIndex());
        dto.setNavamsaRashi(navamsaRashi.getKey());
        dto.setNakshatra(nakshatraPosition(lon));
        return dto;
    }

    /**
     * Derselbe Körper im D9. {@code siderealLongitude} bleibt die D1-Länge — das Navamsa ist ein
     * Zeichen, kein eigener Grad. Wer eine D9-Gradangabe erwartet, soll sie nicht bekommen,
     * sondern merken, dass D9 anders funktioniert.
     */
    private VedicPlanetDTO navamsaDto(VedicBody body, double lon, RawChart raw, Rashi navamsaAscendant) {
        Rashi navamsaRashi = Navamsa.signOf(lon);
        VedicPlanetDTO dto = new VedicPlanetDTO();
        dto.setBody(body.getKey());
        dto.setSiderealLongitude(lon);
        dto.setRashiIndex(navamsaRashi.getIndex());
        dto.setRashi(navamsaRashi.getKey());
        dto.setRashiLord(navamsaRashi.getLord().getKey());
        dto.setNavamsaRashiIndex(navamsaRashi.getIndex());
        dto.setNavamsaRashi(navamsaRashi.getKey());
        dto.setHouse(navamsaAscendant == null ? null : Rashi.countFrom(navamsaAscendant, navamsaRashi));
        boolean node = body == VedicBody.RAHU || body == VedicBody.KETU;
        dto.setAlwaysRetrograde(node);
        Double speed = raw.speeds.get(body);
        dto.setRetrograde(node || (speed != null && speed < 0));
        return dto;
    }

    private List<VedicHouseDTO> houses(Rashi ascendantRashi, RawChart raw) {
        List<VedicHouseDTO> houses = new ArrayList<>(12);
        for (int house = 1; house <= 12; house++) {
            Rashi rashi = Rashi.byIndex(ascendantRashi.getIndex() + house - 1);
            VedicHouseDTO dto = new VedicHouseDTO();
            dto.setHouse(house);
            dto.setRashiIndex(rashi.getIndex());
            dto.setRashi(rashi.getKey());
            dto.setLord(rashi.getLord().getKey());
            List<String> bodies = new ArrayList<>();
            for (VedicBody body : VedicBody.values()) {
                if (Rashi.ofLongitude(raw.longitudes.get(body)) == rashi) bodies.add(body.getKey());
            }
            dto.setBodies(bodies);
            houses.add(dto);
        }
        return houses;
    }

    /** Nakshatra, Pada und die genaue Lage darin. */
    public static NakshatraPositionDTO nakshatraPosition(double siderealLongitude) {
        double lon = VedicMath.normalizeDegrees(siderealLongitude);
        // Nakshatra UND Pada aus derselben 108er-Zahl: so kann die eine Angabe nicht weiterzählen,
        // während die andere noch zurückliegt. Siehe VedicMath.absoluteIndex.
        int absolutePada = VedicMath.absolutePada(lon);
        Nakshatra nakshatra = Nakshatra.byIndex(absolutePada / 4 + 1);
        int pada = absolutePada % 4 + 1;
        double degreeIn = lon - (nakshatra.getIndex() - 1) * VedicMath.NAKSHATRA_SPAN;
        // Die Gradangabe ist Anzeige; am Rand kann sie um Bruchteile eines Milligrad über die
        // Intervallgrenze hinausschießen, ohne dass die Zuordnung darunter leidet.
        if (degreeIn < 0) degreeIn = 0;
        if (degreeIn > VedicMath.NAKSHATRA_SPAN) degreeIn = VedicMath.NAKSHATRA_SPAN;

        NakshatraPositionDTO dto = new NakshatraPositionDTO();
        dto.setIndex(nakshatra.getIndex());
        dto.setKey(nakshatra.getKey());
        dto.setDisplay(nakshatra.getDisplay());
        dto.setPada(pada);
        dto.setLord(nakshatra.getLord().getKey());
        dto.setYoni(nakshatra.getYoni().getKey());
        dto.setYoniMale(nakshatra.isYoniMale());
        dto.setGana(nakshatra.getGana().getKey());
        dto.setNadi(nakshatra.getNadi().getKey());
        dto.setNadiAyurvedic(nakshatra.getNadi().getAyurvedicKey());
        dto.setSiderealLongitude(lon);
        dto.setDegreeInNakshatra(degreeIn);
        dto.setRemainingDegrees(VedicMath.NAKSHATRA_SPAN - degreeIn);
        dto.setRemainingDegreesInPada(pada * VedicMath.PADA_SPAN - degreeIn);
        return dto;
    }

    // ── Infrastruktur ────────────────────────────────────────────────────────

    private <T> T run(java.util.function.Function<SwissEph, T> work, Ayanamsa ayanamsa) {
        try {
            return executor.runWithTimeout(() -> {
                SwissEph eph = executor.current();
                // Zusätzlich zum Pool-Initialisierer: deckt den Fall ab, dass dieselbe Instanz
                // vorher für eine andere Ayanamsha benutzt wurde.
                eph.swe_set_sid_mode(ayanamsa.getSidMode(), 0, 0);
                return work.apply(eph);
            }, CALC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Vedic calculation timed out after "
                    + CALC_TIMEOUT_SECONDS + "s", e);
        }
    }

    private ZoneId resolveZone(double[] coordinates) {
        return timeZoneService.getTimeZone(coordinates[0], coordinates[1])
                .orElseThrow(() -> new IllegalArgumentException(
                        "No time zone found for the provided coordinates."));
    }

    private static void validate(VedicPersonRequest person) {
        double[] c = person.getCoordinates();
        if (c == null || c.length != 2) {
            throw new IllegalArgumentException(
                    "Coordinates must contain exactly two values: [latitude, longitude]");
        }
        if (c[0] < -90 || c[0] > 90 || c[1] < -180 || c[1] > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values.");
        }
        if (person.getDateTime() == null) {
            throw new IllegalArgumentException("dateTime cannot be null");
        }
    }

    private static String cacheKey(double julianDay, double[] coordinates,
                                   Ayanamsa ayanamsa, NodeType nodeType, boolean birthTimeKnown) {
        // Auf die Minute gerundet wie in der tropischen Strecke; drei Dezimalen der Koordinaten
        // sind etwa 111 m und für den Aszendenten mehr als genug.
        //
        // `birthTimeKnown` gehört in den Schlüssel, seit es das ERGEBNIS verändert: ohne
        // Geburtszeit fehlt die zeitabhängige Hälfte des Charts. Ohne diesen Bestandteil bekäme
        // die zweite Anfrage den Eintrag der ersten — mit Aszendent, obwohl sie keine Zeit hat,
        // oder ohne, obwohl sie eine hat.
        return VedicRuleSet.contractKey(ayanamsa, nodeType)
                + "|" + Math.round(julianDay * 1440.0)
                + "|" + Math.round(coordinates[0] * 1000)
                + "|" + Math.round(coordinates[1] * 1000)
                + "|" + (birthTimeKnown ? "t" : "f");
    }

    /**
     * Setzt die anfragebezogenen Felder auf eine <b>Kopie</b>. Der Cache-Eintrag selbst wird nie
     * verändert — sonst schriebe Person B ihr Label in das Chart, das Person A gerade liest.
     */
    private static VedicChartDTO withRequestFields(VedicChartDTO cached, VedicPersonRequest person) {
        VedicChartDTO copy = new VedicChartDTO();
        copy.setLabel(person.getLabel());
        // Aus dem Chart, nicht aus der Anfrage: das Flag und die vorhandenen Felder müssen
        // dasselbe sagen. Seit `birthTimeKnown` im Cache-Schlüssel steht, kann es nicht abweichen.
        copy.setBirthTimeKnown(cached.isBirthTimeKnown());
        copy.setLocalDateTime(cached.getLocalDateTime());
        copy.setUtDateTime(cached.getUtDateTime());
        copy.setZoneId(cached.getZoneId());
        copy.setJulianDayUT(cached.getJulianDayUT());
        copy.setCoordinates(cached.getCoordinates());
        copy.setTimeDependentAvailable(cached.isTimeDependentAvailable());
        copy.setAyanamsaDegrees(cached.getAyanamsaDegrees());
        copy.setAyanamsaDegreesFormatted(cached.getAyanamsaDegreesFormatted());
        copy.setAyanamsaApiDegrees(cached.getAyanamsaApiDegrees());
        copy.setAscendantLongitude(cached.getAscendantLongitude());
        copy.setAscendantDegreeInSign(cached.getAscendantDegreeInSign());
        copy.setAscendantDegreeInSignFormatted(cached.getAscendantDegreeInSignFormatted());
        copy.setAscendantRashiIndex(cached.getAscendantRashiIndex());
        copy.setAscendantRashi(cached.getAscendantRashi());
        copy.setAscendantRashiLord(cached.getAscendantRashiLord());
        copy.setAscendantNakshatra(cached.getAscendantNakshatra());
        copy.setAscendantNavamsaRashiIndex(cached.getAscendantNavamsaRashiIndex());
        copy.setAscendantNavamsaRashi(cached.getAscendantNavamsaRashi());
        copy.setMoonRashiIndex(cached.getMoonRashiIndex());
        copy.setMoonRashi(cached.getMoonRashi());
        copy.setMoonNakshatra(cached.getMoonNakshatra());
        copy.setVarna(cached.getVarna());
        copy.setVashya(cached.getVashya());
        copy.setPlanets(cached.getPlanets());
        copy.setNavamsaPlanets(cached.getNavamsaPlanets());
        copy.setHouses(cached.getHouses());
        return copy;
    }

    /** Rohwerte einer Ephemeridenabfrage, bevor daraus ein DTO wird. */
    private static final class RawChart {
        final Map<VedicBody, Double> longitudes = new EnumMap<>(VedicBody.class);
        final Map<VedicBody, Double> speeds = new EnumMap<>(VedicBody.class);
        double ascendant;
        double appliedAyanamsa;
        double apiAyanamsa;
    }
}
