package com.example.auth_demo.service;

import com.example.auth_demo.entity.Expense;
import com.example.auth_demo.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {
    @Autowired
    private ExpenseRepository expenseRepository;

    public Expense addExpense(Expense expense) {
        if (expense.getCreatedAt() == null) {
            expense.setCreatedAt(new Date());
        }
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(new Date());
        }
        return expenseRepository.save(expense);
    }

    public List<Expense> getExpensesByUserId(Long userId) {
        return expenseRepository.findByUserId(userId);
    }

    public Page<Expense> getExpensesByUserId(Long userId, Date startDate, Date endDate, String category, Pageable pageable) {
        if (startDate != null && endDate != null && category != null && !category.isEmpty()) {
            return expenseRepository.findByUserIdAndExpenseDateBetweenAndCategory(userId, startDate, endDate, category, pageable);
        } else if (startDate != null && endDate != null) {
            return expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDate, endDate, pageable);
        } else if (category != null && !category.isEmpty()) {
            return expenseRepository.findByUserIdAndCategory(userId, category, pageable);
        } else {
            return expenseRepository.findByUserId(userId, pageable);
        }
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id).orElse(null);
    }

    public Expense updateExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }

    public Map<String, Object> getMonthlyStatistics(Long userId, Integer year, Integer month) {
        Calendar calendar = Calendar.getInstance();
        if (year == null) {
            year = calendar.get(Calendar.YEAR);
        }
        if (month == null) {
            month = calendar.get(Calendar.MONTH) + 1;
        }

        List<Expense> expenses = expenseRepository.findByUserIdAndYearAndMonth(userId, year, month);
        Double total = expenseRepository.getMonthlyTotal(userId, year, month);
        if (total == null) {
            total = 0.0;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("total", total);
        result.put("count", expenses.size());
        result.put("average", expenses.isEmpty() ? 0 : total / expenses.size());

        return result;
    }

    public List<Map<String, Object>> getCategoryStatistics(Long userId, Integer year, Integer month) {
        Calendar calendar = Calendar.getInstance();
        if (year == null) {
            year = calendar.get(Calendar.YEAR);
        }
        if (month == null) {
            month = calendar.get(Calendar.MONTH) + 1;
        }

        List<Object[]> results = expenseRepository.getCategoryStatistics(userId, year, month);
        Double total = expenseRepository.getMonthlyTotal(userId, year, month);
        if (total == null || total == 0) {
            return new ArrayList<>();
        }

        return results.stream().map(row -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("category", row[0]);
            stat.put("amount", row[1]);
            stat.put("percentage", ((Double) row[1] / total * 100));
            return stat;
        }).collect(Collectors.toList());
    }
}