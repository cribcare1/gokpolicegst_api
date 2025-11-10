package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.BankDetailsMaster;
import com.dvl.tdsddo.response.BankDetailsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BankDetailsRepository extends JpaRepository<BankDetailsMaster,Integer> {
    boolean existsByAccountNumberAndStatus(String accountNumber, String status);

    List<BankDetailsMaster> findByStatus(String status);

    List<BankDetailsMaster> findByGstId(Integer gstId);

    @Query("""
        SELECT new com.dvl.tdsddo.response.BankDetailsResponse(
            b.id, b.bankName, b.branchName, b.accountNumber, b.accountType,
            b.accountName, b.ifscCode, b.micrCode, g.id, g.gstName
        )
        FROM BankDetailsMaster b
        JOIN GSTMaster g ON b.gstId = g.id
        WHERE (:gstId IS NULL OR b.gstId = :gstId)
        AND b.status = 'active'
        ORDER BY b.id DESC
    """)
    List<BankDetailsResponse> findAllActiveBanks(@Param("gstId") Integer gstId);
}
