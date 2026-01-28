package com.example.auth_demo.repository;

import com.example.auth_demo.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    
    Page<Expense> findByUserId(Long userId, Pageable pageable);
    
    Page<Expense> findByUserIdAndExpenseDateBetween(Long userId, Date startDate, Date endDate, Pageable pageable);
    
    Page<Expense> findByUserIdAndCategory(Long userId, String category, Pageable pageable);
    
    Page<Expense> findByUserIdAndExpenseDateBetweenAndCategory(Long userId, Date startDate, Date endDate, String category, Pageable pageable);
    
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    List<Expense> findByUserIdAndYearAndMonth(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    Double getMonthlyTotal(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);
    
    @Query("SELECT e.category, SUM(e.amount) as total FROM Expense e WHERE e.userId = :userId AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month GROUP BY e.category")
    List<Object[]> getCategoryStatistics(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);
}