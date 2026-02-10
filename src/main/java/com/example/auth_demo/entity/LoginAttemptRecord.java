package com.example.auth_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("login_attempts")
@Data
public class LoginAttemptRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("attempt_key")
    private String attemptKey;
    private Integer totalAttempts;
    private Integer failureCount;
    private Integer consecutiveFailures;
    private Date lastAttemptTime;
}