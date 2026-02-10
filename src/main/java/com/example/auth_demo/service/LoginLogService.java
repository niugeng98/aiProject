package com.example.auth_demo.service;

import com.example.auth_demo.entity.LoginLog;
import com.example.auth_demo.mapper.LoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class LoginLogService {
    @Autowired
    private LoginLogMapper loginLogMapper;

    public void saveLoginLog(Long userId, String username, String ipAddress, String userAgent, String status, String message) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(userAgent);
        loginLog.setStatus(status);
        loginLog.setMessage(message);
        loginLog.setLoginTime(new Date());
        loginLogMapper.insert(loginLog);
    }

    public List<LoginLog> getAllLoginLogs() {
        return loginLogMapper.selectList(null);
    }
}