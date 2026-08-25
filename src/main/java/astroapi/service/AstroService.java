package astroapi.service;

import astroapi.dto.*;
import astroapi.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swisseph.SweDate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service class responsible for performing various astrological calculations and analyses.
 * This includes generating horoscopes, analyzing planetary aspects, calculating transits,
 * and other related astrological tasks.
 *
 * <p>
 * The class integrates with the {@link SwissEphemerisHelper} for precise astronomical calculations,
 * {@link CompatibilityAnalyzer} for synastry and compatibility analyses, and
 * {@link TimeZoneService} for determining appropriate time zones based on geographic coordinates.
 * </p>
 */
@Service
public class AstroService {

    private final SwissEphemerisHelper swissEphemerisHelper;
    private final CompatibilityAnalyzer compatibilityAnalyzer;
    private final TimeZoneService timeZoneService;

    /**
     * Constructor for AstroService, initializing required dependencies.
     *
     * @param swissEphemerisHelper  Swiss Ephemeris helper for astronomical calculations.
     * @param timeZoneService       Service for resolving time zones by geographic location.
     * @param compatibilityAnalyzer Compatibility analyzer for relationship and synastry calculations.
     */
    @Autowired
    public AstroService(SwissEphemerisHelper swissEphemerisHelper, TimeZoneService timeZoneService, CompatibilityAnalyzer compatibilityAnalyzer) {
        this.swissEphemerisHelper = swissEphemerisHelper;
        this.timeZoneService = timeZoneService;
        this.compatibilityAnalyzer = compatibilityAnalyzer;
    }

    /**
     * Retrieves the current Moon position details based on the specified time zone.
     *
     * @param zoneId The time zone to use for the Moon's position calculation.
     * @return A {@link MoonDTO} object containing Moon position details and interpretations.
     */
    public MoonDTO getMoonPos(ZoneId zoneId) {
        return swissEphemerisHelper.getCurrentMoonPosDetails(zoneId, null);
    }

    public MoonDTO getMoonPos(ZoneId zoneId, java.time.LocalDateTime fixedDateTime) {
        return swissEphemerisHelper.getCurrentMoonPosDetails(zoneId, fixedDateTime);
    }

    /**
     * Calculates a localized horoscope for a given date, time, location, and house system.
     *
     * @param dateTime    The date and time for the horoscope.
     * @param coordinates An array containing the latitude and longitude of the location.
     * @param houseSystem The house system to use for the horoscope calculation.
     * @return An {@link EphiDatenDTO} containing horoscope data, including planetary positions and houses.
     * @throws IllegalArgumentException If the input coordinates are invalid or a time zone cannot be determined.
     */
    public EphiDatenDTO getLocalizedHoroscope(LocalDateTime dateTime,
                                              double[] coordinates,
                                              HouseSystem houseSystem,
                                              HoroscopeType horoscopeType) {
        // Local wall-clock time at the location → UT (Swiss Ephemeris / calculateJulianDay expect UT).
        LocalDateTime dateTimeUT = toUt(dateTime, coordinates);

        return swissEphemerisHelper.createEphiDatenDTO(
                dateTimeUT,
                coordinates[0],
                coordinates[1],
                houseSystem,
                horoscopeType
        );
    }

    /**
     * Resolves the IANA time zone for the given [latitude, longitude] coordinates.
     *
     * @throws IllegalArgumentException if the coordinates are invalid or no zone can be found.
     */
    private ZoneId resolveZone(double[] coordinates) {
        if (coordinates == null || coordinates.length != 2) {
            throw new IllegalArgumentException("Coordinates must contain exactly two values: [latitude, longitude]");
        }
        double latitude = coordinates[0];
        double longitude = coordinates[1];
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values.");
        }
        return timeZoneService.getTimeZone(latitude, longitude)
                .orElseThrow(() -> new IllegalArgumentException("No time zone found for the provided coordinates."));
    }

    /**
     * Interprets a local wall-clock time at the given coordinates and converts it to UT.
     * All Swiss Ephemeris calls (via {@code calculateJulianDay}) treat their LocalDateTime as UT,
     * so any request time that arrives as local time must be converted here first — otherwise
     * retrograde/lunar-phase/transit calculations are off by the location's UTC offset.
     */
    private LocalDateTime toUt(LocalDateTime localDateTime, double[] coordinates) {
        ZoneId zone = resolveZone(coordinates);
        return ZonedDateTime.of(localDateTime, zone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /**
     * Generates a synastry (compatibility) horoscope based on the birth data of two individuals.
     *
     * @param synastryRequest Contains birth data and coordinates of both individuals.
     * @return A {@link SynastryDTO} object with compatibility analysis results.
     */
   public SynastryDTO getPartnerHoroscope(SynastryRequest synastryRequest) {

            EphiDatenDTO partnerOne = getLocalizedHoroscope(
                    synastryRequest.getPartnerOne().getDateTime(),
                    synastryRequest.getPartnerOne().getCoordinates(),
                    HouseSystem.fromCode(synastryRequest.getPartnerOne().getHouseSystem()),
                    HoroscopeType.NATAL
            );

            EphiDatenDTO partnerTwo = getLocalizedHoroscope(
                    synastryRequest.getPartnerTwo().getDateTime(),
                    synastryRequest.getPartnerTwo().getCoordinates(),
                    HouseSystem.fromCode(synastryRequest.getPartnerTwo().getHouseSystem()),
                    HoroscopeType.NATAL
            );

            List<AspectDTO> synastricAspects = swissEphemerisHelper.calculateTransits(partnerOne, partnerTwo);

            SynastryDTO synastryDTO = new SynastryDTO();
            synastryDTO.setAspects(synastricAspects);

            CompatibilityResult result = compatibilityAnalyzer.analyzeCompatibility(partnerOne, partnerTwo);
            synastryDTO.setCompatibilityLevel(result.getCompatibilityLevel());
            synastryDTO.setRequestedHouseSystem(partnerOne.getRequestedHouseSystem());
            synastryDTO.setEffectiveHouseSystems(List.of(partnerOne.getEffectiveHouseSystem(), partnerTwo.getEffectiveHouseSystem()));
            synastryDTO.setHouseSystemFallback(partnerOne.isHouseSystemFallback() || partnerTwo.isHouseSystemFallback());

            return synastryDTO;
        }


    /**
     * Generates a composite horoscope by averaging the horoscopes of two individuals.
     *
     * @param compositeRequest Contains the necessary details for creating a composite horoscope.
     * @return A {@link CompositeHoroscopeDTO} object with composite horoscope data.
     */
    public CompositeHoroscopeDTO getCompositeHoroscope(CompositeRequest compositeRequest) {
        EphiDatenDTO partnerOne = getLocalizedHoroscope(
                compositeRequest.getPartnerOne().getDateTime(),
                compositeRequest.getPartnerOne().getCoordinates(),
                HouseSystem.fromCode(compositeRequest.getPartnerOne().getHouseSystem()),
                HoroscopeType.NATAL
        );

        EphiDatenDTO partnerTwo = getLocalizedHoroscope(
                compositeRequest.getPartnerTwo().getDateTime(),
                compositeRequest.getPartnerTwo().getCoordinates(),
                HouseSystem.fromCode(compositeRequest.getPartnerTwo().getHouseSystem()),
                HoroscopeType.NATAL
        );

        return swissEphemerisHelper.calculateCompositeHoroscope(
                partnerOne,
                partnerTwo,
                compositeRequest.getCompositeLatitude(),
                compositeRequest.getCompositeLongitude(),
                HouseSystem.fromCode(compositeRequest.getHouseSystem())
        );
    }

    /**
     * Generates a personalized daily horoscope using natal and transit data.
     *
     * @param dailyHoroscopeRequest Contains the natal data and the desired date for the horoscope.
     * @return A {@link DailyHoroscopeDTO} object with detailed horoscope information.
     */
    public DailyHoroscopeDTO getDailyHoroscope(DailyHoroscopeRequest dailyHoroscopeRequest) {
        DailyHoroscopeDTO dailyHoroscopeDTO = new DailyHoroscopeDTO();

        dailyHoroscopeDTO.setDate(dailyHoroscopeRequest.getDateTime().toLocalDate());
        dailyHoroscopeDTO.setTimeZone(ZoneId.systemDefault());

        EphiDatenDTO natalChart = getLocalizedHoroscope(
                dailyHoroscopeRequest.getHoroscope().getDateTime(),
                dailyHoroscopeRequest.getHoroscope().getCoordinates(),
                HouseSystem.fromCode(dailyHoroscopeRequest.getHoroscope().getHouseSystem()),
                HoroscopeType.NATAL
        );

        dailyHoroscopeDTO.setNatalPlanetPositions(natalChart.getPlanets());
        dailyHoroscopeDTO.setNatalHouseCusps(natalChart.getCuspsAsMap());
        dailyHoroscopeDTO.setNatalAscendant(natalChart.getAscendant());
        dailyHoroscopeDTO.setNatalMidheaven(natalChart.getMediumCoeli());

        EphiDatenDTO transitChart = getLocalizedHoroscope(
                dailyHoroscopeRequest.getDateTime(),
                dailyHoroscopeRequest.getCoordinates(),
                HouseSystem.fromCode(dailyHoroscopeRequest.getHouseSystem()),
                HoroscopeType.TRANSIT
        );

        dailyHoroscopeDTO.setTransitPlanetPositions(transitChart.getPlanets());
        dailyHoroscopeDTO.setTransitHouseCusps(transitChart.getCuspsAsMap());
        dailyHoroscopeDTO.setTransitAscendant(transitChart.getAscendant());
        dailyHoroscopeDTO.setTransitMidheaven(transitChart.getMediumCoeli());
        dailyHoroscopeDTO.setRequestedHouseSystem(transitChart.getRequestedHouseSystem());
        dailyHoroscopeDTO.setEffectiveHouseSystem(transitChart.getEffectiveHouseSystem());
        dailyHoroscopeDTO.setHouseSystemFallback(transitChart.isHouseSystemFallback());

        List<AspectDTO> aspects = swissEphemerisHelper.calculateTransits(natalChart, transitChart);
        AstroUtils.filterTransitAspects(aspects); // transit-specific orbs (not the wide general ones)
        dailyHoroscopeDTO.setAspects(aspects);

        LocalDateTime transitUt = toUt(dailyHoroscopeRequest.getDateTime(), dailyHoroscopeRequest.getCoordinates());
        dailyHoroscopeDTO.setRetrogradePlanets(swissEphemerisHelper.getRetrogradeStatuses(transitUt));
        dailyHoroscopeDTO.setLunarPhase(swissEphemerisHelper.getLunarPhase(transitUt));

        return dailyHoroscopeDTO;
    }
    public DailyHoroscopeDTO getDailyHoroscopeFromNatal(DailyHoroscopeFromNatalRequest request) {
        DailyHoroscopeDTO dto = new DailyHoroscopeDTO();
        dto.setDate(request.getDateTime().toLocalDate());
        dto.setTimeZone(ZoneId.systemDefault());

        EphiDatenDTO natalChart = request.getNatalChart();
        dto.setNatalPlanetPositions(natalChart.getPlanets());
        dto.setNatalHouseCusps(natalChart.getCuspsAsMap());
        dto.setNatalAscendant(natalChart.getAscendant());
        dto.setNatalMidheaven(natalChart.getMediumCoeli());

        EphiDatenDTO transitChart = getLocalizedHoroscope(
                request.getDateTime(),
                request.getCoordinates(),
                HouseSystem.fromCode(request.getHouseSystem()),
                HoroscopeType.TRANSIT
        );
        dto.setTransitPlanetPositions(transitChart.getPlanets());
        dto.setTransitHouseCusps(transitChart.getCuspsAsMap());
        dto.setTransitAscendant(transitChart.getAscendant());
        dto.setTransitMidheaven(transitChart.getMediumCoeli());
        dto.setRequestedHouseSystem(transitChart.getRequestedHouseSystem());
        dto.setEffectiveHouseSystem(transitChart.getEffectiveHouseSystem());
        dto.setHouseSystemFallback(transitChart.isHouseSystemFallback());

        List<AspectDTO> aspects = swissEphemerisHelper.calculateTransits(natalChart, transitChart);
        AstroUtils.filterTransitAspects(aspects); // transit-specific orbs (not the wide general ones)
        dto.setAspects(aspects);
        LocalDateTime transitUt = toUt(request.getDateTime(), request.getCoordinates());
        dto.setRetrogradePlanets(swissEphemerisHelper.getRetrogradeStatuses(transitUt));
        dto.setLunarPhase(swissEphemerisHelper.getLunarPhase(transitUt));

        return dto;
    }

    /**
     * Berechnet die Phasenanalyse inkl. Geburtshoroskop. Anschließend werden für jeden Datenpunkt
     * Transit-Aspekte, Retrograde etc. gegen das Geburtshoroskop berechnet.
     */
    public PhaseAnalysisDTO calculatePhaseAnalysisWithBirthData(PhaseAnalysisWithBirthDataRequest request) {
        // 1. Geburtshoroskop berechnen
        EphiDatenDTO natalChart = getLocalizedHoroscope(
                request.getBirthDateTime(),
                request.getBirthCoordinates(),
                HouseSystem.fromCode(request.getBirthHouseSystem()),
                HoroscopeType.PHASE
        );
        // 2. PhaseAnalysis berechnen (Transit-Daten ohne Bezug zu Natal).
        //    Start/Ende sind lokale Zeiten am Transit-Ort → auf UT umrechnen, damit die
        //    Ephemeriden-Rechnung (und die daran hängenden Retrograde/Lunar-Werte) stimmen.
        LocalDateTime startUt = toUt(request.getStartDateTime(), request.getCoordinates());
        LocalDateTime endUt = toUt(request.getEndDateTime(), request.getCoordinates());
        PhaseAnalysisDTO baseAnalysis = swissEphemerisHelper.calculatePhaseAnalysis(
                startUt,
                endUt,
                request.getCoordinates(),
                HouseSystem.fromCode(request.getHouseSystem()),
                request.getIntervalHours()
        );

        // 3. Transit-Aspekte, Retrograde, LunarPhase, Events berechnen (gleiche UT-Basis wie oben)
        List<PhaseDataEntryDTO> transitEntries = enrichPhaseAnalysisWithTransits(natalChart, baseAnalysis, startUt, request.getIntervalHours());

        // 4. Ergebnis zurückgeben
        baseAnalysis.setTransits(transitEntries);
        return baseAnalysis;
    }

    /**
     * Berechnet die Phasenanalyse auf Basis eines bereits vorhandenen NatalChart (wird mitgeliefert).
     * Anschließend werden Transit-Aspekte, Retrograde etc. dazu berechnet.
     */
    public PhaseAnalysisDTO calculatePhaseAnalysisWithNatalDTO(PhaseAnalysisWithNatalDTORequest request) {
        EphiDatenDTO natalChart = request.getNatalChart();
        // Lokale Start/Ende-Zeiten am Transit-Ort → UT (siehe calculatePhaseAnalysisWithBirthData).
        LocalDateTime startUt = toUt(request.getStartDateTime(), request.getCoordinates());
        LocalDateTime endUt = toUt(request.getEndDateTime(), request.getCoordinates());
        PhaseAnalysisDTO baseAnalysis = swissEphemerisHelper.calculatePhaseAnalysis(
                startUt,
                endUt,
                request.getCoordinates(),
                HouseSystem.fromCode(request.getHouseSystem()),
                request.getIntervalHours()
        );

        List<PhaseDataEntryDTO> transitEntries = enrichPhaseAnalysisWithTransits(natalChart, baseAnalysis, startUt, request.getIntervalHours());

        baseAnalysis.setTransits(transitEntries);
        return baseAnalysis;
    }

    /**
     * Hilfsmethode zur Anreicherung der PhaseAnalysis mit Transit-Daten (Aspekte etc.).
     *
     * @param natalChart Das Geburtshoroskop
     * @param baseAnalysis Das Basisergebnis der PhaseAnalysis (nur EphiDatenDTO)
     * @param start Start-Zeitpunkt
     * @param intervalHours Interval in Stunden
     * @return Liste von PhaseDataEntryDTO mit allen notwendigen Zusatzinformationen
     */
    private List<PhaseDataEntryDTO> enrichPhaseAnalysisWithTransits(EphiDatenDTO natalChart, PhaseAnalysisDTO baseAnalysis,
                                                                    LocalDateTime start, int intervalHours) {
        List<PhaseDataEntryDTO> transitEntries = new ArrayList<>();
        LocalDateTime current = start;
        for (EphiDatenDTO ephiDaten : baseAnalysis.getDataPoints()) {
            PhaseDataEntryDTO entry = new PhaseDataEntryDTO();
            entry.setTransitData(ephiDaten);
            // Aspekte zwischen natal und transit
            List<AspectDTO> aspects = swissEphemerisHelper.calculateTransits(natalChart, ephiDaten);
            AstroUtils.filterTransitAspects(aspects); // transit-specific orbs (not the wide general ones)
            entry.setAspects(aspects);
            // Retrograde-Status für den aktuellen Zeitpunkt
            Map<String, Boolean> retrogradePlanets = swissEphemerisHelper.getRetrogradeStatuses(current);
            entry.setRetrogradePlanets(retrogradePlanets);

            // LunarPhase für den aktuellen Zeitpunkt
            String lunarPhase = swissEphemerisHelper.getLunarPhase(current);
            entry.setLunarPhase(lunarPhase);

            transitEntries.add(entry);

            current = current.plusHours(intervalHours);
        }

        return transitEntries;
    }
    /**
     * Calculates the PhaseAnalysis with birth data and then converts it to a PhaseAnalysisShortDTO,
     * which contains only a subset of the data.
     */
    public PhaseAnalysisShortDTO calculatePhaseAnalysisWithBirthDataShort(PhaseAnalysisWithBirthDataRequest request) {
        PhaseAnalysisDTO fullResult = calculatePhaseAnalysisWithBirthData(request);
        return convertToShortDTO(fullResult);
    }

    /**
     * Calculates the PhaseAnalysis with a given natal chart (already provided in the request)
     * and then converts it to a PhaseAnalysisShortDTO.
     */
    public PhaseAnalysisShortDTO calculatePhaseAnalysisWithNatalDTOShort(PhaseAnalysisWithNatalDTORequest request) {
        PhaseAnalysisDTO fullResult = calculatePhaseAnalysisWithNatalDTO(request);
        return convertToShortDTO(fullResult);
    }

    /**
     * Helper method to convert the full PhaseAnalysisDTO to a PhaseAnalysisShortDTO,
     * reducing the data from PhaseDataEntryDTO to PhaseDataEntryShortDTO.
     */
    private PhaseAnalysisShortDTO convertToShortDTO(PhaseAnalysisDTO fullResult) {
        PhaseAnalysisShortDTO shortDTO = new PhaseAnalysisShortDTO();
        List<PhaseDataEntryShortDTO> shortEntries = new ArrayList<>();

        if (fullResult.getTransits() != null) {
            for (PhaseDataEntryDTO entry : fullResult.getTransits()) {
                PhaseDataEntryShortDTO shortEntry = new PhaseDataEntryShortDTO();
                shortEntry.setAspects(entry.getAspects());
                shortEntry.setRetrogradePlanets(entry.getRetrogradePlanets());

                shortEntries.add(shortEntry);
            }
        }

        shortDTO.setTransits(shortEntries);
        return shortDTO;
    }

    public List<ZodiacHoroscopeDTO> getGeneralZodiacHoroscopes(LocalDateTime dateTime, double[] coordinates, HouseSystem houseSystem) {
        return swissEphemerisHelper.generateZodiacHoroscopes(
                dateTime,
                coordinates[0],
                coordinates[1],
                houseSystem
        );
    }
}
