package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.CustomerMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerMasterRepository extends JpaRepository<CustomerMaster,Integer> {
    Optional<CustomerMaster> findByGstNumberAndStatus(String gstNumber, String active);

    Optional<CustomerMaster> findByExemptionNumberAndStatus(String exemptionNumber, String active);

    List<CustomerMaster> findByDdoIdAndStatus(Integer ddoId, String active);
}
