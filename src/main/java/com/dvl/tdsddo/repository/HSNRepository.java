package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.HSNMaster;
import com.dvl.tdsddo.model.HsnGstHistory;
import com.dvl.tdsddo.response.HSNMasterDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface HSNRepository extends JpaRepository<HSNMaster,Integer> {

    Optional<HSNMaster> findByHsnCode(String hsnCode);
    List<HSNMaster> findByStatus(String status);


//    @Query("""
//    SELECT new com.dvl.tdsddo.response.HSNMasterDto(
//        h.id, h.hsnCode, h.gstId, h.serviceName,
//        h.totalGst, h.igst, h.cgst, h.sgst,
//        g.gstNumber,
//        h.updatedDate,
//        true
//    )
//    FROM HSNMaster h
//    LEFT JOIN GSTMaster g ON g.id = h.gstId
//    WHERE h.status = :status
//""")
//    List<HSNMasterDto> findAllHSN(@Param("status") String status);


    @Query("""
    SELECT new com.dvl.tdsddo.response.HSNMasterDto(
        h.id,
        h.hsnCode,
        h.gstId,
        h.serviceName,
        h.totalGst,
        h.igst,
        h.cgst,
        h.sgst,
        g.gstNumber,
        h.updatedDate,
        CASE 
            WHEN (EXISTS (
                SELECT 1 
                FROM InvoiceItem i 
                JOIN InvoiceMaster im ON i.invoiceId = im.id 
                WHERE i.hsnId = h.id
            )) THEN false
            ELSE true
        END
    )
    FROM HSNMaster h
    LEFT JOIN GSTMaster g ON g.id = h.gstId
    WHERE h.status = :status
""")
    List<HSNMasterDto> findAllHSN(@Param("status") String status);

    @Query("""
    SELECT new com.dvl.tdsddo.response.HSNMasterDto(
        h.id,
        h.hsnCode,
        h.gstId,
        h.serviceName,
        h.totalGst,
        h.igst,
        h.cgst,
        h.sgst,
        g.gstNumber,
        h.updatedDate,
        CASE 
            WHEN (EXISTS (
                SELECT 1 
                FROM InvoiceItem i 
                JOIN InvoiceMaster im ON i.invoiceId = im.id 
                WHERE i.hsnId = h.id
            )) THEN false
            ELSE true
        END
    )
    FROM HSNMaster h
    LEFT JOIN GSTMaster g ON g.id = h.gstId
    WHERE h.status = :status
    AND h.gstId =:gstId
""")
    List<HSNMasterDto> findAllHSNByGstId(Integer gstId,@Param("status") String status);
}
