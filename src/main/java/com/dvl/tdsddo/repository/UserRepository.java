package com.dvl.tdsddo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dvl.tdsddo.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {
	// Check if username already exists
	boolean existsByUserName(String userName);

	// Check if email already exists
	boolean existsByEmail(String email);

	// Check if mobile number already exists
	boolean existsByMobileNumber(String mobileNumber);

	boolean existsByDdoTan(String ddoTan);

	boolean existsByDdoCode(String ddoCode);

	// Optional method to fetch user by username
	Optional<User> findByUserName(String userName);

	List<User> findByRoleAndStatusAndAdminId(String role, String status, Integer adminId);

	List<User> findByRoleAndStatus(String role, String status);

	Page<User> findByRoleAndStatusAndAdminId(String role, String status, Integer adminId, Pageable pageable);

	@Query("select user from User user where user.email =:email and user.status ='active'")
	Optional<User> findByEmailIdAndStatus(String email);

	@Query("select user from User user where user.mobileNumber =:mobile and user.status ='active'")
	Optional<User> findByMobileNumberAndStatus(String mobile);

	@Query("select user from User user where user.userName =:userName and user.status ='active'")
	Optional<User> findByUserNameAndStatus(String userName);

}
