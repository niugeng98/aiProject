package com.example.auth_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth_demo.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}