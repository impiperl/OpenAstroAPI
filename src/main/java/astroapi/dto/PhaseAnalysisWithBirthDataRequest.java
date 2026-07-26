package astroapi.dto;

import java.time.LocalDateTime;

public class PhaseAnalysisWithBirthDataRequest {
    private LocalDateTime birthDateTime;
    private double[] birthCoordinates; // Geburtsort
    private char birthHouseSystem;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private double[] coordinates; // [latitude, longitude]
    private char houseSystem;
    private int intervalHours; // z.B. alle 6 Stunden, 12 Stunden etc.

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public char getHouseSystem() {
        return houseSystem;
    }

    public void setHouseSystem(char houseSystem) {
        this.houseSystem = houseSystem;
    }

    public int getIntervalHours() {
        return intervalHours;
    }

    public void setIntervalHours(int intervalHours) {
        this.intervalHours = intervalHours;
    }

    public LocalDateTime getBirthDateTime() {
        return birthDateTime;
    }

    public void setBirthDateTime(LocalDateTime birthDateTime) {
        this.birthDateTime = birthDateTime;
    }

    public char getBirthHouseSystem() {
        return birthHouseSystem;
    }

    public void setBirthHouseSystem(char birthHouseSystem) {
        this.birthHouseSystem = birthHouseSystem;
    }

    public void setBirthCoordinates(double[] birthCoordinates) {
        this.birthCoordinates = birthCoordinates;
    }

    public double[] getBirthCoordinates() {
        return birthCoordinates;
    }
}
