package astroapi.utils;

import astroapi.dto.*;
import astroapi.service.SwissEphemerisExecutor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import swisseph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class SwissEphemerisHelper {
    private static final Logger logger = LoggerFactory.getLogger(SwissEphemerisHelper.class);
    private SwissEphemerisExecutor epheExecutor;

    /**
     * The Swiss Ephemeris instance owned by the current worker thread.
     * Must only be called from inside an {@code epheExecutor.runWithTimeout(...)}
     * task, so the calculation runs on a pool thread with its own instance.
     */
    private SwissEph swe() {
        return epheExecutor.current();
    }

    /** Shut the worker pool down cleanly when the application context stops. */
    @PreDestroy
    public void shutdown() {
        epheExecutor.shutdown();
    }

    // Planets change ~1°/day max (except Moon ~13°/day). Rounding to the nearest minute
    // is precise enough for all use-cases and gives a high hit-rate for transit noon requests.
    private final Cache<Long, Map<String, Double>> planetCache;
    private final Cache<String, HouseDataDTO>      cuspCache;
    // Lunar phase and retrograde status barely change within minutes; 10-min key is fine.
    private final Cache<Long, String>              lunarPhaseCache;
    private final Cache<Long, Map<String, Boolean>> retrogradeCache;

    public SwissEphemerisHelper() {
        epheExecutor = new SwissEphemerisExecutor("ephe/");

        planetCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(8, TimeUnit.HOURS)
                .build();
        cuspCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(8, TimeUnit.HOURS)
                .build();
        lunarPhaseCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
        retrogradeCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(8, TimeUnit.HOURS)
                .build();
    }

    // Round Julian Day to the nearest minute (1 JD = 1440 minutes)
    private static long jdToMinuteKey(double julianDay) {
        return Math.round(julianDay * 1440.0);
    }

    // Round Julian Day to the nearest 10 minutes
    private static long jdTo10MinKey(double julianDay) {
        return Math.round(julianDay * 144.0);
    }

    /**
     * Retrieves the Sun's position (longitude) for a given LocalDateTime.
     * This calculation is executed in a separate thread with a timeout.
     *
     * @param date The desired date and time.
     * @return The Sun's position in degrees.
     * @throws RuntimeException if the calculation times out or encounters an error.
     */
    public double getSunPosition(LocalDateTime date) {
        try {
            return epheExecutor.runWithTimeout(() -> {
                double julianDay = calculateJulianDay(date);
                double[] xpos = new double[6];
                int ret = swe().swe_calc_ut(julianDay, SweConst.SE_SUN, SweConst.SEFLG_SWIEPH, xpos, null);
                if (ret == SweConst.OK) {
                    return xpos[0]; // Sun's longitude in degrees
                } else {
                    throw new IllegalArgumentException("Error calculating the Sun's position: " + ret);
                }
            }, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Sun position calculation timed out.", e);
        }
    }

    /**
     * Retrieves the position of a specified planet for a given LocalDateTime.
     * Executed in a separate thread with a timeout.
     *
     * @param planetId Swiss Ephemeris constant for the planet (e.g., SweConst.SE_SUN, SE_MOON).
     * @param dateTime The date and time to calculate the planet's position.
     * @return The planet's longitude in degrees or NaN if an error occurs.
     */
    private double getPlanetPosition(int planetId, LocalDateTime dateTime) {
        try {
            return epheExecutor.runWithTimeout(() -> {
                double julianDay = calculateJulianDay(dateTime);
                double[] position = new double[6];
                StringBuffer serr = new StringBuffer();
                int flags = SweConst.SEFLG_SWIEPH;

                int result = swe().swe_calc_ut(julianDay, planetId, flags, position, serr);
                if (result == SweConst.ERR) {
                    return Double.NaN;
                }
                return position[0]; // Planet longitude
            }, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Planet position calculation timed out for planetId: " + planetId, e);
        }
    }

    /**
     * Calculates the current Moon position and its interpretation.
     *
     * @param zoneId The time zone.
     * @return A MoonDTO containing Moon data and its interpretation.
     */
    public MoonDTO getCurrentMoonPosDetails(ZoneId zoneId) {
        return getCurrentMoonPosDetails(zoneId, null);
    }

    public MoonDTO getCurrentMoonPosDetails(ZoneId zoneId, java.time.LocalDateTime fixedDateTime) {
        // A provided fixedDateTime is interpreted as wall-clock time in the given
        // zoneId (consistent with the now(zoneId) branch); it is converted to UTC
        // below for the actual calculation.
        ZonedDateTime dateTime = fixedDateTime != null
                ? ZonedDateTime.of(fixedDateTime, zoneId)
                : ZonedDateTime.now(zoneId);
        ZonedDateTime dateTimeUT = dateTime.withZoneSameInstant(ZoneOffset.UTC);
        double hour = dateTimeUT.getHour() + dateTimeUT.getMinute() / 60.0 + dateTimeUT.getSecond() / 3600.0;
        double julianDay = new SweDate(
                dateTimeUT.getYear(),
                dateTimeUT.getMonthValue(),
                dateTimeUT.getDayOfMonth(),
                hour
        ).getJulDay();

        // Computed outside the calculation task so we don't submit a nested
        // Swiss-Ephemeris task from within a pool thread (would risk pool starvation).
        // Use the UTC-normalised time so the phase matches the Moon position above
        // (calculateJulianDay treats its LocalDateTime as UT).
        final String lunarPhase = getLunarPhase(dateTimeUT.toLocalDateTime());

        try {
            return epheExecutor.runWithTimeout(() -> {
                double[] xx = new double[6];
                StringBuffer serr = new StringBuffer();
                int iflag = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED;

                // Execute Swiss Ephemeris calculation inside the timeout block
                int result = swe().swe_calc_ut(julianDay, SweConst.SE_MOON, iflag, xx, serr);

                MoonDTO moonDTO = new MoonDTO();
                moonDTO.setJulianDate(julianDay);

                if (result == SweConst.ERR) {
                    // Error handling: Set NaN values and an error message
                    moonDTO.setLongitude(Double.NaN);
                    moonDTO.setLatitude(Double.NaN);
                    moonDTO.setDistance(Double.NaN);
                    moonDTO.setSpeedLongitude(Double.NaN);
                    moonDTO.setSpeedLatitude(Double.NaN);
                    moonDTO.setSpeedDistance(Double.NaN);
                    moonDTO.setMoonPosInterpretation("Error in calculation");
                    return moonDTO;
                }

                // Set Moon data
                moonDTO.setLongitude(xx[0]);
                moonDTO.setLatitude(xx[1]);
                moonDTO.setDistance(xx[2]);
                moonDTO.setSpeedLongitude(xx[3]);
                moonDTO.setSpeedLatitude(xx[4]);
                moonDTO.setSpeedDistance(xx[5]);
                moonDTO.setMoonPhase(lunarPhase);

                // Interpret Moon position
                String interpretation = AstrologyUtils.interpretDegree(xx[0]);
                moonDTO.setMoonPosInterpretation(interpretation);

                return moonDTO;

            }, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Moon position calculation timed out.", e);
        }
    }


    /**
     * Calculates planetary positions for a given Julian Day in a separate thread with a timeout.
     *
     * @param julianDay The Julian Day.
     * @return A map of planet names to their positions in degrees.
     */
    public Map<String, Double> calculatePlanetPositions(double julianDay) {
        long key = jdToMinuteKey(julianDay);
        return planetCache.get(key, k -> {
            try {
                return epheExecutor.runWithTimeout(() -> {
                    Map<String, Double> planets = new HashMap<>();
                    double[] result = new double[6];
                    StringBuffer errorBuffer = new StringBuffer();

                    for (Planet planet : Planet.values()) {
                        int planetId = planet.getSweConstValue();
                        int returnCode = swe().swe_calc_ut(julianDay, planetId, SweConst.SEFLG_SWIEPH, result, errorBuffer);
                        if (returnCode == SweConst.ERR) {
                            logger.error("Error calculating {}: {}", planet.getName(), errorBuffer.toString());
                            planets.put(planet.getName(), Double.NaN);
                        } else {
                            planets.put(planet.getName(), result[0]);
                        }
                    }
                    return planets;
                }, 3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Calculation of planet positions timed out.", e);
            }
        });
    }

    /**
     * Calculates the house cusps and Ascendant/MC for given parameters.
     * Executed in a separate thread with a timeout.
     *
     * @param julianDay The Julian Day.
     * @param latitude  The geographic latitude.
     * @param longitude The geographic longitude.
     * @param houseSystem The house system to use.
     * @return A HouseDataDTO with cusps, Ascendant, and MC.
     */
    public HouseDataDTO calculateCusps(double julianDay, double latitude, double longitude, HouseSystem houseSystem) {
        // 3 decimal places ~ 111 m precision; more than sufficient for house cusps
        String key = jdToMinuteKey(julianDay) + "_"
                + Math.round(latitude * 1000) + "_"
                + Math.round(longitude * 1000) + "_"
                + houseSystem.getCode();
        return cuspCache.get(key, k -> {
            try {
                return epheExecutor.runWithTimeout(() -> {
                    double[] cuspDegrees = new double[13];
                    double[] ascmc = new double[10];
                    HouseDataDTO houseData = new HouseDataDTO();

                    int ret = swe().swe_houses(julianDay, SweConst.SEFLG_SWIEPH, latitude, longitude, houseSystem.getCode(), cuspDegrees, ascmc);
                    if (ret == SweConst.ERR) {
                        throw new IllegalArgumentException("Error calculating house cusps.");
                    }

                    List<Double> cusps = new ArrayList<>();
                    for (int i = 1; i <= 12; i++) {
                        cusps.add(cuspDegrees[i]);
                    }
                    houseData.setCusps(cusps);
                    houseData.setAscendant(ascmc[0]);
                    houseData.setMediumCoeli(ascmc[1]);

                    return houseData;
                }, 3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("House cusp calculation timed out.", e);
            }
        });
    }

    /**
     * Retrieves significant astrological events for a given date.
     *
     * @param dateTime The date and time for which to find events.
     * @return A list of significant event descriptions.
     */
    public List<String> getSignificantEvents(LocalDateTime dateTime) {
        List<String> events = new ArrayList<>();

        // Check for Solar or Lunar Eclipses
        String eclipse = checkForEclipse(dateTime);
        if (eclipse != null) {
            events.add(eclipse);
        }

        // Check for Planetary Ingresses (when a planet enters a new sign)
        List<String> ingresses = checkForIngresses(dateTime);
        events.addAll(ingresses);

        return events;
    }

    /**
     * Checks for eclipses (both lunar and solar) on the given date.
     *
     * @param dateTime The date and time to check.
     * @return A string describing the eclipse or null if none.
     */
    private String checkForEclipse(LocalDateTime dateTime) {
        String lunarEclipse = checkForLunarEclipse(dateTime);
        String solarEclipse = checkForSolarEclipse(dateTime);

        if (lunarEclipse != null && solarEclipse != null) {
            return lunarEclipse + " and " + solarEclipse;
        } else if (lunarEclipse != null) {
            return lunarEclipse;
        } else if (solarEclipse != null) {
            return solarEclipse;
        } else {
            return null;
        }
    }

    /**
     * Checks if a lunar eclipse occurs on the given date.
     *
     * @param dateTime The date and time to check.
     * @return A string with information about the lunar eclipse or null if none occurs.
     */
    private String checkForLunarEclipse(LocalDateTime dateTime) {
        try {
            return epheExecutor.runWithTimeout(() -> {
                double julianDayUT = calculateJulianDay(dateTime);
                double[] tret = new double[10];
                StringBuffer serr = new StringBuffer();

                // Combine eclipse types for lunar eclipses
                int eclipseTypeFlags = SweConst.SE_ECL_TOTAL | SweConst.SE_ECL_PARTIAL | SweConst.SE_ECL_PENUMBRAL;

                int retFlag = swe().swe_lun_eclipse_when(julianDayUT, SweConst.SEFLG_SWIEPH, eclipseTypeFlags, tret, 0, serr);

                if (retFlag == SweConst.ERR) {
                    System.err.println("Error in lunar eclipse calculation: " + serr);
                    return null;
                }

                double eclipseTimeUT = tret[0];
                LocalDateTime eclipseDateTime = convertJulianDayToLocalDateTime(eclipseTimeUT);

                // Check if the eclipse occurs on the same day
                if (eclipseDateTime.toLocalDate().equals(dateTime.toLocalDate())) {
                    int eclipseType = retFlag & (SweConst.SE_ECL_TOTAL | SweConst.SE_ECL_PARTIAL | SweConst.SE_ECL_PENUMBRAL);
                    String eclipseTypeStr = getLunarEclipseType(eclipseType);
                    return "Lunar Eclipse (" + eclipseTypeStr + ") on " + eclipseDateTime.toLocalDate();
                }

                return null;
            }, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Lunar eclipse calculation timed out.", e);
        }
    }

    /**
     * Returns the type of lunar eclipse as a string.
     *
     * @param eclipseType The eclipse type flag.
     * @return The type of lunar eclipse as a string.
     */
    private String getLunarEclipseType(int eclipseType) {
        if ((eclipseType & SweConst.SE_ECL_TOTAL) != 0) {
            return "Total";
        } else if ((eclipseType & SweConst.SE_ECL_PARTIAL) != 0) {
            return "Partial";
        } else if ((eclipseType & SweConst.SE_ECL_PENUMBRAL) != 0) {
            return "Penumbral";
        } else {
            return "Unknown";
        }
    }

    /**
     * Checks if a solar eclipse occurs on the given date.
     *
     * @param dateTime The date and time to check.
     * @return A string with information about the solar eclipse or null if none occurs.
     */
    private String checkForSolarEclipse(LocalDateTime dateTime) {
        try {
            return epheExecutor.runWithTimeout(() -> {
                double julianDayUT = calculateJulianDay(dateTime);
                double[] tret = new double[10];
                StringBuffer serr = new StringBuffer();

                // Combine eclipse types for solar eclipses
                int eclipseTypeFlags = SweConst.SE_ECL_TOTAL | SweConst.SE_ECL_ANNULAR | SweConst.SE_ECL_PARTIAL | SweConst.SE_ECL_ANNULAR_TOTAL;

                int retFlag = swe().swe_sol_eclipse_when_glob(julianDayUT, SweConst.SEFLG_SWIEPH, eclipseTypeFlags, tret, 0, serr);

                if (retFlag == SweConst.ERR) {
                    System.err.println("Error in solar eclipse calculation: " + serr.toString());
                    return null;
                }

                double eclipseTimeUT = tret[0];
                LocalDateTime eclipseDateTime = convertJulianDayToLocalDateTime(eclipseTimeUT);

                // Check if the eclipse occurs on the same day
                if (eclipseDateTime.toLocalDate().equals(dateTime.toLocalDate())) {
                    int eclipseType = retFlag & (SweConst.SE_ECL_TOTAL | SweConst.SE_ECL_ANNULAR | SweConst.SE_ECL_PARTIAL | SweConst.SE_ECL_ANNULAR_TOTAL);
                    String eclipseTypeStr = getSolarEclipseType(eclipseType);
                    return "Solar Eclipse (" + eclipseTypeStr + ") on " + eclipseDateTime.toLocalDate();
                }

                return null;
            }, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Solar eclipse calculation timed out.", e);
        }
    }


    /**
     * Returns the type of solar eclipse as a string.
     *
     * @param eclipseType The eclipse type flag.
     * @return The type of solar eclipse as a string.
     */
    private String getSolarEclipseType(int eclipseType) {
        if ((eclipseType & SweConst.SE_ECL_TOTAL) != 0) {
            return "Total";
        } else if ((eclipseType & SweConst.SE_ECL_ANNULAR) != 0) {
            return "Annular";
        } else if ((eclipseType & SweConst.SE_ECL_PARTIAL) != 0) {
            return "Partial";
        } else if ((eclipseType & SweConst.SE_ECL_ANNULAR_TOTAL) != 0) {
            return "Hybrid";
        } else {
            return "Unknown";
        }
    }

    /**
     * Retrieves the topocentric position of the Moon.
     *
     * @param dateTime The date and time.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @param altitude The altitude of the location.
     * @param zoneId The time zone.
     * @return The Moon's longitude in degrees or NaN if an error occurs.
     */
    public Double getMoonPosTopocentric(LocalDateTime dateTime, double latitude, double longitude, double altitude, ZoneId zoneId) {
        try {
            return epheExecutor.runWithTimeout(() -> {
                ZonedDateTime zonedTime = ZonedDateTime.of(dateTime, zoneId);
                ZonedDateTime dateTimeUT = zonedTime.withZoneSameInstant(ZoneOffset.UTC);

                double jdET = SweDate.getJulDay(
                        dateTimeUT.getYear(),
                        dateTimeUT.getMonthValue(),
                        dateTimeUT.getDayOfMonth(),
                        dateTimeUT.getHour() + dateTimeUT.getMinute() / 60.0 + dateTimeUT.getSecond() / 3600.0
                );

                // Set topocentric parameters
                swe().swe_set_topo(longitude, latitude, altitude);

                double[] xx = new double[6];
                StringBuffer serr = new StringBuffer();
                // SEFLG_TOPOCTR is required for swe_set_topo to actually take effect —
                // without it the result would be geocentric despite the topo call.
                int iflag = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED | SweConst.SEFLG_TOPOCTR;

                // Calculate Moon position
                int result = swe().swe_calc_ut(jdET, SweConst.SE_MOON, iflag, xx, serr);

                // Check for errors
                if (result < 0) {
                    return Double.NaN;
                } else {
                    return xx[0];
                }
            }, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Topocentric Moon position calculation timed out.", e);
        }
    }

    /**
     * Calculates the composite horoscope by averaging the positions of two individual horoscopes.
     *
     * @param horoscopeOne The first individual's horoscope data.
     * @param horoscopeTwo The second individual's horoscope data.
     * @param compositeLat The latitude of the composite location.
     * @param compositeLon The longitude of the composite location.
     * @param houseSystem  The house system to be used for composite houses.
     * @return A {@link CompositeHoroscopeDTO} containing the composite horoscope data.
     * @throws IllegalArgumentException If input data is invalid or if calculations fail.
     */
    public CompositeHoroscopeDTO calculateCompositeHoroscope(EphiDatenDTO horoscopeOne, EphiDatenDTO horoscopeTwo,
                                                             double compositeLat, double compositeLon, HouseSystem houseSystem) {
        if (horoscopeOne == null || horoscopeTwo == null) {
            throw new IllegalArgumentException("Both horoscopes must be provided for composite calculation.");
        }

        // Calculate average planetary positions
        Map<String, Double> compositePlanets = calculateCompositePlanets(horoscopeOne.getPlanets(), horoscopeTwo.getPlanets());

        // Calculate average Ascendant and Medium Coeli
        double compositeAscendant = averageDegrees(horoscopeOne.getAscendant(), horoscopeTwo.getAscendant());
        double compositeMediumCoeli = averageDegrees(horoscopeOne.getMediumCoeli(), horoscopeTwo.getMediumCoeli());

        // Calculate composite houses based on composite location and averaged Ascendant
        HouseDataDTO compositeHouseData = calculateCompositeHouses(compositeAscendant, compositeMediumCoeli, compositeLat, compositeLon, houseSystem);

        // Create CompositeHoroscopeDTO
        CompositeHoroscopeDTO compositeHoroscope = new CompositeHoroscopeDTO();
        compositeHoroscope.setPlanets(compositePlanets);
        compositeHoroscope.setCusps(compositeHouseData.getCusps());
        compositeHoroscope.setAscendant(compositeHouseData.getAscendant());
        compositeHoroscope.setMediumCoeli(compositeHouseData.getMediumCoeli());
        compositeHoroscope.setAscendantInterpretation(AstrologyUtils.interpretDegree(compositeHouseData.getAscendant()));
        compositeHoroscope.setMediumCoeliInterpretation(AstrologyUtils.interpretDegree(compositeHouseData.getMediumCoeli()));
        compositeHoroscope.setAspects(calculateTransits(horoscopeOne, horoscopeTwo));
        return compositeHoroscope;
    }

    /**
     * Calculates the composite planetary positions by averaging individual positions.
     *
     * @param planetsOne Planet positions of partner one.
     * @param planetsTwo Planet positions of partner two.
     * @return A map containing the composite planetary positions.
     */
    private Map<String, Double> calculateCompositePlanets(Map<String, Double> planetsOne, Map<String, Double> planetsTwo) {
        Map<String, Double> compositePlanets = new HashMap<>();
        for (String planet : planetsOne.keySet()) {
            if (planetsTwo.containsKey(planet)) {
                double pos1 = planetsOne.get(planet);
                double pos2 = planetsTwo.get(planet);
                double average = averageDegrees(pos1, pos2);
                compositePlanets.put(planet, average);
            }
        }
        return compositePlanets;
    }

    /**
     * Averages two degree values, ensuring the result stays within 0-360 degrees.
     *
     * @param degreeOne The first degree value.
     * @param degreeTwo The second degree value.
     * @return The averaged degree value.
     */
    private double averageDegrees(double degreeOne, double degreeTwo) {
        double avg = (degreeOne + degreeTwo) / 2.0;
        if (avg >= 360.0) {
            avg -= 360.0;
        }
        return avg;
    }

    /**
     * Calculates the composite Ascendant and Medium Coeli based on the composite location.
     *
     * @param compositeAscendant    The averaged Ascendant degree.
     * @param compositeMediumCoeli  The averaged Medium Coeli degree.
     * @param latitude              The latitude of the composite location.
     * @param longitude             The longitude of the composite location.
     * @param houseSystem           The house system to be used.
     * @return A {@link HouseDataDTO} containing the composite house data.
     */
    private HouseDataDTO calculateCompositeHouses(double compositeAscendant, double compositeMediumCoeli,
                                                  double latitude, double longitude, HouseSystem houseSystem) {

        // Set Ascendant and MC
        double[] ascmc = new double[10];
        ascmc[0] = compositeAscendant; // Ascendant
        ascmc[1] = compositeMediumCoeli; // Medium Coeli

        // Calculate house cusps
        double[] cuspDegrees = new double[13]; // Houses 1-12
        char houseSystemCode = houseSystem.getCode(); // e.g., 'P' for Placidus
        double julianDay = calculateJulianDay(LocalDateTime.now()); // Julian Day for current date and time

        // Calculate house cusps using Swiss Ephemeris (on a pool thread with a thread-owned instance)
        int ret;
        try {
            ret = epheExecutor.runWithTimeout(() -> swe().swe_houses(
                    julianDay,
                    SweConst.SEFLG_SWIEPH,
                    latitude,
                    longitude,
                    houseSystemCode,
                    cuspDegrees,
                    ascmc
            ), 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Composite house calculation timed out.", e);
        }

        if (ret == SweConst.ERR) {
            throw new IllegalArgumentException("Error calculating composite houses: " + ret);
        }

        // Convert house cusps to a list
        List<Double> cusps = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            cusps.add(cuspDegrees[i]);
        }

        // Create HouseDataDTO
        HouseDataDTO houseData = new HouseDataDTO();
        houseData.setCusps(cusps);
        houseData.setAscendant(ascmc[0]);
        houseData.setMediumCoeli(ascmc[1]);

        return houseData;
    }

    /**
     * Calculates aspects between natal and transit planets.
     *
     * @param natalChart   The natal chart data.
     * @param transitChart The transit chart data.
     * @return A list of AspectDTO containing the aspects.
     */
    public List<AspectDTO> calculateTransits(EphiDatenDTO natalChart, EphiDatenDTO transitChart) {
        List<AspectDTO> aspects = new ArrayList<>();
        Map<String, Double> natalPlanets = natalChart.getPlanets();
        Map<String, Double> transitPlanets = transitChart.getPlanets();
        List<Double> natalHouseCusps = natalChart.getCusps();
        List<Double> transitHouseCusps = transitChart.getCusps(); // In case you need them

        for (String natalPlanet : natalPlanets.keySet()) {
            double natalPos = natalPlanets.get(natalPlanet);

            for (String transitPlanet : transitPlanets.keySet()) {
                double transitPos = transitPlanets.get(transitPlanet);
                double angle = Math.abs(natalPos - transitPos);
                angle = angle > 180 ? 360 - angle : angle;

                AspectType aspectType = getAspectType(angle);
                if (aspectType != null) {
                    AspectDTO aspectDTO = new AspectDTO();
                    aspectDTO.setNatalPlanet(natalPlanet);
                    aspectDTO.setTransitPlanet(transitPlanet);
                    aspectDTO.setAspectType(aspectType.getDescription());
                    aspectDTO.setExactAngle(aspectType.getExactDegree());
                    aspectDTO.setOrb(Math.abs(angle - aspectType.getExactDegree()));
                    aspectDTO.setNatalSign(AstrologyUtils.interpretDegree(natalPos));
                    aspectDTO.setTransitSign(AstrologyUtils.interpretDegree(transitPos));
                    aspectDTO.setNatalHouse(AstrologyUtils.getHouseForDegree(natalPos, natalHouseCusps));
                    aspectDTO.setTransitHouse(AstrologyUtils.getHouseForDegree(transitPos, natalHouseCusps)); // Transit planets in natal houses

                    aspects.add(aspectDTO);
                }
            }
        }
        return aspects;
    }

    /**
     * Retrieves planetary positions for a specific date and time.
     *
     * @param dateTime The date and time.
     * @return A map of planet names to their positions in degrees.
     */
    public Map<String, Double> getPlanetPositions(LocalDateTime dateTime) {
        double julianDay = calculateJulianDay(dateTime);
        return calculatePlanetPositions(julianDay);
    }

    /**
     * Determines the retrograde status of planets on a given date.
     *
     * @param dateTime The date and time for which to determine retrograde statuses.
     * @return A map where the key is the planet name and the value is a Boolean indicating retrograde status.
     */
    public Map<String, Boolean> getRetrogradeStatuses(LocalDateTime dateTime) {
        double julianDay = calculateJulianDay(dateTime);
        long key = jdToMinuteKey(julianDay);
        return retrogradeCache.get(key, k -> {
            try {
                return epheExecutor.runWithTimeout(() -> {
                    Map<String, Boolean> retrogradeStatuses = new HashMap<>();
                    int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED;
                    StringBuffer errorBuffer = new StringBuffer();
                    double[] position = new double[6];

                    for (Planet planet : Planet.values()) {
                        int planetId = planet.getSweConstValue();
                        int result = swe().swe_calc_ut(julianDay, planetId, flags, position, errorBuffer);
                        if (result != SweConst.ERR) {
                            retrogradeStatuses.put(planet.getName(), position[3] < 0);
                        } else {
                            logger.error("Error calculating retrograde for {}: {}", planet.getName(), errorBuffer.toString());
                            retrogradeStatuses.put(planet.getName(), null);
                        }
                    }
                    return retrogradeStatuses;
                }, 3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Retrograde calculation timed out.", e);
            }
        });
    }

    /**
     * Checks for planetary ingresses on the given date.
     *
     * @param dateTime The date and time to check.
     * @return A list of strings describing the ingresses.
     */
    private List<String> checkForIngresses(LocalDateTime dateTime) {
        List<String> ingresses = new ArrayList<>();

        // Example for the Sun
        LocalDateTime previousDay = dateTime.minusDays(1);
        double sunPosToday = getPlanetPosition(SweConst.SE_SUN, dateTime);
        double sunPosYesterday = getPlanetPosition(SweConst.SE_SUN, previousDay);

        ZodiacSign signToday = ZodiacSign.fromDegree(sunPosToday);
        ZodiacSign signYesterday = ZodiacSign.fromDegree(sunPosYesterday);

        if (!signToday.equals(signYesterday)) {
            ingresses.add("Sun enters " + signToday.getName());
        }

        // Repeat for other planets if desired

        return ingresses;
    }

    /**
     * Calculates the current planetary positions for a given date.
     *
     * @param date The desired date.
     * @return A map of planets and their positions in degrees.
     */
    public Map<Planet, Double> getCurrentPlanetPositions(LocalDateTime date) {
        double julianDay = calculateJulianDay(date);
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED;
        try {
            return epheExecutor.runWithTimeout(() -> {
                Map<Planet, Double> planetPositions = new HashMap<>();
                for (Planet planet : Planet.values()) {
                    double[] xpos = new double[6];
                    int ret = swe().swe_calc_ut(julianDay, planet.getSweConstValue(), flags, xpos, null);
                    if (ret == SweConst.OK) {
                        planetPositions.put(planet, xpos[0]); // xpos[0] is the position in degrees
                    }
                }
                return planetPositions;
            }, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Current planet positions calculation timed out.", e);
        }
    }

    /**
     * Calculates the aspects between planets in a single chart.
     *
     * @param planetPositions A map of planet names to their positions in degrees.
     * @return A list of {@link AspectDTO} describing the aspects.
     */
    public List<AspectDTO> calculateAspects(Map<String, Double> planetPositions,   List<Double> houseCusps) {
        List<AspectDTO> aspects = new ArrayList<>();
        List<String> planetNames = new ArrayList<>(planetPositions.keySet());

        for (int i = 0; i < planetNames.size(); i++) {
            String planet1 = planetNames.get(i);
            double pos1 = planetPositions.get(planet1);

            for (int j = i + 1; j < planetNames.size(); j++) {
                String planet2 = planetNames.get(j);
                double pos2 = planetPositions.get(planet2);

                double angle = calculateAspectAngle(pos1, pos2);
                AspectType aspectType = getAspectType(angle);
                if (aspectType != null) {
                    AspectDTO aspectDTO = new AspectDTO();
                    aspectDTO.setNatalPlanet(planet1);
                    aspectDTO.setTransitPlanet(planet2);
                    aspectDTO.setAspectType(aspectType.getDescription());
                    aspectDTO.setExactAngle(aspectType.getExactDegree());
                    aspectDTO.setOrb(Math.abs(angle - aspectType.getExactDegree()));
                    aspectDTO.setNatalSign(AstrologyUtils.interpretDegree(pos1));
                    aspectDTO.setTransitSign(AstrologyUtils.interpretDegree(pos2));
                    aspectDTO.setNatalHouse(AstrologyUtils.getHouseForDegree(pos1, houseCusps));
                    aspectDTO.setTransitHouse(AstrologyUtils.getHouseForDegree(pos2, houseCusps));

                    aspects.add(aspectDTO);
                }
            }
        }
        return aspects;
    }
    /**
     * Calculates the angular difference between two positions.
     *
     * @param pos1 The first position in degrees.
     * @param pos2 The second position in degrees.
     * @return The angular difference, adjusted to the range 0-180 degrees.
     */
    private double calculateAspectAngle(double pos1, double pos2) {
        double angle = Math.abs(pos1 - pos2);
        return angle > 180 ? 360 - angle : angle;
    }

    /**
     * Determines the aspect type based on the given angle.
     *
     * @param angle The angle between the planets.
     * @return The corresponding {@link AspectType} or null if no aspect is found.
     */
    private AspectType getAspectType(double angle) {
        for (AspectType aspect : AspectType.values()) {
            if (Math.abs(angle - aspect.getExactDegree()) <= aspect.getOrb()) {
                return aspect;
            }
        }
        return null;
    }


    /**
     * Determines the zodiac sign based on the given degree.
     *
     * @param degree The degree value (0° to 360°).
     * @return The corresponding ZodiacSign enum.
     */
    public ZodiacSign getZodiacSign(double degree) {
        return ZodiacSign.fromDegree(degree);
    }

    /**
     * Calculates the Julian Day based on the provided {@link LocalDateTime}.
     *
     * @param dateTime The {@link LocalDateTime} object for which the Julian Day is to be calculated.
     * @return The Julian Day as a {@code double}.
     */
    public double calculateJulianDay(LocalDateTime dateTime) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        int day = dateTime.getDayOfMonth();
        double hours = dateTime.getHour() + (dateTime.getMinute() / 60.0) + (dateTime.getSecond() / 3600.0);

        // Calculation of Julian Day using Swiss Ephemeris
        return new SweDate(year, month, day, hours).getJulDay();
    }

    /**
     * Converts a Julian Day to a LocalDateTime object.
     *
     * @param julianDayUT The Julian Day in UT.
     * @return The corresponding LocalDateTime object.
     */
    private LocalDateTime convertJulianDayToLocalDateTime(double julianDayUT) {
        // Create a SweDate object with the Julian date
        SweDate sweDate = new SweDate(julianDayUT);

        // Retrieve the date and time components
        int year = sweDate.getYear();
        int month = sweDate.getMonth();
        int day = sweDate.getDay();
        double hours = sweDate.getHour();

        // Convert the fractional hours into minutes and seconds
        int hour = (int) hours;
        double fractionalHour = hours - hour;
        int minute = (int) (fractionalHour * 60);
        double fractionalMinute = fractionalHour * 60 - minute;
        int second = (int) (fractionalMinute * 60);

        // Create a LocalDateTime object
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    /**
     * Calculates the current lunar phase for a given date.
     *
     * @param dateTime The desired date.
     * @return The name of the current lunar phase.
     */
    public String getLunarPhase(LocalDateTime dateTime) {
        double julianDay = calculateJulianDay(dateTime);
        long key = jdTo10MinKey(julianDay);
        return lunarPhaseCache.get(key, k -> {
            try {
                return epheExecutor.runWithTimeout(() -> {
                    double[] sunPos = new double[6];
                    double[] moonPos = new double[6];
                    StringBuffer serr = new StringBuffer();
                    int flags = SweConst.SEFLG_SWIEPH;

                    int sunResult  = swe().swe_calc_ut(julianDay, SweConst.SE_SUN,  flags, sunPos,  serr);
                    int moonResult = swe().swe_calc_ut(julianDay, SweConst.SE_MOON, flags, moonPos, serr);

                    if (sunResult == SweConst.ERR || moonResult == SweConst.ERR) {
                        return "Unknown";
                    }

                    double elongation = (moonPos[0] - sunPos[0] + 360) % 360;

                    // Key phases (New/Full/Quarter) centred on exact angle, 12° wide (~1 day even at apogee speed).
                    // Crescent/Gibbous fill the remaining arcs.
                    if (elongation < 6.0 || elongation >= 354.0)  return "New Moon";
                    if (elongation < 84.0)   return "Waxing Crescent";
                    if (elongation < 96.0)   return "First Quarter";
                    if (elongation < 174.0)  return "Waxing Gibbous";
                    if (elongation < 186.0)  return "Full Moon";
                    if (elongation < 264.0)  return "Waning Gibbous";
                    if (elongation < 276.0)  return "Last Quarter";
                    return "Waning Crescent";
                }, 3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Lunar phase calculation timed out.", e);
            }
        });
    }
    public PhaseAnalysisDTO calculatePhaseAnalysis(LocalDateTime start, LocalDateTime end,
                                                   double[] coordinates, HouseSystem houseSystem,
                                                   int intervalHours) {

        if (coordinates == null || coordinates.length != 2) {
            throw new IllegalArgumentException("Coordinates must contain exactly two values: [latitude, longitude]");
        }

        double latitude = coordinates[0];
        double longitude = coordinates[1];

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values.");
        }

        List<EphiDatenDTO> dataPoints = new ArrayList<>();
        LocalDateTime current = start;

        while (!current.isAfter(end)) {

            // Nur Transit-Snapshot (Planeten/Häuser/Asc-MC) — die Single-Chart-Aspekte der
            // Transitkarte werden hier nicht gebraucht (Natal-vs-Transit kommt später dazu).
            EphiDatenDTO dto = createTransitSnapshot(current, latitude, longitude, houseSystem);
            dto.setCoordinates(coordinates);
            dataPoints.add(dto);

            // nächsten Zeit-Schritt
            current = current.plusHours(intervalHours);
        }

        PhaseAnalysisDTO result = new PhaseAnalysisDTO();
        result.setDataPoints(dataPoints);
        return result;
    }

    /**
     * Lightweight transit snapshot for a single moment: planets, houses, Asc/MC and coordinates —
     * <b>without</b> the chart's own single-chart aspects. Use this for transit series (phase
     * analysis): those per-point single-chart aspects are otherwise computed but never used — the
     * meaningful aspects are natal-vs-transit and get added later in enrichPhaseAnalysisWithTransits.
     */
    public EphiDatenDTO createTransitSnapshot(LocalDateTime dateTime,
                                              double latitude,
                                              double longitude,
                                              HouseSystem houseSystem) {
        double julianDay = calculateJulianDay(dateTime);
        Map<String, Double> planets = calculatePlanetPositions(julianDay);
        HouseDataDTO houseData = calculateCusps(julianDay, latitude, longitude, houseSystem);

        EphiDatenDTO dto = new EphiDatenDTO();
        dto.setJulianDate(julianDay);
        dto.setPlanets(planets);
        dto.setCusps(houseData.getCusps());
        dto.setAscendant(houseData.getAscendant());
        dto.setMediumCoeli(houseData.getMediumCoeli());
        dto.setAscendantInterpretation(AstrologyUtils.interpretDegree(houseData.getAscendant()));
        dto.setMediumCoeliInterpretation(AstrologyUtils.interpretDegree(houseData.getMediumCoeli()));
        dto.setCoordinates(new double[]{latitude, longitude});
        return dto;
    }

    public EphiDatenDTO createEphiDatenDTO(LocalDateTime dateTime,
                                            double latitude,
                                            double longitude,
                                            HouseSystem houseSystem,
                                            HoroscopeType type) {
        // Full chart = transit snapshot + this chart's own single-chart aspects.
        EphiDatenDTO dto = createTransitSnapshot(dateTime, latitude, longitude, houseSystem);
        List<AspectDTO> aspects = calculateAspects(dto.getPlanets(), dto.getCusps());
        AstroUtils.filterRelevantAspects(aspects, type);
        dto.setAspects(aspects);
        return dto;
    }
    public List<ZodiacHoroscopeDTO> generateZodiacHoroscopes(LocalDateTime dateTime, double latitude, double longitude, HouseSystem houseSystem) {
        List<ZodiacHoroscopeDTO> results = new ArrayList<>();

        // Echte Transitdaten (Planeten/Häuser/Asc) nur EINMAL berechnen — leichter Snapshot ohne
        // Single-Chart-Aspekte, die pro Zeichen mit ersetzter Sonne ohnehin neu gerechnet werden.
        EphiDatenDTO base = createTransitSnapshot(dateTime, latitude, longitude, houseSystem);

        // Defensive Kopie: base.getPlanets() kann direkt aus dem Caffeine-Cache stammen. Diese Map
        // niemals mutieren, sonst überschreibt man den gecachten echten Sonnenstand.
        Map<String, Double> basePlanets = new HashMap<>(base.getPlanets());

        for (ZodiacSign sign : ZodiacSign.values()) {
            // Sonne auf 15° des aktuellen Zeichens
            double sunDegree = sign.getStartDegree() + 15.0;
            if (sunDegree >= 360.0) sunDegree -= 360;

            // Pro Zeichen eine eigene Planet-Map (Kopie) — kein geteilter State zwischen Ergebnissen/Cache.
            Map<String, Double> planets = new HashMap<>(basePlanets);
            planets.put("Sun", sunDegree);

            List<AspectDTO> aspects = calculateAspects(planets, base.getCusps());

            ZodiacHoroscopeDTO reduced = new ZodiacHoroscopeDTO();
            reduced.setZodiacSign(sign.getName());
            reduced.setSunDegree(sunDegree);
            reduced.setAscendant(base.getAscendantInterpretation());
            reduced.setAspects(aspects);
            reduced.setCusps(base.getCusps());
            reduced.setPlanets(planets);
            results.add(reduced);
        }
        return results;
    }
}
