package com.example.auth_demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;
import java.util.Date;

@TableName("expense")
@Data
public class Expense {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Double amount;
    private String category;
    private String description;
    private String paymentMethod;
    private Date expenseDate;
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}