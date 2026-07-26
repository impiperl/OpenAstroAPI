package astroapi.dto;

import java.util.List;

public class HouseDataDTO {
    private List<Double> cusps;
    private double ascendant;
    private double mediumCoeli;


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


}
