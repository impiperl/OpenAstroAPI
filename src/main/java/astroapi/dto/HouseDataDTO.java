package astroapi.dto;

import java.util.List;

public class HouseDataDTO {
    private List<Double> cusps;
    private double ascendant;
    private double mediumCoeli;
    private String requestedHouseSystem;
    private String effectiveHouseSystem;
    private boolean houseSystemFallback;


    public HouseDataDTO() {
    }

    public void setMediumCoeli(double mediumCoeli) {
        this.mediumCoeli = mediumCoeli;
    }

    public double getMediumCoeli() {
        return mediumCoeli;
    }

    public double getAscendant() {
        return ascendant;
    }

    public void setAscendant(double ascendant) {
        this.ascendant = ascendant;
    }
    public List<Double> getCusps() {
        return cusps;
    }

    public void setCusps(List<Double> cusps) {
        this.cusps = cusps;
    }
    public String getRequestedHouseSystem() { return requestedHouseSystem; }
    public void setRequestedHouseSystem(String value) { this.requestedHouseSystem = value; }
    public String getEffectiveHouseSystem() { return effectiveHouseSystem; }
    public void setEffectiveHouseSystem(String value) { this.effectiveHouseSystem = value; }
    public boolean isHouseSystemFallback() { return houseSystemFallback; }
    public void setHouseSystemFallback(boolean value) { this.houseSystemFallback = value; }


}
