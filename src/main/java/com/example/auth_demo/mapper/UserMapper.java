package com.example.auth_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth_demo.entity.User;

public interface UserMapper extends BaseMapper<User> {
    User selectByUsername(String username);
    User selectByEmail(String email);
}