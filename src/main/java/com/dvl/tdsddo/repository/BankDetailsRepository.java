package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.BankDetailsMaster;
import com.dvl.tdsddo.response.BankDetailsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankDetailsRepository extends JpaRepository<BankDetailsMaster,Integer> {
    boolean existsByAccountNumberAndStatus(String accountNumber, String status);

    List<BankDetailsMaster> findByStatus(String status);

    List<BankDetailsMaster> findByGstId(Integer gstId);

//    @Query("""
//        SELECT new com.dvl.tdsddo.response.BankDetailsResponse(
//            b.id, b.bankName, b.branchName, b.accountNumber, b.accountType,
//            b.accountName, b.ifscCode, b.micrCode, g.id, g.gstName,g.gstNumber,true,b.status
//        )
//        FROM BankDetailsMaster b
//        JOIN GSTMaster g ON b.gstId = g.id
//        WHERE (:gstId IS NULL OR b.gstId = :gstId)
//        ORDER BY b.id DESC
//    """)
//        //        AND b.status = 'active'
//    List<BankDetailsResponse> findAllActiveBanks(@Param("gstId") Integer gstId);

    @Query("""
    SELECT new com.dvl.tdsddo.response.BankDetailsResponse(
        b.id,
        b.bankName,
        b.branchName,
        b.accountNumber,
        b.accountType,
        b.accountName,
        b.ifscCode,
        b.micrCode,
        g.id,
        g.gstName,
        g.gstNumber,
        CASE WHEN COUNT(i.id) > 0 THEN false ELSE true END,
        b.status,
        b.effectiveDate
    )
    FROM BankDetailsMaster b
    JOIN GSTMaster g ON b.gstId = g.id
    LEFT JOIN InvoiceMaster i ON i.bankId = b.id
    WHERE (:gstId IS NULL OR b.gstId = :gstId) AND b.ddoId is null
    GROUP BY b.id, b.bankName, b.branchName, b.accountNumber, b.accountType, b.accountName, b.ifscCode, b.micrCode, g.id, g.gstName, g.gstNumber, b.status
    ORDER BY b.id DESC
""")
    List<BankDetailsResponse> findAllActiveBanks(@Param("gstId") Integer gstId);

    @Query("""
    SELECT new com.dvl.tdsddo.response.BankDetailsResponse(
        b.id,
        b.bankName,
        b.branchName,
        b.accountNumber,
        b.accountType,
        b.accountName,
        b.ifscCode,
        b.micrCode,
        g.id,
        g.gstName,
        g.gstNumber,
        CASE WHEN COUNT(i.id) > 0 THEN FALSE ELSE TRUE END,
        b.status,
        b.effectiveDate
    )
    FROM BankDetailsMaster b
   left JOIN GSTMaster g ON b.gstId = g.id
    LEFT JOIN InvoiceMaster i ON i.bankId = b.id
    WHERE b.ddoId = :ddoId
      AND b.status = 'active'
    GROUP BY b.id, b.bankName, b.branchName, b.accountNumber, b.accountType,
             b.accountName, b.ifscCode, b.micrCode, g.id, g.gstName, g.gstNumber,
             b.status, b.effectiveDate
    ORDER BY b.updatedDate DESC
""")
    List<BankDetailsResponse> findAllActiveBanksForDDO(@Param("ddoId") Integer ddoId);


    @Query("""
    SELECT new com.dvl.tdsddo.response.BankDetailsResponse(
        b.id,
        b.bankName,
        b.branchName,
        b.accountNumber,
        b.accountType,
        b.accountName,
        b.ifscCode,
        b.micrCode,
        g.id,
        g.gstName,
        g.gstNumber,
        CASE WHEN COUNT(i.id) = 0 THEN true ELSE false END,
        b.status,
        b.effectiveDate
    )
    FROM BankDetailsMaster b
    JOIN GSTMaster g ON b.gstId = g.id
    LEFT JOIN InvoiceMaster i ON i.bankId = b.id
    WHERE (:gstId IS NULL OR b.gstId = :gstId)
      AND b.status = 'active'
    GROUP BY b.id, b.bankName, b.branchName, b.accountNumber, b.accountType, b.accountName, b.ifscCode, b.micrCode,
             g.id, g.gstName, g.gstNumber, b.status, b.effectiveDate
    ORDER BY b.updatedDate DESC
""")
    BankDetailsResponse findActiveBankByGstId(@Param("gstId") Integer gstId);


    @Query("""
    SELECT new com.dvl.tdsddo.response.BankDetailsResponse(
        b.id,
        b.bankName,
        b.branchName,
        b.accountNumber,
        b.accountType,
        b.accountName,
        b.ifscCode,
        b.micrCode,
        g.id,
        g.gstName,
        g.gstNumber,
        CASE WHEN COUNT(i.id) = 0 THEN true ELSE false END,
        b.status,
        b.effectiveDate
    )
    FROM BankDetailsMaster b
    JOIN GSTMaster g ON b.gstId = g.id
    LEFT JOIN InvoiceMaster i ON i.bankId = b.id
    WHERE b.gstId = :gstId
      AND b.status = 'active'
      AND b.id = (
          SELECT MIN(b2.id)
          FROM BankDetailsMaster b2
          WHERE b2.gstId = :gstId
            AND b2.status = 'active'
      )
    GROUP BY b.id, b.bankName, b.branchName, b.accountNumber, b.accountType,
             b.accountName, b.ifscCode, b.micrCode,
             g.id, g.gstName, g.gstNumber, b.status, b.effectiveDate
""")
    Optional<BankDetailsResponse> findFirstActiveBankByGstId(
            @Param("gstId") Integer gstId
    );

    Optional<BankDetailsMaster> findByGstIdAndStatus(Integer gstId, String active);

    Optional<BankDetailsMaster> findByDdoIdAndStatus(Integer ddoId, String active);

    boolean existsByGstIdAndStatus(Integer gstId, String active);

    boolean existsByDdoIdAndStatus(Integer ddoId, String active);
}
