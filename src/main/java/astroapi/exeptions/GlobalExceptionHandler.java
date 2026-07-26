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