package com.dvl.tdsddo.model;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customer_master")
public class CustomerMaster extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String customerType;
    private Integer ddoId;
    private String customerName;
    private String customerEmail;
    private String address;
    private String mobile;
    private String city;
    private String pinCode;
    private String stateCode;
    private String gstNumber;
    private String exemptionNumber;
    private String serviceType;
    private String status= TdsDdoConstant.ACTIVE;  // ACTIVE / INACTIVE
}
