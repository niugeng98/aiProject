package com.example.auth_demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.auth_demo.entity.Income;
import com.example.auth_demo.mapper.IncomeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncomeService {
    @Autowired
    private IncomeMapper incomeMapper;

    public Income addIncome(Income income) {
        if (income.getCreatedAt() == null) {
            income.setCreatedAt(new Date());
        }
        if (income.getIncomeDate() == null) {
            income.setIncomeDate(new Date());
        }
        incomeMapper.insert(income);
        return income;
    }

    public List<Income> getIncomesByUserId(Long userId) {
        return incomeMapper.selectByUserId(userId);
    }

    public IPage<Income> getIncomesByUserId(Long userId, Date startDate, Date endDate, String category, int page, int size) {
        QueryWrapper<Income> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        
        if (startDate != null && endDate != null) {
            queryWrapper.between("income_date", startDate, endDate);
        }
        
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq("category", category);
        }
        
        queryWrapper.orderByDesc("income_date");
        
        IPage<Income> incomePage = new Page<>(page, size);
        return incomeMapper.selectPage(incomePage, queryWrapper);
    }

    public Income getIncomeById(Long id) {
        return incomeMapper.selectById(id);
    }

    public Income updateIncome(Income income) {
        incomeMapper.updateById(income);
        return income;
    }

    public void deleteIncome(Long id) {
        incomeMapper.deleteById(id);
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

        // 查询当月的所有收入
        QueryWrapper<Income> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.between("income_date", startDate, endDate);
        List<Income> incomes = incomeMapper.selectList(queryWrapper);

        // 计算总金额
        Double total = incomes.stream()
                .mapToDouble(Income::getAmount)
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("total", total);
        result.put("count", incomes.size());
        result.put("average", incomes.isEmpty() ? 0 : total / incomes.size());

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

        // 查询当月的所有收入
        QueryWrapper<Income> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.between("income_date", startDate, endDate);
        List<Income> incomes = incomeMapper.selectList(queryWrapper);

        // 计算总金额
        Double total = incomes.stream()
                .mapToDouble(Income::getAmount)
                .sum();

        if (total == null || total == 0) {
            return new ArrayList<>();
        }

        // 按类别分组统计
        Map<String, Double> categoryMap = incomes.stream()
                .collect(Collectors.groupingBy(
                        Income::getCategory,
                        Collectors.summingDouble(Income::getAmount)
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
