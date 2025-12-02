package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.BankSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankSnapshotRepository extends JpaRepository<BankSnapshot,Integer> {
    Optional<BankSnapshot> findByInvoiceId(Integer id);
}
