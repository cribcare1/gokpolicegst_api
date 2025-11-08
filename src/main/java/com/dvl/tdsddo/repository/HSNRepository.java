package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.HSNMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface HSNRepository extends JpaRepository<HSNMaster,Integer> {

    Optional<HSNMaster> findByHsnCode(String hsnCode);
    List<HSNMaster> findByStatus(String status);
}
