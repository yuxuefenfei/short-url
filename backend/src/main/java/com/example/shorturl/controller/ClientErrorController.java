package com.example.shorturl.controller;

import com.example.shorturl.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/client-errors")
public class ClientErrorController {

    @PostMapping
    public ApiResponse<Void> reportClientError(@Valid @RequestBody ClientErrorRequest request) {
        log.warn(
                "收到前端错误上报: type={}, code={}, status={}, url={}, method={}, clientTimestamp={}, serverTimestamp={}",
                request.getType(),
                request.getCode(),
                request.getStatus(),
                request.getUrl(),
                request.getMethod(),
                request.getTimestamp(),
                LocalDateTime.now()
        );
        return ApiResponse.success();
    }

    @Data
    public static class ClientErrorRequest {
        @NotBlank
        private String type;
        @NotBlank
        private String message;
        private String code;
        private Integer status;
        private String url;
        private String method;
        private String timestamp;
        private Map<String, Object> details;
    }
}
