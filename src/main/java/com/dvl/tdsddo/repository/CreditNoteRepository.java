package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditNoteRepository extends JpaRepository<CreditNote,Integer> {
    Optional<CreditNote> findByInvoiceId(Integer id);
}
