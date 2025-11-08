package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.PanMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PanMasterRepository extends JpaRepository<PanMaster,Integer > {
    PanMaster findByPanNumber(String panNumber);
    List<PanMaster> findByStatus(String status);

    List<PanMaster> findByStatusIgnoreCase(String status);

    PanMaster findByMobileAndStatus(String mobile, String active);
}
