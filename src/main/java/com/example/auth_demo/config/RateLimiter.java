package com.example.auth_demo.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth_demo.entity.RateLimitRecord;
import com.example.auth_demo.mapper.RateLimitRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {
    
    @Autowired
    private RateLimitRecordMapper rateLimitRecordMapper;
    
    // 默认限制：每分钟60个请求
    private static final int DEFAULT_MAX_REQUESTS = 60;
    private static final long DEFAULT_TIME_WINDOW = TimeUnit.MINUTES.toMillis(1);
    
    // 微信登录接口限制：每分钟5个请求
    private static final int WECHAT_LOGIN_MAX_REQUESTS = 5;
    private static final long WECHAT_LOGIN_TIME_WINDOW = TimeUnit.MINUTES.toMillis(1);
    
    /**
     * 检查是否超过速率限制
     * @param ip IP地址
     * @param endpoint 请求端点
     * @return 是否允许请求
     */
    public boolean isAllowed(String ip, String endpoint) {
        if (endpoint.contains("/wechat-login")) {
            return checkRateLimit(ip, endpoint, WECHAT_LOGIN_MAX_REQUESTS, WECHAT_LOGIN_TIME_WINDOW);
        }
        return checkRateLimit(ip, endpoint, DEFAULT_MAX_REQUESTS, DEFAULT_TIME_WINDOW);
    }
    
    /**
     * 检查是否超过速率限制（结合设备指纹）
     * @param ip IP地址
     * @param deviceFingerprint 设备指纹
     * @param endpoint 请求端点
     * @return 是否允许请求
     */
    public boolean isAllowed(String ip, String deviceFingerprint, String endpoint) {
        // 结合IP和设备指纹生成唯一标识
        String uniqueIdentifier = ip + "|" + deviceFingerprint;
        if (endpoint.contains("/wechat-login")) {
            return checkRateLimit(uniqueIdentifier, endpoint, WECHAT_LOGIN_MAX_REQUESTS, WECHAT_LOGIN_TIME_WINDOW);
        }
        return checkRateLimit(uniqueIdentifier, endpoint, DEFAULT_MAX_REQUESTS, DEFAULT_TIME_WINDOW);
    }
    
    /**
     * 检查速率限制
     */
    private boolean checkRateLimit(String identifier, String endpoint, int maxRequests, long timeWindow) {
        String key = identifier + ":" + endpoint;
        
        // 清理过期记录
        cleanupExpiredRecords(timeWindow);
        
        // 查询现有记录
            QueryWrapper<RateLimitRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("rate_key", key);
            wrapper.eq("endpoint", endpoint);
            RateLimitRecord record = rateLimitRecordMapper.selectOne(wrapper);
            
            long currentTime = System.currentTimeMillis();
            
            if (record == null) {
                // 首次请求
                record = new RateLimitRecord();
                record.setRateKey(key);
                record.setEndpoint(endpoint);
                record.setRequestCount(1);
                record.setLastUpdated(new Date());
                rateLimitRecordMapper.insert(record);
                return true;
            }
        
        // 检查是否在时间窗口内
        long recordTime = record.getLastUpdated().getTime();
        if (currentTime - recordTime > timeWindow) {
            // 超出时间窗口，重置计数
            record.setRequestCount(1);
            record.setLastUpdated(new Date());
            rateLimitRecordMapper.updateById(record);
            return true;
        }
        
        // 检查是否超过限制
        if (record.getRequestCount() >= maxRequests) {
            return false;
        }
        
        // 增加计数
        record.setRequestCount(record.getRequestCount() + 1);
        record.setLastUpdated(new Date());
        rateLimitRecordMapper.updateById(record);
        return true;
    }
    
    /**
     * 清理过期的速率限制记录
     */
    private void cleanupExpiredRecords(long timeWindow) {
        long cutoffTime = System.currentTimeMillis() - timeWindow;
        QueryWrapper<RateLimitRecord> wrapper = new QueryWrapper<>();
        wrapper.lt("last_updated", new Date(cutoffTime));
        rateLimitRecordMapper.delete(wrapper);
    }
}