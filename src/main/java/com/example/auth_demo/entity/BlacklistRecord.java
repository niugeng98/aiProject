package com.example.auth_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("blacklist_records")
@Data
public class BlacklistRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ipAddress;
    private Date addedTime;
    private Date expiryTime;
}