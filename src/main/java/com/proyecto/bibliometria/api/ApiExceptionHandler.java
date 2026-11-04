package com.proyecto.bibliometria.api;

import java.util.NoSuchElementException;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail noEncontrado(NoSuchElementException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class
    })
    ProblemDetail solicitudInvalida(Exception exception) {
        String detalle = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .distinct()
                        .collect(java.util.stream.Collectors.joining("; "))
                : "La solicitud contiene datos inválidos.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail algoritmoNoDisponible(IllegalStateException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No fue posible ejecutar el algoritmo solicitado. Verifica los archivos de embeddings configurados.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail errorHttp(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }
}
