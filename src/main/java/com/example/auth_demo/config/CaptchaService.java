package com.example.auth_demo.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth_demo.entity.CaptchaRecord;
import com.example.auth_demo.mapper.CaptchaRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@Component
public class CaptchaService {
    
    @Autowired
    private CaptchaRecordMapper captchaRecordMapper;
    
    // 验证码有效期：5分钟
    private static final long CAPTCHA_EXPIRY = TimeUnit.MINUTES.toMillis(5);
    
    // 验证码长度
    private static final int CAPTCHA_LENGTH = 6;
    
    // 验证码字符集
    private static final String CAPTCHA_CHARACTERS = "0123456789";
    
    /**
     * 生成验证码
     * @param key 验证码唯一标识（如IP+设备指纹）
     * @return 生成的验证码
     */
    public String generateCaptcha(String key) {
        cleanupExpiredCaptchas();
        
        // 生成随机验证码
        StringBuilder captcha = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            captcha.append(CAPTCHA_CHARACTERS.charAt(random.nextInt(CAPTCHA_CHARACTERS.length())));
        }
        
        // 存储验证码和过期时间
        String captchaValue = captcha.toString();
        
        // 先删除已存在的记录
            QueryWrapper<CaptchaRecord> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("captcha_key", key);
            captchaRecordMapper.delete(deleteWrapper);
            
            // 创建新记录
            CaptchaRecord record = new CaptchaRecord();
            record.setCaptchaKey(key);
            record.setCaptcha(captchaValue);
            record.setCreatedTime(new Date());
            record.setExpiryTime(new Date(System.currentTimeMillis() + CAPTCHA_EXPIRY));
            captchaRecordMapper.insert(record);
        
        return captchaValue;
    }
    
    /**
     * 验证验证码
     * @param key 验证码唯一标识
     * @param inputCaptcha 用户输入的验证码
     * @return 验证码是否有效
     */
    public boolean validateCaptcha(String key, String inputCaptcha) {
        cleanupExpiredCaptchas();
        
        // 查询验证码记录
        QueryWrapper<CaptchaRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("captcha_key", key);
        CaptchaRecord record = captchaRecordMapper.selectOne(wrapper);
        
        if (record == null) {
            return false;
        }
        
        // 验证验证码
        boolean isValid = record.getCaptcha().equals(inputCaptcha);
        
        // 验证码使用后删除
        if (isValid) {
            captchaRecordMapper.deleteById(record.getId());
        }
        
        return isValid;
    }
    
    /**
     * 清理过期的验证码
     */
    private void cleanupExpiredCaptchas() {
        QueryWrapper<CaptchaRecord> wrapper = new QueryWrapper<>();
        wrapper.lt("expiry_time", new Date());
        captchaRecordMapper.delete(wrapper);
    }
}