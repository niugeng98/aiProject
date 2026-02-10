package com.example.auth_demo.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth_demo.entity.LoginAttemptRecord;
import com.example.auth_demo.mapper.LoginAttemptRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class RiskAssessmentService {
    
    @Autowired
    private LoginAttemptRecordMapper loginAttemptRecordMapper;
    
    // 登录尝试记录过期时间：1小时
    private static final long LOGIN_ATTEMPT_EXPIRY = TimeUnit.HOURS.toMillis(1);
    
    // 风险等级阈值（调整为更宽松，适合小程序登录）
    private static final int LOW_RISK_THRESHOLD = 30;
    private static final int MEDIUM_RISK_THRESHOLD = 60;
    private static final int HIGH_RISK_THRESHOLD = 90;
    
    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW,    // 低风险
        MEDIUM, // 中风险
        HIGH,   // 高风险
        CRITICAL // 极高风险
    }
    
    /**
     * 评估用户登录风险
     * @param ip IP地址
     * @param deviceFingerprint 设备指纹
     * @param isSuccess 是否登录成功
     * @return 风险等级
     */
    public RiskLevel assessLoginRisk(String ip, String deviceFingerprint, boolean isSuccess) {
        cleanupExpiredAttempts();
        
        // 使用IP和设备指纹的组合作为唯一标识
        String key = ip + "|" + deviceFingerprint;
        
        // 获取或创建登录尝试记录
        LoginAttemptRecord record = getOrCreateAttemptRecord(key);
        
        // 更新登录尝试记录
        updateAttemptRecord(record, isSuccess);
        
        // 计算风险分数
        int riskScore = calculateRiskScore(record);
        
        // 根据风险分数返回风险等级
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            return RiskLevel.CRITICAL;
        } else if (riskScore >= MEDIUM_RISK_THRESHOLD) {
            return RiskLevel.HIGH;
        } else if (riskScore >= LOW_RISK_THRESHOLD) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
    
    /**
     * 获取或创建登录尝试记录
     */
    private LoginAttemptRecord getOrCreateAttemptRecord(String key) {
        QueryWrapper<LoginAttemptRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("attempt_key", key);
        LoginAttemptRecord record = loginAttemptRecordMapper.selectOne(wrapper);
        
        if (record == null) {
            record = new LoginAttemptRecord();
            record.setAttemptKey(key);
            record.setTotalAttempts(0);
            record.setFailureCount(0);
            record.setConsecutiveFailures(0);
            record.setLastAttemptTime(new Date());
            loginAttemptRecordMapper.insert(record);
        }
        
        return record;
    }
    
    /**
     * 更新登录尝试记录
     */
    private void updateAttemptRecord(LoginAttemptRecord record, boolean isSuccess) {
        record.setTotalAttempts(record.getTotalAttempts() + 1);
        record.setLastAttemptTime(new Date());
        
        if (!isSuccess) {
            record.setFailureCount(record.getFailureCount() + 1);
            record.setConsecutiveFailures(record.getConsecutiveFailures() + 1);
        } else {
            // 登录成功，重置连续失败次数
            record.setConsecutiveFailures(0);
        }
        
        loginAttemptRecordMapper.updateById(record);
    }
    
    /**
     * 计算风险分数
     * @param record 登录尝试记录
     * @return 风险分数
     */
    private int calculateRiskScore(LoginAttemptRecord record) {
        int score = 0;
        
        // 失败次数权重（降低权重，适合小程序登录）
        score += record.getFailureCount() * 10;
        
        // 尝试频率权重（简化计算，基于总尝试次数和时间差）
        long timeDiff = System.currentTimeMillis() - record.getLastAttemptTime().getTime();
        int minutesDiff = (int) (timeDiff / (60 * 1000)) + 1;
        int attemptsPerMinute = record.getTotalAttempts() / minutesDiff;
        score += attemptsPerMinute * 3;
        
        // 连续失败权重（降低权重，适合小程序登录）
        score += record.getConsecutiveFailures() * 8;
        
        // 确保分数在0-100之间
        return Math.min(100, Math.max(0, score));
    }
    
    /**
     * 清理过期的登录尝试记录
     */
    private void cleanupExpiredAttempts() {
        long cutoffTime = System.currentTimeMillis() - LOGIN_ATTEMPT_EXPIRY;
        QueryWrapper<LoginAttemptRecord> wrapper = new QueryWrapper<>();
        wrapper.lt("last_attempt_time", new Date(cutoffTime));
        loginAttemptRecordMapper.delete(wrapper);
    }
}