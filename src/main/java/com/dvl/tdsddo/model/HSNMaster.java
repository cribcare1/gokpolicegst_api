package com.dvl.tdsddo.model;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hsn_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HSNMaster extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hsn_code", nullable = false, unique = true, length = 50)
    private String hsnCode;

    private Integer gstId;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    private String totalGst;

    @Column(name = "igst")
    private String igst;

    @Column(name = "cgst")
    private String cgst;

    @Column(name = "sgst")
    private String sgst;

    @Column(name = "status", nullable = false, length = 10)
    @JsonIgnore
    private String status = TdsDdoConstant.ACTIVE;
}
