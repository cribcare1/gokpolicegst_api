package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem,Integer> {
    void deleteByInvoiceId(Integer id);

    Optional<InvoiceItem> findByInvoiceId(Integer id);
}
