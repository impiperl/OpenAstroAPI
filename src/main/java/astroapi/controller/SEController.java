package astroapi.controller;

import astroapi.dto.*;
import astroapi.service.AstroService;
import astroapi.utils.HoroscopeType;
import astroapi.utils.HouseSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST Controller that provides endpoints for various astrological calculations.
 * <p>
 * This class exposes APIs to compute localized horoscopes, moon positions, partner compatibility,
 * composite horoscopes, daily horoscopes, transits, progressions, aspects, planetary configurations,
 * and asteroid positions.
 * </p>
 */
@RestController
@RequestMapping("/api")
public class SEController {

    @Autowired
    private AstroService astroService;

    /**
     * Endpoint to calculate a localized horoscope.
     *
     * @param astroRequest The {@link AstroRequest} containing the date, location, and house system.
     * @return An {@link EphiDatenDTO} containing the horoscope data.
     */
    @PostMapping("/localizedHoroscope")
    public EphiDatenDTO getLocalizedHoroscope(@Valid @RequestBody AstroRequest astroRequest) {
        char houseSystemCode = astroRequest.getHouseSystem();
        HouseSystem houseSystem = HouseSystem.fromCode(houseSystemCode);
        return astroService.getLocalizedHoroscope(astroRequest.getDateTime(), astroRequest.getCoordinates(), houseSystem, HoroscopeType.TRANSIT);
    }
    @PostMapping("/phaseAnalysisWithBirthData")
    public PhaseAnalysisDTO getPhaseAnalysisWithBirthData(@Valid @RequestBody PhaseAnalysisWithBirthDataRequest request) {
        return astroService.calculatePhaseAnalysisWithBirthData(request);
    }

    @PostMapping("/phaseAnalysisWithNatalDTO")
    public PhaseAnalysisDTO getPhaseAnalysisWithNatalDTO(@Valid @RequestBody PhaseAnalysisWithNatalDTORequest request) {
        return astroService.calculatePhaseAnalysisWithNatalDTO(request);
    }
    @PostMapping("/phaseAnalysisWithBirthDataShort")
    public PhaseAnalysisShortDTO getPhaseAnalysisWithBirthDataShort(@Valid @RequestBody PhaseAnalysisWithBirthDataRequest request) {
        return astroService.calculatePhaseAnalysisWithBirthDataShort(request);
    }

    @PostMapping("/phaseAnalysisWithNatalDTOShort")
    public PhaseAnalysisShortDTO getPhaseAnalysisWithNatalDTOShort(@Valid @RequestBody PhaseAnalysisWithNatalDTORequest request) {
        return astroService.calculatePhaseAnalysisWithNatalDTOShort(request);
    }
    /**
     * Endpoint to calculate the current Moon position and its interpretation.
     *
     * @param moonRequest The {@link MoonRequest} containing the time zone.
     * @return A {@link MoonDTO} with the Moon data and its interpretation.
     */
    @PostMapping("/moonpos")
    public MoonDTO getMoonPos(@Valid @RequestBody MoonRequest moonRequest) {
        return astroService.getMoonPos(moonRequest.getZoneId(), moonRequest.getDateTime());
    }

    /**
     * Endpoint to calculate a partner horoscope (synastry).
     *
     * @param synastryRequest The {@link SynastryRequest} containing the astrological data of both partners.
     * @return A {@link SynastryDTO} with compatibility data.
     */
    @PostMapping("/partnerHoroscope")
    public SynastryDTO getPartnerHoroscope(@Valid @RequestBody SynastryRequest synastryRequest) {
        return astroService.getPartnerHoroscope(synastryRequest);
    }

    /**
     * Endpoint to calculate a composite horoscope.
     *
     * @param compositeRequest The {@link CompositeRequest} containing the astrological data of two individuals.
     * @return A {@link CompositeHoroscopeDTO} with the combined horoscope data.
     */
    @PostMapping("/compositeHoroscope")
    public CompositeHoroscopeDTO getCompositeHoroscope(@Valid @RequestBody CompositeRequest compositeRequest) {
        return astroService.getCompositeHoroscope(compositeRequest);
    }

    /**
     * Endpoint to calculate a personalized daily horoscope based on the Sun sign and the date.
     *
     * @param dailyHoroscopeRequest The {@link DailyHoroscopeRequest} containing the Sun sign and the date.
     * @return A {@link DailyHoroscopeDTO} with the daily prediction.
     */
    @PostMapping("/dailyHoroscope")
    public DailyHoroscopeDTO getDailyHoroscope(@Valid @RequestBody DailyHoroscopeRequest dailyHoroscopeRequest) {
        return astroService.getDailyHoroscope(dailyHoroscopeRequest);
    }

    @PostMapping("/dailyHoroscopeFromNatal")
    public DailyHoroscopeDTO getDailyHoroscopeFromNatal(@RequestBody DailyHoroscopeFromNatalRequest request) {
        return astroService.getDailyHoroscopeFromNatal(request);
    }

    @PostMapping("/general-zodiac-horoscopes")
    public List<ZodiacHoroscopeDTO> getGeneralHoroscopes(@RequestBody AstroRequest request) {
        HouseSystem houseSystem = HouseSystem.fromCode(request.getHouseSystem());
        List<ZodiacHoroscopeDTO> results = astroService.getGeneralZodiacHoroscopes(
                request.getDateTime(),
                request.getCoordinates(),
                houseSystem
        );
        return results;
    }
}
