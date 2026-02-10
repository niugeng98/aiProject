package com.example.auth_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("login_log")
@Data
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String message;
    private Date loginTime;
}