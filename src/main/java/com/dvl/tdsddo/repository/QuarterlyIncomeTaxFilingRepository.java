package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.entity.QuarterlyIncomeTaxFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuarterlyIncomeTaxFilingRepository extends JpaRepository<QuarterlyIncomeTaxFiling, Long> {
    List<QuarterlyIncomeTaxFiling> findByDdoId(Integer ddoId);
}

