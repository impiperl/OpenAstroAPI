package astroapi.dto;

import java.util.List;
import java.util.Map;

public class PhaseDataEntryDTO {
    private EphiDatenDTO transitData;
    private List<AspectDTO> aspects;
    private Map<String, Boolean> retrogradePlanets;
    private String lunarPhase;
    private List<String> significantEvents;

    public EphiDatenDTO getTransitData() {
        return transitData;
    }

    public void setTransitData(EphiDatenDTO transitData) {
        this.transitData = transitData;
    }

    public List<AspectDTO> getAspects() {
        return aspects;
    }

    public void setAspects(List<AspectDTO> aspects) {
        this.aspects = aspects;
    }

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

    public List<String> getSignificantEvents() {
        return significantEvents;
    }

    public void setSignificantEvents(List<String> significantEvents) {
        this.significantEvents = significantEvents;
    }
}
