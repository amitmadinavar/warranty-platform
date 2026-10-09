package com.wp;
import org.springframework.dao.DataIntegrityViolationException; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; import java.util.Map;
@RestControllerAdvice
public class Errors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> a(ResponseStatusException e){ return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason())); }
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> b(Exception e){ return ResponseEntity.badRequest().body(Map.of("error","Duplicate or invalid data")); }
}
