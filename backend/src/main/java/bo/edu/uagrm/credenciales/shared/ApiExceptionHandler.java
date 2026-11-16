package bo.edu.uagrm.credenciales.shared;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({
        org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        jakarta.persistence.OptimisticLockException.class,
        org.springframework.dao.PessimisticLockingFailureException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> concurrentUpdate(RuntimeException failure) {
        return error("El registro cambió durante la operación. Actualiza e inténtalo otra vez.");
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    Map<String, Object> uploadTooLarge() {
        return error(
                "El envío supera el tamaño permitido. Reduce las imágenes e inténtalo de nuevo.");
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    org.springframework.http.ResponseEntity<Map<String, Object>> status(
            org.springframework.web.server.ResponseStatusException exception) {
        String message =
                switch (exception.getStatusCode().value()) {
                    case 404 -> "El registro no está disponible.";
                    case 403 -> "Tu cuenta no tiene acceso a esta operación.";
                    case 409 -> "Los datos entran en conflicto con otro registro.";
                    default -> "No se pudo completar la solicitud.";
                };
        return org.springframework.http.ResponseEntity.status(exception.getStatusCode())
                .body(error(message));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> duplicate(
            org.springframework.dao.DataIntegrityViolationException exception) {
        return error("Los datos ya están registrados o entran en conflicto con otro registro.");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> badRequest(RuntimeException exception) {
        return error(exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    Map<String, Object> unauthorized(BadCredentialsException exception) {
        return error(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> validation(MethodArgumentNotValidException exception) {
        String message =
                exception.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .orElse("Datos inválidos");
        return error(message);
    }

    private Map<String, Object> error(String message) {
        return Map.of(
                "timestamp",
                Instant.now(),
                "message",
                message == null ? "Error inesperado" : message);
    }
}
