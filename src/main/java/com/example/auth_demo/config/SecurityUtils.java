package com.example.auth_demo.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

@Component
public class SecurityUtils {
    
    /**
     * 获取真实的客户端IP地址
     */
    public static String getClientIp() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 验证手机号格式
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        String regex = "^1[3-9]\\d{9}$";
        return Pattern.matches(regex, phoneNumber);
    }
    
    /**
     * 生成随机密码
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
    
    /**
     * 生成设备指纹
     */
    public static String generateDeviceFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = getClientIp();
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        String connection = request.getHeader("Connection");
        
        // 组合设备信息
        String deviceInfo = userAgent + "|" + ip + "|" + acceptLanguage + "|" + acceptEncoding + "|" + connection;
        
        try {
            // 使用MD5生成设备指纹
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(deviceInfo.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果MD5不可用，使用简单的哈希
            return String.valueOf(deviceInfo.hashCode());
        }
    }
}