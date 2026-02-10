package com.example.auth_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth_demo.entity.Expense;

import java.util.List;

public interface ExpenseMapper extends BaseMapper<Expense> {
    List<Expense> selectByUserId(Long userId);
}