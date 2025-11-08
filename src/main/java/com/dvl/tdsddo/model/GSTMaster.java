package com.dvl.tdsddo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "gst_master",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "gst_number")
        }
)
public class GSTMaster extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "gst_name", nullable = false, length = 150)
    private String gstName;

    @Column(name = "gst_holder_name")
    private String gstHolderName;

    @Column(name = "gst_number", nullable = false, unique = true)
    private String gstNumber;

    @Column(name = "state_code")
    private Integer stateCode;

//    @Column(name = "gst_address", nullable = false, length = 255)
//    private String gstAddress;

    @Column(name = "user_id")
    private Integer userId;

//    @Column(name = "gst_email", length = 100)
//    private String gstEmail;
//
//    @Column(name = "gst_mobile", length = 15)
//    private String gstMobile;

    @Column(name = "status", nullable = false, length = 20)
    @JsonIgnore
    private String status;
}
