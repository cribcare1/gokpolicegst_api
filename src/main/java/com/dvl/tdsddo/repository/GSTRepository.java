package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.GSTMaster;
import com.dvl.tdsddo.response.GSTResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.List;

public interface GSTRepository extends JpaRepository<GSTMaster,Integer> {
    GSTMaster findByGstNumber(@NotBlank(message = "GST number is required") @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GST number format") String gstNumber);

    List<GSTMaster> findByStatus(String status);

    GSTMaster findByUserIdAndStatus(Integer userId,String status);



    @Query("""
        SELECT new com.dvl.tdsddo.response.GSTResponse(
            u.id,
            u.email,
            u.address,
            u.mobileNumber,
            u.city,
            u.pinCode,
            g.id,
            g.gstNumber,
            g.gstHolderName,
            g.gstName,
             CAST(COUNT(d.id) AS int),
             g.stateCode
        )
        FROM GSTMaster g
         JOIN User u ON u.id = g.userId
        LEFT JOIN DdoGStMapping d ON d.fromGst = g.id AND d.status = 'active'
        WHERE g.status = 'active'
        GROUP BY 
            u.id, u.email, u.address, u.mobileNumber, u.city, u.pinCode,
            g.id, g.gstNumber, g.gstHolderName, g.gstName
        ORDER BY g.gstName
        """)
    List<GSTResponse> findAllActiveGstWithUserAndDdoCount();
}
