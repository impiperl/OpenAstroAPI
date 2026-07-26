package astroapi.dto;

import java.util.List;
import java.util.Map;

public class ZodiacHoroscopeDTO {
    private Map<String, Double> planets;
    private String zodiacSign;
    private double sunDegree;
    private String ascendant;
    private List<AspectDTO> aspects;
    private List<Double> cusps;

    // Getter & Setter
    public String getZodiacSign() {
        return zodiacSign;
    }
    public void setZodiacSign(String zodiacSign) {
        this.zodiacSign = zodiacSign;
    }

    public double getSunDegree() {
        return sunDegree;
    }
    public void setSunDegree(double sunDegree) {
        this.sunDegree = sunDegree;
    }

    public String getAscendant() {
        return ascendant;
    }
    public void setAscendant(String ascendant) {
        this.ascendant = ascendant;
    }

    public List<AspectDTO> getAspects() {
        return aspects;
    }
    public void setAspects(List<AspectDTO> aspects) {
        this.aspects = aspects;
    }
    public List<Double> getCusps() {
        return cusps;
    }
    public void setCusps(List<Double> cusps) {
        this.cusps = cusps;
    }
    public Map<String, Double> getPlanets() {
        return planets;
    }

    public void setPlanets(Map<String, Double> planets) {
        this.planets = planets;
    }
}