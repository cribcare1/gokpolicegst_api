package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.GSTSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GstSnapshotRepository extends JpaRepository<GSTSnapshot,Integer> {
    Optional<GSTSnapshot> findByInvoiceId(Integer id);
}
