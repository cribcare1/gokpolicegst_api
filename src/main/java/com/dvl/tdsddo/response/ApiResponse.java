package com.dvl.tdsddo.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {
    private String status;   // "SUCCESS" or "ERROR"
    private String message;
    private Object data;     // optional
}
