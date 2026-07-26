package astroapi.dto;

import java.util.List;
import java.util.Map;

public class PhaseDataEntryShortDTO {
    private List<AspectDTO> aspects;
    private Map<String, Boolean> retrogradePlanets;

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
}
