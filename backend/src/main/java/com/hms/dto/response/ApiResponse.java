package com.hms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse {

    private String message;
    private Object data;
    private Boolean success;
    private Long timestamp;

    public ApiResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(String message, Object data, Boolean success) {
        this.message = message;
        this.data = data;
        this.success = success;
        this.timestamp = System.currentTimeMillis();
    }

}