package com.dvl.tdsddo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bank_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankSnapshot extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer invoiceId;

    private String bankName;
    private String branchName;

    private String accountNumber;
    private String ifscCode;
}
