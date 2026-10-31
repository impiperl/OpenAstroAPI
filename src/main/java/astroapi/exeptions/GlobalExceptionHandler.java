package astroapi.exeptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Kein Sonnenaufgang am gefragten Ort — jenseits der Polarkreise der Normalfall.
     *
     * <p>422 statt 500: Die Anfrage war gültig, die Antwort ist nur an diesem Ort nicht
     * berechenbar. Der Code ist maschinenlesbar, damit eine Oberfläche den Fall benennen kann,
     * ohne die Meldung zu zerlegen.
     */
    @ExceptionHandler(NoSunriseException.class)
    public ResponseEntity<Object> handleNoSunrise(NoSunriseException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "no_sunrise");
        body.put("message", ex.getMessage());
        body.put("latitude", ex.getLatitude());
        body.put("longitude", ex.getLongitude());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    /**
     * The Swiss Ephemeris worker pool shed load (bounded queue stayed full past
     * the wait window). Signal the client to retry rather than failing as a 500.
     */
    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<Object> handleOverload(RejectedExecutionException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Service temporarily overloaded, please retry.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "1")
                .body(body);
    }
}
