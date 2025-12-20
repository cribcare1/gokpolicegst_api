package com.dvl.tdsddo.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.dvl.tdsddo.response.DDOCurrentGstResponse;
import com.dvl.tdsddo.response.DashBoardresponse;
import com.dvl.tdsddo.response.DashboardStatsResponse;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dvl.tdsddo.model.User;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {
	// Check if username already exists
	boolean existsByUserName(String userName);

	// Check if email already exists
	boolean existsByEmail(String email);

	// Check if mobile number already exists
	boolean existsByMobileNumber(String mobileNumber);

	boolean existsByDdoTan(String ddoTan);

	boolean existsByDdoCode(String ddoCode);
    boolean existsByDdoTanAndStatus(String ddoTan,String Status);
	// Optional method to fetch user by username
	Optional<User> findByUserName(String userName);

//	List<User> findByRoleAndStatusAndAdminId(String role, String status, Integer adminId);

	List<User> findByRoleAndStatus(String role, String status);

	Page<User> findByRoleAndStatusAndCreatedBy(String role, String status, Integer adminId, Pageable pageable);

	@Query("select user from User user where user.email =:email and user.status ='active'")
	Optional<User> findByEmailIdAndStatus(String email);

	@Query("select user from User user where user.mobileNumber =:mobile and user.status ='active'")
	Optional<User> findByMobileNumberAndStatus(String mobile);

	@Query("select user from User user where user.userName =:userName and user.status ='active'")
	Optional<User> findByUserNameAndStatus(String userName);




    @Query("""
    SELECT new com.dvl.tdsddo.response.DashboardStatsResponse(
        (SELECT COUNT(p) FROM PanMaster p WHERE p.status = 'active'),
        (SELECT COUNT(b) FROM BankDetailsMaster b WHERE b.status = 'active'),
        (SELECT COUNT(u) FROM User u WHERE u.status = 'active' AND u.role = 'DDO'),
        (SELECT COUNT(g) FROM GSTMaster g WHERE g.status = 'active'),
        (SELECT COUNT(h) FROM HSNMaster h WHERE h.status = 'active'),
        (SELECT COUNT(i) FROM InvoiceMaster i WHERE i.invoiceStatus = 'SAVE'),
        (SELECT COUNT(i) FROM InvoiceMaster i WHERE i.invoiceStatus = 'SUBMITTED')
    )
""")
    DashboardStatsResponse getActiveDashboardCounts();



    //        WHERE u.role = 'DDO'


    @Query("""
    SELECT new com.dvl.tdsddo.response.DashboardStatsResponse(
        (SELECT COUNT(p) FROM PanMaster p WHERE p.status = 'active'),
        (SELECT COUNT(b) FROM BankDetailsMaster b WHERE b.status = 'active' AND b.gstId = :gstId),
        (SELECT COUNT(u) FROM User u 
            JOIN DdoGStMapping d ON u.id = d.ddoId 
            WHERE u.status = 'active' AND u.role = 'DDO' AND d.toGST = :gstId AND d.status = 'active'),
        (SELECT COUNT(g) FROM GSTMaster g WHERE g.status = 'active' AND g.id = :gstId),
        (SELECT COUNT(h) FROM HSNMaster h WHERE h.status = 'active' AND h.gstId = :gstId),
        (SELECT COUNT(i) FROM InvoiceMaster i WHERE i.gstId = :gstId AND i.invoiceStatus = 'SAVED'),
        (SELECT COUNT(i) FROM InvoiceMaster i WHERE i.gstId = :gstId AND i.invoiceStatus = 'SUBMITTED')
    )
""")
    DashboardStatsResponse getDashboardStatsByGst(@Param("gstId") Integer gstId);


    boolean existsByDdoCodeAndIdNot(String ddoCode, Integer id);

    boolean existsByMobileNumberAndIdNot(@Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits") String mobile, Integer id);


    boolean existsByDdoTanAndStatusAndIdNot(String ddoTan,String Status,Integer id);

    @Query(value = """
    SELECT 
        u.id AS ddoId,
        u.full_name AS ddoName,
        u.mobile_number AS mobile,
        u.email AS email,
        u.ddo_code AS ddoCode,
        u.address AS address,
        u.city AS city,
        u.pin AS pinCode,
        u.area AS area,
        g.id AS gstId,
        g.gst_name AS gstName,
        g.gst_number AS gstNumber,
        g.gst_holder_name AS gstHolderName,
        g.gst_image AS gstImage,
        g.state_code AS stateCode
    FROM 
        ddo_gst_mapping m
    JOIN 
        users u ON u.id = m.ddo_id
    JOIN 
        gst_master g ON g.id = m.to_gst
    WHERE 
        m.status = 'active' AND u.id = :ddoId
    LIMIT 1
""", nativeQuery = true)
    Optional<DDOCurrentGstResponse> findCurrentGstByDdoId(@Param("ddoId") Integer ddoId);

    List<User> findByIdIn(Set<Integer> ids);
}
