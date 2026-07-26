package astroapi.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class DailyHoroscopeDTO {
    // Date and Time Information
    private LocalDate date;
    private ZoneId timeZone;

    // Natal Chart Data
    private Map<String, Double> natalPlanetPositions;
    private Map<Integer, Double> natalHouseCusps;
    private Double natalAscendant;
    private Double natalMidheaven;

    // Transit Data
    private Map<String, Double> transitPlanetPositions;
    private Map<Integer, Double> transitHouseCusps;
    private Double transitAscendant;
    private Double transitMidheaven;

    // Calculated Aspects
    private List<AspectDTO> aspects;

    // Additional Data
    private Map<String, Boolean> retrogradePlanets;
    private String lunarPhase;

    // Metadata
    private String calculationSettings;

    // Getters and Setters

    // Date and Time Information
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone;
    }

    // Natal Chart Data
    public Map<String, Double> getNatalPlanetPositions() {
        return natalPlanetPositions;
    }

    public void setNatalPlanetPositions(Map<String, Double> natalPlanetPositions) {
        this.natalPlanetPositions = natalPlanetPositions;
    }

    public Map<Integer, Double> getNatalHouseCusps() {
        return natalHouseCusps;
    }

    public void setNatalHouseCusps(Map<Integer, Double> natalHouseCusps) {
        this.natalHouseCusps = natalHouseCusps;
    }

    public Double getNatalAscendant() {
        return natalAscendant;
    }

    public void setNatalAscendant(Double natalAscendant) {
        this.natalAscendant = natalAscendant;
    }

    public Double getNatalMidheaven() {
        return natalMidheaven;
    }

    public void setNatalMidheaven(Double natalMidheaven) {
        this.natalMidheaven = natalMidheaven;
    }

    // Transit Data
    public Map<String, Double> getTransitPlanetPositions() {
        return transitPlanetPositions;
    }

    public void setTransitPlanetPositions(Map<String, Double> transitPlanetPositions) {
        this.transitPlanetPositions = transitPlanetPositions;
    }

    public Map<Integer, Double> getTransitHouseCusps() {
        return transitHouseCusps;
    }

    public void setTransitHouseCusps(Map<Integer, Double> transitHouseCusps) {
        this.transitHouseCusps = transitHouseCusps;
    }

    public Double getTransitAscendant() {
        return transitAscendant;
    }

    public void setTransitAscendant(Double transitAscendant) {
        this.transitAscendant = transitAscendant;
    }

    public Double getTransitMidheaven() {
        return transitMidheaven;
    }

    public void setTransitMidheaven(Double transitMidheaven) {
        this.transitMidheaven = transitMidheaven;
    }

    // Calculated Aspects
    public List<AspectDTO> getAspects() {
        return aspects;
    }

    public void setAspects(List<AspectDTO> aspects) {
        this.aspects = aspects;
    }

    // Additional Data
    public Map<String, Boolean> getRetrogradePlanets() {
        return retrogradePlanets;
    }

    public void setRetrogradePlanets(Map<String, Boolean> retrogradePlanets) {
        this.retrogradePlanets = retrogradePlanets;
    }

    public String getLunarPhase() {
        return lunarPhase;
    }

    public void setLunarPhase(String lunarPhase) {
        this.lunarPhase = lunarPhase;
    }

    // Metadata
    public String getCalculationSettings() {
        return calculationSettings;
    }

    public void setCalculationSettings(String calculationSettings) {
        this.calculationSettings = calculationSettings;
    }
}
