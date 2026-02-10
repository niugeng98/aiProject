package com.example.auth_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("captcha_records")
@Data
public class CaptchaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("captcha_key")
    private String captchaKey;
    private String captcha;
    private Date createdTime;
    private Date expiryTime;
}