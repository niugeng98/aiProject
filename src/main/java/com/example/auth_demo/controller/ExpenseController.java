package com.example.auth_demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.auth_demo.common.Result;
import com.example.auth_demo.entity.Expense;
import com.example.auth_demo.service.ExpenseService;
import com.example.auth_demo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    @Autowired
    private ExpenseService expenseService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<Result<?>> addExpense(@RequestBody Expense expense, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        expense.setUserId(userId);
        Expense savedExpense = expenseService.addExpense(expense);
        return ResponseEntity.ok(Result.success("添加成功", savedExpense));
    }

    @GetMapping
    public ResponseEntity<Result<?>> getExpenses(
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
        // 使用当前登录用户的ID
        Long targetUserId = userId != null ? userId : currentUserId;
        
        IPage<Expense> expenses = expenseService.getExpensesByUserId(targetUserId, startDate, endDate, category, page, size);
        return ResponseEntity.ok(Result.success(expenses));
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
        Map<String, Object> statistics = expenseService.getMonthlyStatistics(userId, year, month);
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
        List<Map<String, Object>> statistics = expenseService.getCategoryStatistics(userId, year, month);
        return ResponseEntity.ok(Result.success(statistics));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<?>> getExpenseById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Expense expense = expenseService.getExpenseById(id);
        if (expense == null) {
            return ResponseEntity.badRequest().body(Result.error(404, "记录不存在"));
        }
        if (!expense.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Result.error(403, "无权访问"));
        }
        return ResponseEntity.ok(Result.success(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<?>> updateExpense(@PathVariable Long id, @RequestBody Expense expense, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Expense existingExpense = expenseService.getExpenseById(id);
        if (existingExpense == null || !existingExpense.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Result.error(403, "无权访问"));
        }
        expense.setId(id);
        expense.setUserId(userId);
        Expense updatedExpense = expenseService.updateExpense(expense);
        return ResponseEntity.ok(Result.success("更新成功", updatedExpense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<?>> deleteExpense(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        Expense expense = expenseService.getExpenseById(id);
        if (expense == null || !expense.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Result.error(403, "无权访问"));
        }
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(Result.success("删除成功", null));
    }
}