package com.kousenit.demo.controllers;

import com.kousenit.demo.dto.ApiError;
import com.kousenit.demo.dto.ValidationError;
import com.kousenit.demo.exceptions.InsufficientStockException;
import com.kousenit.demo.exceptions.ProductNotFoundException;
import com.kousenit.demo.exceptions.ProductValidationException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandlers {
    private static final String VALIDATION_FAILED = "Validation failed";

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProductNotFoundException(
            ProductNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage()
        );
        problemDetail.setType(
                URI.create("http://api.product.shopping/problems/product-not-found"));
        problemDetail.setTitle("Product not found");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        if(ex.getProductId() != null){
            problemDetail.setProperty("ProductId", ex.getProductId());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientStockException(
            InsufficientStockException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage()
        );
        problemDetail.setType(URI.create("http://api.product.shopping/problems/insufficient-stock"));
        problemDetail.setTitle("Insufficient Stock");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("productId", ex.getProductId());
        problemDetail.setProperty("requestedQuantity", ex.getRequestedQuantity());
        problemDetail.setProperty("availableQuantity", ex.getAvailableQuantity());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(ProductValidationException.class)
    public ResponseEntity<ProblemDetail> handleProductValidationException(
            ProductValidationException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage()
        );
        problemDetail.setType(URI.create("http://api.product.shopping/problems/validation-error"));
        problemDetail.setTitle("Validation error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        if(ex.getField() != null) {
            problemDetail.setProperty("field", ex.getField());
            problemDetail.setProperty("value", ex.getValue());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldErrors)
                .collect(Collectors.toList());

        ex.getBindingResult().getGlobalErrors().forEach(objectError ->
                validationErrors.add(ValidationError.of(
                        objectError.getObjectName(),
                        null,
                        objectError.getDefaultMessage(),
                        objectError.getCode()
                )));

        ApiError apiError = ApiError.withValidationErrors(
                HttpStatus.BAD_REQUEST,
                VALIDATION_FAILED,
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ValidationError> constraintViolations = ex.getConstraintViolations()
                .stream()
                .map(this::mapConstraintViolations)
                .collect(Collectors.toList());
        ApiError apiError = ApiError.withValidationErrors(
                HttpStatus.BAD_REQUEST,
                VALIDATION_FAILED,
                request.getRequestURI(),
                constraintViolations
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "Data integrity violation. This operation conflicts with existing data constraints.";
        String type = "https://api.shopping.com/problems/data-integrity";
        String title = "Data Integrity Violation";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, message);
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Malformed JSON request");
        problemDetail.setType(URI.create("https://api.shopping.com/problems/malformed-request"));
        problemDetail.setTitle("Malformed Request");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        assert ex.getRequiredType() != null;
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, message);
        problemDetail.setType(URI.create("https://api.shopping.com/problems/type-mismatch"));
        problemDetail.setTitle("Type Mismatch");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("parameter", ex.getName());
        problemDetail.setProperty("rejectedValue", ex.getValue());
        problemDetail.setProperty("expectedType", ex.getRequiredType().getSimpleName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }



    public ValidationError mapFieldErrors(FieldError fieldError){
        return ValidationError.of(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage(),
                fieldError.getCode()
        );
    }
    public ValidationError mapConstraintViolations(ConstraintViolation<?> violation){
        String field = violation.getPropertyPath().toString();
        return ValidationError.of(
                field,
                violation.getInvalidValue(),
                violation.getMessage(),
                null
        );
    }
}
