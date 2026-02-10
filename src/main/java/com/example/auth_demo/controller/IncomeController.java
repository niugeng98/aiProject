package com.example.auth_demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.auth_demo.common.Result;
import com.example.auth_demo.entity.Income;
import com.example.auth_demo.service.IncomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {
    @Autowired
    private IncomeService incomeService;

    @PostMapping
    public ResponseEntity<Result<?>> addIncome(@RequestBody Income income, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        income.setUserId(userId);
        Income savedIncome = incomeService.addIncome(income);
        return ResponseEntity.ok(Result.success("添加成功", savedIncome));
    }

    @GetMapping
    public ResponseEntity<Result<?>> getIncomes(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Long targetUserId = userId != null ? userId : currentUserId;
        
        IPage<Income> incomes = incomeService.getIncomesByUserId(targetUserId, startDate, endDate, category, page, size);
        return ResponseEntity.ok(Result.success(incomes));
    }

    @GetMapping("/statistics/monthly")
    public ResponseEntity<Result<?>> getMonthlyStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Map<String, Object> statistics = incomeService.getMonthlyStatistics(userId, year, month);
        return ResponseEntity.ok(Result.success(statistics));
    }

    @GetMapping("/statistics/category")
    public ResponseEntity<Result<?>> getCategoryStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        List<Map<String, Object>> statistics = incomeService.getCategoryStatistics(userId, year, month);
        return ResponseEntity.ok(Result.success(statistics));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<?>> getIncomeById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Income income = incomeService.getIncomeById(id);
        if (income == null) {
            return ResponseEntity.badRequest().body(Result.error(404, "记录不存在"));
        }
        if (!income.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Result.error(403, "无权访问"));
        }
        return ResponseEntity.ok(Result.success(income));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<?>> updateIncome(@PathVariable Long id, @RequestBody Income income, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Income existingIncome = incomeService.getIncomeById(id);
        if (existingIncome == null || !existingIncome.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Result.error(403, "无权访问"));
        }
        income.setId(id);
        income.setUserId(userId);
        Income updatedIncome = incomeService.updateIncome(income);
        return ResponseEntity.ok(Result.success("更新成功", updatedIncome));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<?>> deleteIncome(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Income income = incomeService.getIncomeById(id);
        if (income == null || !income.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Result.error(403, "无权访问"));
        }
        incomeService.deleteIncome(id);
        return ResponseEntity.ok(Result.success("删除成功", null));
    }
}
