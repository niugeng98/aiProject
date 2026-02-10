package com.example.auth_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("rate_limit_records")
@Data
public class RateLimitRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("rate_key")
    private String rateKey;
    private String endpoint;
    private Integer requestCount;
    private Date lastUpdated;
}