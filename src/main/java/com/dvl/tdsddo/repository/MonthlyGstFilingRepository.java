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
}