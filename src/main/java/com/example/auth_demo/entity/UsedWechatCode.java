package com.example.auth_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("used_wechat_codes")
@Data
public class UsedWechatCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Date usedTime;
    private Date expiryTime;
}