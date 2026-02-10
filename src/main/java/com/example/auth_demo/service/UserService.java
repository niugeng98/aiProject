package com.example.auth_demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.auth_demo.entity.User;
import com.example.auth_demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return user;
    }

    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public User findByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public User updateProfile(Long userId, User user) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            return null;
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        userMapper.updateById(existingUser);
        return existingUser;
    }

    public void changePassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);
        }
    }
}