package com.dvl.tdsddo.Exception;


import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(TdsDdoConstant.ERROR, ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse> handleInvalidRequest(InvalidRequestException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse(TdsDdoConstant.ERROR, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(new ApiResponse(TdsDdoConstant.ERROR, message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleAll(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ApiResponse(TdsDdoConstant.ERROR, "Something went wrong: " + ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleAllRunTimeException(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ApiResponse(TdsDdoConstant.ERROR,  ex.getMessage(), null));
    }
}

