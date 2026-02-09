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
       WHERE i.gstId = :gstId AND i.ddoId = :ddoId 
       AND i.receiptNumber IS NOT NULL
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
         AND (i.isShortfall is NULL OR i.isShortfall = false)
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


    @Query("""
SELECT i, gs, bs, c, cn, ii, hsn
FROM InvoiceMaster i
LEFT JOIN GSTSnapshot gs ON gs.invoiceId = i.id
LEFT JOIN BankSnapshot bs ON bs.invoiceId = i.id
LEFT JOIN CustomerMaster c ON c.id = i.customerId
LEFT JOIN CreditNote cn ON cn.invoiceId = i.id
LEFT JOIN InvoiceItem ii ON ii.invoiceId = i.id
LEFT JOIN HSNMaster hsn ON hsn.id = ii.hsnId
WHERE (:ddoId IS NULL OR i.ddoId = :ddoId)
  AND (:gstId IS NULL OR i.gstId = :gstId)
  AND (:status IS NULL OR i.invoiceStatus = :status)
  AND (:isShortfall IS NULL OR i.isShortfall = :isShortfall)
  AND i.status NOT IN ('delete')
ORDER BY i.id DESC
""")
    List<Object[]> fetchInvoices(
            @Param("ddoId") Integer ddoId,
            @Param("gstId") Integer gstId,
            @Param("status") String status,
            @Param("isShortfall") Boolean isShortfall
    );


//    @Query("""
//SELECT i, gs, bs, c, cn, ii, hsn
//FROM InvoiceMaster i
//LEFT JOIN GSTSnapshot gs ON gs.invoiceId = i.id
//LEFT JOIN BankSnapshot bs ON bs.invoiceId = i.id
//LEFT JOIN CustomerMaster c ON c.id = i.customerId
//LEFT JOIN CreditNote cn ON cn.invoiceId = i.id
//LEFT JOIN InvoiceItem ii ON ii.invoiceId = i.id
//LEFT JOIN HSNMaster hsn ON hsn.id = ii.hsnId
//WHERE (:ddoId IS NULL OR i.ddoId = :ddoId)
//  AND (:gstId IS NULL OR i.gstId = :gstId)
//  AND (:status IS NULL OR i.invoiceStatus = :status)
//  AND i.status NOT IN ('delete')
//ORDER BY i.id DESC
//""")
//    List<Object[]> fetchInvoices(
//            @Param("ddoId") Integer ddoId,
//            @Param("gstId") Integer gstId,
//            @Param("status") String status
//    );
//TODO
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
    List<InvoiceMaster> findAllByIds(@Param("ids") List<Integer> ids);

    @Query("""
SELECT i, gs, bs, cn, ii
FROM InvoiceMaster i
LEFT JOIN GSTSnapshot gs ON gs.invoiceId = i.id
LEFT JOIN BankSnapshot bs ON bs.invoiceId = i.id
LEFT JOIN CreditNote cn ON cn.invoiceId = i.id
LEFT JOIN InvoiceItem ii ON ii.invoiceId = i.id
WHERE i.id IN :invoiceIds
""")
    List<Object[]> fetchInvoicesForShortfall(
            @Param("invoiceIds") List<Integer> invoiceIds);

    @Query("""
    SELECT DISTINCT i.customerId
    FROM InvoiceMaster i
    WHERE i.customerId IN :customerIds
""")
    List<Integer> findCustomerIdsWithInvoices(
            @Param("customerIds") List<Integer> customerIds
    );

}
