package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.InvoiceMaster;
import com.dvl.tdsddo.response.InvoiceResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InvoiceMasterRepository extends JpaRepository<InvoiceMaster,Integer> {
    Optional<InvoiceMaster> findTopByInvoiceNumberStartingWithOrderByIdDesc(String prefix);

//    @Query("""
//    SELECT i FROM InvoiceMaster i
//    WHERE (:ddoId IS NULL OR i.ddoId = :ddoId)
//      AND (:gstId IS NULL OR i.gstId = :gstId)
//""")
//    List<InvoiceMaster> findByFilters(Integer ddoId, Integer gstId);

    @Query("""
    SELECT i FROM InvoiceMaster i
    WHERE (:ddoId IS NULL OR i.ddoId = :ddoId)
      AND (:gstId IS NULL OR i.gstId = :gstId)
      AND (i.status IS NULL OR i.status NOT IN :excludedStatuses)
    ORDER BY i.id DESC
""")
    List<InvoiceMaster> findByFilters(Integer ddoId, Integer gstId, List<String> excludedStatuses);


    @Query("""
       SELECT i.receiptNumber
       FROM InvoiceMaster i
       WHERE i.gstId = :gstId AND i.ddoId = :ddoId AND i.receiptNumber IS NOT NULL
       ORDER BY i.id DESC
       LIMIT 1
       """)
    String findLastReceipt(@Param("gstId") Integer gstId,
                           @Param("ddoId") Integer ddoId);


    @Query("""
       SELECT i.invoiceNumber
       FROM InvoiceMaster i
       WHERE i.gstId = :gstId 
         AND i.ddoId = :ddoId
         AND i.invoiceNumber IS NOT NULL
         AND (i.status IS NULL OR i.status = 'pending')
         ORDER BY i.id DESC
       LIMIT 1
       """)
    String findLastSavedInvoice(@Param("gstId") Integer gstId,
                                @Param("ddoId") Integer ddoId);


    @Query("""
       SELECT i.finalInvoiceNumber
       FROM InvoiceMaster i
       WHERE i.gstId = :gstId 
         AND i.ddoId = :ddoId
         AND i.finalInvoiceNumber IS NOT NULL
       ORDER BY i.id DESC
       LIMIT 1
       """)
    String findLastSubmittedInvoice(@Param("gstId") Integer gstId,
                                @Param("ddoId") Integer ddoId);


//    @Query("""
//            SELECT new com.dvl.tdsddo.response.InvoiceResponse(
//                i.id,
//                i.ddoId,
//                i.gstId,
//                i.customerId,
//                i.invoiceNumber,
//                i.invoiceStatus,
//                i.invoiceDate,
//                i.remarks,
//                COALESCE(i.totalAmount, 0),
//                COALESCE(i.totalCgst, 0),
//                COALESCE(i.totalSgst, 0),
//                COALESCE(i.totalIgst, 0),
//                COALESCE(i.grandTotal, 0),
//                COALESCE(i.paidAmount, 0),
//                COALESCE(i.balanceAmount, 0),
//
//                gs.gstName,
//                gs.gstNumber,
//                gs.stateCode,
//                gs.gstHolderName,
//
//                bs.bankName,
//                bs.branchName,
//                bs.accountNumber,
//                bs.ifscCode,
//
//                cn.creditNoteNumber,
//                COALESCE(cn.creditNoteAmount, 0),
//                COALESCE(cn.mismatchAmount, 0),
//                cn.reason
//            )
//            FROM InvoiceMaster i
//            LEFT JOIN GSTSnapshot gs ON gs.invoiceId = i.id
//            LEFT JOIN BankSnapshot bs ON bs.invoiceId = i.id
//            LEFT JOIN CreditNote cn ON cn.invoiceId = i.id
//            WHERE (:gstId IS NULL OR i.gstId = :gstId)
//              AND (:ddoId IS NULL OR i.ddoId = :ddoId)
//              AND (:status IS NULL OR i.invoiceStatus = :status)
//            ORDER BY i.id DESC
//            """)
//    List<InvoiceResponse> findInvoicesFiltered(
//            @Param("gstId") Integer gstId,
//            @Param("ddoId") Integer ddoId,
//            @Param("status") String status
//    );

//    @Query("""
//SELECT i, gs, bs, c, cn, ii
//FROM InvoiceMaster i
//LEFT JOIN GSTSnapshot gs ON gs.invoiceId = i.id
//LEFT JOIN BankSnapshot bs ON bs.invoiceId = i.id
//LEFT JOIN CustomerMaster c ON c.id = i.customerId
//LEFT JOIN CreditNote cn ON cn.invoiceId = i.id
//LEFT JOIN InvoiceItem ii ON ii.invoiceId = i.id
//WHERE (:ddoId IS NULL OR i.ddoId = :ddoId)
//AND (:gstId IS NULL OR i.gstId = :gstId)
//AND (:status IS NULL OR i.invoiceStatus = :status)
//ORDER BY i.id DESC
//""")
//    List<Object[]> fetchInvoices(@Param("ddoId") Integer ddoId,
//                                 @Param("gstId") Integer gstId,
//                                 @Param("status") String status);

    @Query("""
SELECT i, gs, bs, c, cn, ii
FROM InvoiceMaster i
LEFT JOIN GSTSnapshot gs ON gs.invoiceId = i.id
LEFT JOIN BankSnapshot bs ON bs.invoiceId = i.id
LEFT JOIN CustomerMaster c ON c.id = i.customerId
LEFT JOIN CreditNote cn ON cn.invoiceId = i.id
LEFT JOIN InvoiceItem ii ON ii.invoiceId = i.id
WHERE (:ddoId IS NULL OR i.ddoId = :ddoId)
AND (:gstId IS NULL OR i.gstId = :gstId)
AND (:status IS NULL OR i.invoiceStatus = :status)
ORDER BY i.id DESC
""")
    List<Object[]> fetchInvoices(@Param("ddoId") Integer ddoId,
                                 @Param("gstId") Integer gstId,
                                 @Param("status") String status);

    Optional<InvoiceMaster> findTopByInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(String prefix, String saved);

    Optional<InvoiceMaster> findTopByFinancialYearAndInvoiceNumberStartingWithAndInvoiceStatusOrderByIdDesc(String financialYear, String prefix, String saved);

    Optional<InvoiceMaster> findTopByFinalInvoiceNumberStartingWithAndInvoiceStatusAndFinancialYearOrderByIdDesc(String prefix, String submitted, String fy);

    @Query("""
    SELECT i.ddoId, i.gstId, i.receiptNumber
    FROM InvoiceMaster i
    WHERE CONCAT(i.gstId, '-', i.ddoId) IN :keys
    ORDER BY i.id DESC
""")
    List<Object[]> findLastReceiptsForKeys(@Param("keys") Set<String> keys);

    @Query("""
    SELECT i 
    FROM InvoiceMaster i 
    WHERE i.id IN :ids
""")
    List<InvoiceMaster> findAllByIds(@Param("ids") List<Integer> ids);}
