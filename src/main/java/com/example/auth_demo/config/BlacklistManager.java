package com.example.auth_demo.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth_demo.entity.BlacklistRecord;
import com.example.auth_demo.mapper.BlacklistRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class BlacklistManager {
    
    @Autowired
    private BlacklistRecordMapper blacklistRecordMapper;
    
    // 黑名单默认过期时间：24小时
    private static final long BLACKLIST_EXPIRY_TIME = TimeUnit.HOURS.toMillis(24);
    
    /**
     * 检查IP是否在黑名单中
     * @param ip IP地址
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String ip) {
        cleanupExpiredBlacklist();
        QueryWrapper<BlacklistRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("ip_address", ip);
        return blacklistRecordMapper.selectOne(wrapper) != null;
    }
    
    /**
     * 将IP加入黑名单
     * @param ip IP地址
     */
    public void addToBlacklist(String ip) {
        BlacklistRecord record = new BlacklistRecord();
        record.setIpAddress(ip);
        record.setAddedTime(new Date());
        record.setExpiryTime(new Date(System.currentTimeMillis() + BLACKLIST_EXPIRY_TIME));
        
        // 先尝试删除已存在的记录
        QueryWrapper<BlacklistRecord> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("ip_address", ip);
        blacklistRecordMapper.delete(deleteWrapper);
        
        // 添加新记录
        blacklistRecordMapper.insert(record);
    }
    
    /**
     * 从黑名单中移除IP
     * @param ip IP地址
     */
    public void removeFromBlacklist(String ip) {
        QueryWrapper<BlacklistRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("ip_address", ip);
        blacklistRecordMapper.delete(wrapper);
    }
    
    /**
     * 清理过期的黑名单记录
     */
    private void cleanupExpiredBlacklist() {
        QueryWrapper<BlacklistRecord> wrapper = new QueryWrapper<>();
        wrapper.lt("expiry_time", new Date());
        blacklistRecordMapper.delete(wrapper);
    }
    
    /**
     * 获取黑名单大小
     * @return 黑名单大小
     */
    public int getBlacklistSize() {
        cleanupExpiredBlacklist();
        return blacklistRecordMapper.selectCount(null).intValue();
    }
}