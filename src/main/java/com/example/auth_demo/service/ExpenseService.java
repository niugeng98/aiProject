package com.example.auth_demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.auth_demo.entity.Expense;
import com.example.auth_demo.mapper.ExpenseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {
    @Autowired
    private ExpenseMapper expenseMapper;

    public Expense addExpense(Expense expense) {
        if (expense.getCreatedAt() == null) {
            expense.setCreatedAt(new Date());
        }
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(new Date());
        }
        expenseMapper.insert(expense);
        return expense;
    }

    public List<Expense> getExpensesByUserId(Long userId) {
        return expenseMapper.selectByUserId(userId);
    }

    public IPage<Expense> getExpensesByUserId(Long userId, Date startDate, Date endDate, String category, int page, int size) {
        QueryWrapper<Expense> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        
        if (startDate != null && endDate != null) {
            queryWrapper.between("expense_date", startDate, endDate);
        }
        
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq("category", category);
        }
        
        queryWrapper.orderByDesc("expense_date");
        
        IPage<Expense> expensePage = new Page<>(page, size);
        return expenseMapper.selectPage(expensePage, queryWrapper);
    }

    public Expense getExpenseById(Long id) {
        return expenseMapper.selectById(id);
    }

    public Expense updateExpense(Expense expense) {
        expenseMapper.updateById(expense);
        return expense;
    }

    public void deleteExpense(Long id) {
        expenseMapper.deleteById(id);
    }

    public Map<String, Object> getMonthlyStatistics(Long userId, Integer year, Integer month) {
        Calendar calendar = Calendar.getInstance();
        if (year == null) {
            year = calendar.get(Calendar.YEAR);
        }
        if (month == null) {
            month = calendar.get(Calendar.MONTH) + 1;
        }

        // 构建月份的开始和结束时间
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(year, month - 1, 1, 0, 0, 0);
        Date startDate = startCalendar.getTime();
        
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(year, month - 1, startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endDate = endCalendar.getTime();

        // 查询当月的所有支出
        QueryWrapper<Expense> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.between("expense_date", startDate, endDate);
        List<Expense> expenses = expenseMapper.selectList(queryWrapper);

        // 计算总金额
        Double total = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

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

        // 构建月份的开始和结束时间
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(year, month - 1, 1, 0, 0, 0);
        Date startDate = startCalendar.getTime();
        
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(year, month - 1, startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endDate = endCalendar.getTime();

        // 查询当月的所有支出
        QueryWrapper<Expense> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.between("expense_date", startDate, endDate);
        List<Expense> expenses = expenseMapper.selectList(queryWrapper);

        // 计算总金额
        Double total = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        if (total == null || total == 0) {
            return new ArrayList<>();
        }

        // 按类别分组统计
        Map<String, Double> categoryMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));

        // 转换为需要的格式
        return categoryMap.entrySet().stream().map(entry -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("category", entry.getKey());
            stat.put("amount", entry.getValue());
            stat.put("percentage", (entry.getValue() / total * 100));
            return stat;
        }).collect(Collectors.toList());
    }
}