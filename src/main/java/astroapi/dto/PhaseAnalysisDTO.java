package astroapi.dto;

import java.util.List;

public class PhaseAnalysisDTO {
    private List<EphiDatenDTO> dataPoints;
    private List<PhaseDataEntryDTO> transits;
    public PhaseAnalysisDTO() {}

    public PhaseAnalysisDTO(List<EphiDatenDTO> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public List<EphiDatenDTO> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<EphiDatenDTO> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public void setTransits(List<PhaseDataEntryDTO> transits) {
        this.transits = transits;
    }

    public List<PhaseDataEntryDTO> getTransits() {
        return transits;
    }
}
