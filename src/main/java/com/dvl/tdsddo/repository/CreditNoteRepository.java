package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.CreditNote;
import com.dvl.tdsddo.response.CreditNoteDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreditNoteRepository extends JpaRepository<CreditNote,Integer> {
    Optional<CreditNote> findByInvoiceId(Integer id);

    @Query("""
        SELECT new com.dvl.tdsddo.response.CreditNoteDetails(
            cn.id,
            cn.invoiceId,
            cn.creditNoteNumber,
            im.invoiceNumber,
            cn.creditNoteDate,
            cn.baseAmount,
            cn.creditNoteAmount,
            cn.eInvoiceStatus,
            cn.irnStatus,
            cn.eInvoicePreView,
            cn.reason,
            cm.customerName,
            im.receiptNumber
        )
        FROM CreditNote cn
        JOIN InvoiceMaster im ON cn.invoiceId = im.id
        Join CustomerMaster cm ON im.customerId = cm.id
        Join GSTMaster gm ON im.gstId = gm.id AND gm.status = 'active'
        WHERE
            (:ddoId IS NULL OR im.ddoId = :ddoId)
        AND
            (:gstId IS NULL OR gm.userId = :gstId)
        ORDER BY cn.creditNoteDate DESC
    """)
    List<CreditNoteDetails> findCreditNotesByDdoOrGst(
            @Param("ddoId") Integer ddoId,
            @Param("gstId") Integer gstId
    );

    @Query("""
        SELECT cn.creditNoteNumber
        FROM CreditNote cn
        JOIN InvoiceMaster im ON im.id = cn.invoiceId
        WHERE im.ddoId = :ddoId
          AND im.gstId = :gstId
        ORDER BY cn.id DESC
    """)
    List<String> findLastCreditNoteNumber(
            @Param("ddoId") Integer ddoId,
            @Param("gstId") Integer gstId
    );
}
