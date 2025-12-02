package com.dvl.tdsddo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gst_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GSTSnapshot extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer invoiceId;

    private String gstName;
    private String gstNumber;
    private String stateCode;
    private String gstHolderName;
}

