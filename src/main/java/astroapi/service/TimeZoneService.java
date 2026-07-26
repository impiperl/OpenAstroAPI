package astroapi.service;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Optional;

/**
 * Service class for determining time zones based on geographic coordinates.
 * Utilizes the TimeZoneEngine library for fast and accurate time zone queries.
 */
@Service
public class TimeZoneService {

    private final TimeZoneEngine engine;

    /**
     * Initializes the TimeZoneService and the TimeZoneEngine.
     * The engine is preloaded with time zone data for efficient queries.
     */
    public TimeZoneService() {
        this.engine = TimeZoneEngine.initialize();
    }

    /**
     * Determines the time zone based on the provided latitude and longitude.
     *
     * @param latitude  The geographic latitude in decimal degrees.
     * @param longitude The geographic longitude in decimal degrees.
     * @return An Optional containing the ZoneId if a time zone is found, or an empty Optional if no time zone is available for the coordinates.
     */
    public Optional<ZoneId> getTimeZone(double latitude, double longitude) {
        return engine.query(latitude, longitude);
    }
}
