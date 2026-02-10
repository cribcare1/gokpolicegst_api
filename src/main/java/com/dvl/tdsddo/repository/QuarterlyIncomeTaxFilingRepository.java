package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.entity.QuarterlyIncomeTaxFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuarterlyIncomeTaxFilingRepository extends JpaRepository<QuarterlyIncomeTaxFiling, Long> {
    List<QuarterlyIncomeTaxFiling> findByDdoId(Integer ddoId);

    @Query("""
        SELECT q FROM QuarterlyIncomeTaxFiling q
        JOIN DdoGStMapping m
          ON q.ddoId = m.ddoId
        JOIN GSTMaster g
          ON g.id = m.fromGst
        Join User u
          ON u.id = g.userId
        WHERE u.id = :gstId
          AND m.status = 'active'
          AND g.status = 'active'
    """)
    List<QuarterlyIncomeTaxFiling> findByGstId( Integer gstId);

    boolean existsByDdoIdAndFiscalYearAndReturnTypeAndQuarterAndIdNot(Integer ddoId, String fiscalYear, String returnType, String quarter, Long id);

    boolean existsByDdoIdAndFiscalYearAndReturnTypeAndQuarter(Integer ddoId, String fiscalYear, String returnType, String quarter);
}

