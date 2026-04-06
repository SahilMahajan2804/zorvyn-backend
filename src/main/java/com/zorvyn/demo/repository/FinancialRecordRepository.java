package com.zorvyn.demo.repository;

import com.zorvyn.demo.entity.FinancialRecord;
import com.zorvyn.demo.entity.RecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Optional<FinancialRecord> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT r FROM FinancialRecord r " +
           "WHERE r.deletedAt IS NULL " +
           "AND (:userId   IS NULL OR r.user.id   = :userId) " +
           "AND (:type     IS NULL OR r.type       = :type) " +
           "AND (:category IS NULL OR LOWER(r.category) LIKE %:category%) " +
           "AND (:from     IS NULL OR r.date       >= :from) " +
           "AND (:to       IS NULL OR r.date       <= :to) " +
           "ORDER BY r.date DESC, r.createdAt DESC")
    List<FinancialRecord> findAllFiltered(
            @Param("userId")   Long userId,
            @Param("type")     RecordType type,
            @Param("category") String category,
            @Param("from")     LocalDate from,
            @Param("to")       LocalDate to);

    @Query("SELECT r FROM FinancialRecord r " +
           "WHERE r.deletedAt IS NULL " +
           "AND (:userId IS NULL OR r.user.id = :userId) " +
           "ORDER BY r.date DESC, r.createdAt DESC")
    List<FinancialRecord> findRecentFiltered(@Param("userId") Long userId,
                                             org.springframework.data.domain.Pageable pageable);


    @Query("SELECT r.type, SUM(r.amount) FROM FinancialRecord r " +
           "WHERE r.deletedAt IS NULL AND (:userId IS NULL OR r.user.id = :userId) " +
           "GROUP BY r.type")
    List<Object[]> sumByType(@Param("userId") Long userId);

    @Query("SELECT r.category, r.type, SUM(r.amount) " +
           "FROM FinancialRecord r " +
           "WHERE r.deletedAt IS NULL AND (:userId IS NULL OR r.user.id = :userId) " +
           "GROUP BY r.category, r.type ORDER BY r.category")
    List<Object[]> sumByCategoryAndType(@Param("userId") Long userId);

    @Query("SELECT YEAR(r.date), MONTH(r.date), r.type, SUM(r.amount) " +
           "FROM FinancialRecord r " +
           "WHERE r.deletedAt IS NULL AND (:userId IS NULL OR r.user.id = :userId) " +
           "AND r.date >= :from AND r.date <= :to " +
           "GROUP BY YEAR(r.date), MONTH(r.date), r.type " +
           "ORDER BY YEAR(r.date) ASC, MONTH(r.date) ASC")
    List<Object[]> monthlyTrends(@Param("from") LocalDate from,
                                 @Param("to")   LocalDate to,
                                 @Param("userId") Long userId);
}
