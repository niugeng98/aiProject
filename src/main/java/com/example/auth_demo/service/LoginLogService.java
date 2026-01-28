package com.example.auth_demo.service;

import com.example.auth_demo.entity.LoginLog;
import com.example.auth_demo.repository.LoginLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LoginLogService {
    @Autowired
    private LoginLogRepository loginLogRepository;

    public void saveLoginLog(Long userId, String username, String ipAddress, String userAgent, String status, String message) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setStatus(status);
        loginLog.setMessage(message);
        loginLog.setLoginTime(new Date());
        loginLogRepository.save(loginLog);
    }

    public Iterable<LoginLog> getAllLoginLogs() {
        return loginLogRepository.findAll();
    }
}