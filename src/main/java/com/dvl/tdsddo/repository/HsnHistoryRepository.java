package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.HsnGstHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HsnHistoryRepository extends JpaRepository<HsnGstHistory,Integer> {
    List<HsnGstHistory> findByHsnIdOrderByEffectiveFromDesc(Integer hsnId);
}
