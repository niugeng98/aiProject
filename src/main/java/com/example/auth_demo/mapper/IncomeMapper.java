package com.example.auth_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth_demo.entity.Income;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IncomeMapper extends BaseMapper<Income> {
    List<Income> selectByUserId(Long userId);
}