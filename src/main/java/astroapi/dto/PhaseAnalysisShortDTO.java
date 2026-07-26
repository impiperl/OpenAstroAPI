package astroapi.dto;

import java.util.List;

public class PhaseAnalysisShortDTO {
    private List<PhaseDataEntryShortDTO> transits;
    public PhaseAnalysisShortDTO() {}

    public void setTransits(List<PhaseDataEntryShortDTO> transits) {
        this.transits = transits;
    }

    public List<PhaseDataEntryShortDTO> getTransits() {
        return transits;
    }
}
