package astroapi.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.validation.constraints.NotNull;

public class MoonRequest {

    @NotNull(message = "zoneId must not be null")
    private ZoneId zoneId;

    // optional — if set, calculate for this moment instead of now()
    private LocalDateTime dateTime;

    public ZoneId getZoneId() { return zoneId; }
    public void setZoneId(ZoneId zoneId) { this.zoneId = zoneId; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
}
