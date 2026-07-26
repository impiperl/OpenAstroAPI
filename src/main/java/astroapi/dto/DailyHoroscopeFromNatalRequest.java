package astroapi.dto;

import java.time.LocalDateTime;

public class DailyHoroscopeFromNatalRequest {

    private EphiDatenDTO natalChart;
    private LocalDateTime dateTime;
    private double[] coordinates;
    private char houseSystem;

    public EphiDatenDTO getNatalChart() { return natalChart; }
    public void setNatalChart(EphiDatenDTO natalChart) { this.natalChart = natalChart; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public double[] getCoordinates() { return coordinates; }
    public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }

    public char getHouseSystem() { return houseSystem; }
    public void setHouseSystem(char houseSystem) { this.houseSystem = houseSystem; }
}
