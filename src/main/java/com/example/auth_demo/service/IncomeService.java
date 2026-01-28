package com.example.auth_demo.service;

import com.example.auth_demo.entity.Income;
import com.example.auth_demo.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncomeService {
    @Autowired
    private IncomeRepository incomeRepository;

    public Income addIncome(Income income) {
        if (income.getCreatedAt() == null) {
            income.setCreatedAt(new Date());
        }
        if (income.getIncomeDate() == null) {
            income.setIncomeDate(new Date());
        }
        return incomeRepository.save(income);
    }

    public List<Income> getIncomesByUserId(Long userId) {
        return incomeRepository.findByUserId(userId);
    }

    public Page<Income> getIncomesByUserId(Long userId, Date startDate, Date endDate, String category, Pageable pageable) {
        if (startDate != null && endDate != null && category != null && !category.isEmpty()) {
            return incomeRepository.findByUserIdAndIncomeDateBetweenAndCategory(userId, startDate, endDate, category, pageable);
        } else if (startDate != null && endDate != null) {
            return incomeRepository.findByUserIdAndIncomeDateBetween(userId, startDate, endDate, pageable);
        } else if (category != null && !category.isEmpty()) {
            return incomeRepository.findByUserIdAndCategory(userId, category, pageable);
        } else {
            return incomeRepository.findByUserId(userId, pageable);
        }
    }

    public Income getIncomeById(Long id) {
        return incomeRepository.findById(id).orElse(null);
    }

    public Income updateIncome(Income income) {
        return incomeRepository.save(income);
    }

    public void deleteIncome(Long id) {
        incomeRepository.deleteById(id);
    }

    public Map<String, Object> getMonthlyStatistics(Long userId, Integer year, Integer month) {
        Calendar calendar = Calendar.getInstance();
        if (year == null) {
            year = calendar.get(Calendar.YEAR);
        }
        if (month == null) {
            month = calendar.get(Calendar.MONTH) + 1;
        }

        List<Income> incomes = incomeRepository.findByUserIdAndYearAndMonth(userId, year, month);
        Double total = incomeRepository.getMonthlyTotal(userId, year, month);
        if (total == null) {
            total = 0.0;
        }

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

        List<Object[]> results = incomeRepository.getCategoryStatistics(userId, year, month);
        Double total = incomeRepository.getMonthlyTotal(userId, year, month);
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
