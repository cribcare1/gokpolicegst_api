package com.dvl.tdsddo.repository;


import com.dvl.tdsddo.model.MonthlyGstFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonthlyGstFilingRepository extends JpaRepository<MonthlyGstFiling, Integer> {

    @Query("SELECT m FROM MonthlyGstFiling m ORDER BY m.id DESC")
    List<MonthlyGstFiling> getAllFilings();

    @Query("SELECT m FROM MonthlyGstFiling m where m.ddoId =:ddoId ORDER BY m.id DESC")
    List<MonthlyGstFiling> findByDdoId(@Param(value = "ddoId") Integer ddoId);

    boolean existsByDdoIdAndFilingMonthAndIdNot(Integer ddoId, String filingMonth, Integer id);

    boolean existsByArnNoAndIdNot(String arnNo, Integer id);

    @Query("""
        SELECT f FROM MonthlyGstFiling f
        JOIN DdoGStMapping m 
          ON f.ddoId = m.ddoId
         JOIN GSTMaster g
          ON g.id = m.fromGst
        Join User u
          ON u.id = g.userId
        WHERE u.id = :gstId
          AND m.status = 'active'
          AND f.status = 'active'
          AND g.status = 'active'
    """)
    List<MonthlyGstFiling> findFilingsByGstId(Integer gstId);
}