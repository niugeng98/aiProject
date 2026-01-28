package com.example.auth_demo.repository;

import com.example.auth_demo.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}