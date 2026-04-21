package com.majinhai.website.exception;

import com.majinhai.website.model.dto.ApiErrorDetail;
import com.majinhai.website.model.dto.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ApiErrorDetail>>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        List<ApiErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toDetail)
                .toList();

        return ResponseEntity.badRequest().body(
                ApiResponse.failure(
                        "VALIDATION_ERROR",
                        "请求参数校验失败",
                        details
                )
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.badRequest().body(
                ApiResponse.failure(
                        exception.getCode(),
                        exception.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.failure(
                        "INTERNAL_SERVER_ERROR",
                        "服务器内部错误",
                        null
                )
        );
    }

    private ApiErrorDetail toDetail(FieldError error) {
        return new ApiErrorDetail(
                error.getField(),
                error.getDefaultMessage() == null ? "字段不合法" : error.getDefaultMessage()
        );
    }
}
