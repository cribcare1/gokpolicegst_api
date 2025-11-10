package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.DdoGStMapping;
import com.dvl.tdsddo.response.DDOGstResponse;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DdoGstMappingRepository extends JpaRepository<DdoGStMapping,Integer> {
    Optional<DdoGStMapping> findByDdoIdAndStatus(Integer id, String active);

    List<DdoGStMapping> findAllByDdoIdInAndStatus(@NotEmpty(message = "List of DDO IDs is required") List<Integer> ddoIds, String active);

    @Query(value = """
        SELECT 
            u.id AS userId,
            m.to_gst AS currentGstId,
            m.id AS gstDdoMappingId,
            u.full_name AS ddoName,
            u.mobile_number AS mobile,
            u.email AS email,
            u.ddo_code AS ddoCode,
            u.city AS city,
            u.address AS address,
            g.gst_name AS gstName,
            g.gst_number AS gstNumber,
            u.pin AS pinCode
        FROM 
            ddo_gst_mapping m
        JOIN 
            users u ON u.id = m.ddo_id
        JOIN 
            gst_master g ON g.id = m.to_gst
        WHERE 
            m.status = 'active'
        """, nativeQuery = true)
    List<DDOGstResponse> findAllActiveDdoGstMappings();

    @Query(value = """
    SELECT 
        u.id AS userId,
        m.to_gst AS currentGstId,
        m.id AS gstDdoMappingId,
        u.full_name AS ddoName,
        u.mobile_number AS mobile,
        u.email AS email,
        u.ddo_code AS ddoCode,
        u.city AS city,
        u.address AS address,
        g.gst_name AS gstName,
        g.gst_number AS gstNumber,
        u.pin AS pinCode
    FROM 
        ddo_gst_mapping m
    JOIN 
        users u ON u.id = m.ddo_id
    JOIN 
        gst_master g ON g.id = m.to_gst
    WHERE 
        m.status = 'active' AND m.to_gst = :gstId
    """, nativeQuery = true)
    List<DDOGstResponse> findDdosByToGst(@Param("gstId") Integer gstId);

    @Query(value = """
        SELECT 
            u.id AS userId,
            m.to_gst AS currentGstId,
            m.id AS gstDdoMappingId,
            u.full_name AS ddoName,
            u.mobile_number AS mobile,
            u.email AS email,
            u.ddo_code AS ddoCode,
            u.city AS city,
            u.address AS address,
            g.gst_name AS gstName,
            g.gst_number AS gstNumber,
            u.pin AS pinCode
        FROM 
            ddo_gst_mapping m
        JOIN 
            users u ON u.id = m.ddo_id AND u.status = 'active'
        JOIN 
            gst_master g ON g.id = m.to_gst
        WHERE 
            m.status = 'active'
            AND (:gstId IS NULL OR m.to_gst = :gstId)
        """, nativeQuery = true)
    List<DDOGstResponse> findAllActiveDdosByGstId(@Param("gstId") Integer gstId);}
