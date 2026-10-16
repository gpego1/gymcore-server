package com.gymcore.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ProblemDetail problem(int status, String code, String message, HttpServletRequest request) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), message);
        p.setInstance(URI.create(request.getRequestURI()));
        p.setProperty("code", code);
        return p;
    }
    @ExceptionHandler(BusinessException.class)
    ProblemDetail business(BusinessException e, HttpServletRequest r) {
        return problem(e.getStatus(), e.getCode(), e.getMessage(), r);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        var p = problem(400, "VALIDATION_ERROR", "Revise os campos informados.", r);
        var fields = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        p.setProperty("fieldErrors", fields);
        return p;
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed(Exception e, HttpServletRequest r) {
        return problem(400, "INVALID_REQUEST", "JSON, enum, data ou parâmetro inválido.", r);
    }
    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ProblemDetail conflict(Exception e, HttpServletRequest r) {
        return problem(409, "PERSISTENCE_CONFLICT", "Duplicidade, vínculo inválido ou conflito concorrente. Consulte o estado atual antes de tentar novamente.", r);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail missing(Exception e, HttpServletRequest r) {
        return problem(404, "NOT_FOUND", "Endpoint não encontrado.", r);
    }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ProblemDetail method(Exception e, HttpServletRequest r) { return problem(405, "METHOD_NOT_ALLOWED", "Método não permitido.", r); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ProblemDetail media(Exception e, HttpServletRequest r) { return problem(415, "UNSUPPORTED_MEDIA_TYPE", "Use Content-Type application/json.", r); }
    @ExceptionHandler(DataAccessResourceFailureException.class)
    ProblemDetail unavailable(Exception e, HttpServletRequest r) {
        return problem(503, "DATABASE_UNAVAILABLE", "Persistência temporariamente indisponível.", r);
    }
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e, HttpServletRequest r) {
        LOG.error("Falha interna em {}", r.getRequestURI(), e);
        return problem(500, "INTERNAL_ERROR", "Falha interna ao processar a solicitação.", r);
    }
}
