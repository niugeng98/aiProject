package com.example.auth_demo.controller;

import com.example.auth_demo.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login-logs")
public class LoginLogController {
    @Autowired
    private LoginLogService loginLogService;

    @GetMapping
    public ResponseEntity<?> getAllLoginLogs() {
        return ResponseEntity.ok(loginLogService.getAllLoginLogs());
    }
}