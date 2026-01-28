package com.example.auth_demo.repository;

import com.example.auth_demo.entity.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByUserId(Long userId);
    
    Page<Income> findByUserId(Long userId, Pageable pageable);
    
    Page<Income> findByUserIdAndIncomeDateBetween(Long userId, Date startDate, Date endDate, Pageable pageable);
    
    Page<Income> findByUserIdAndCategory(Long userId, String category, Pageable pageable);
    
    Page<Income> findByUserIdAndIncomeDateBetweenAndCategory(Long userId, Date startDate, Date endDate, String category, Pageable pageable);
    
    @Query("SELECT i FROM Income i WHERE i.userId = :userId AND YEAR(i.incomeDate) = :year AND MONTH(i.incomeDate) = :month")
    List<Income> findByUserIdAndYearAndMonth(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);
    
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.userId = :userId AND YEAR(i.incomeDate) = :year AND MONTH(i.incomeDate) = :month")
    Double getMonthlyTotal(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);
    
    @Query("SELECT i.category, SUM(i.amount) as total FROM Income i WHERE i.userId = :userId AND YEAR(i.incomeDate) = :year AND MONTH(i.incomeDate) = :month GROUP BY i.category")
    List<Object[]> getCategoryStatistics(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);
}
