package com.example.auth_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth_demo.entity.Income;

import java.util.List;

public interface IncomeMapper extends BaseMapper<Income> {
    List<Income> selectByUserId(Long userId);
}