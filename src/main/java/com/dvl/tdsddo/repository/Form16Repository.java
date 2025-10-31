package com.dvl.tdsddo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dvl.tdsddo.model.Form16;
import com.dvl.tdsddo.response.Form16Response;

public interface Form16Repository extends JpaRepository<Form16, Integer> {

	Optional<Form16> findByPanNumberAndFinancialYear(String panNumber, String financialYear);

	@Query("SELECT new com.dvl.tdsddo.response.Form16Response(f.id, f.financialYear, f.name, f.panNumber, f.mobileNumber, f.filePath) "
			+ "FROM Form16 f WHERE f.panNumber = :panNumber ORDER BY f.financialYear DESC")
	List<Form16Response> findTop3ByPanNumber(@Param("panNumber") String panNumber, Pageable pageable);

}
