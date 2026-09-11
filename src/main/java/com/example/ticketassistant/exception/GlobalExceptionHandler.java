package com.example.ticketassistant.exception;
import com.example.ticketassistant.dto.ErrorResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TicketNotFoundException.class) ResponseEntity<ErrorResponse> notFound(TicketNotFoundException e) { return error(HttpStatus.NOT_FOUND,"TICKET_NOT_FOUND","The requested ticket does not exist."); }
    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class}) ResponseEntity<ErrorResponse> invalidRequest(Exception e) { return error(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Request fields are missing or invalid."); }
    @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected(Exception e) { return error(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","An unexpected error occurred."); }
    private ResponseEntity<ErrorResponse> error(HttpStatus status,String error,String message) { return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(),status.value(),error,message)); }
}
