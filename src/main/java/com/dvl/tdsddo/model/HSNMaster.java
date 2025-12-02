package com.dvl.tdsddo.model;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "gst_id")
    private Integer gstId;

    // 🟢 Relation (Many banks can be linked to one GST)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gst_id", insertable = false, updatable = false)
    @JsonIgnore
    private GSTMaster gstMaster;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    private String totalGst;

    @Column(name = "igst")
    private String igst;

    @Column(name = "cgst")
    private String cgst;

    @Column(name = "sgst")
    private String sgst;

    @Transient
    private String gstNumber;

    @Transient
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime effectiveDate;


    @Transient
    private Boolean isEditable=Boolean.TRUE;

    @Column(name = "status", nullable = false, length = 10)
    @JsonIgnore
    private String status = TdsDdoConstant.ACTIVE;

    @PostLoad
    public void loadTransientFields() {

        // Set effective date from BaseModel.updatedDate
        if (this.getUpdatedDate() != null) {
            this.effectiveDate = this.getUpdatedDate();
        }
        // Set GST Number from relation
        if (gstMaster != null) {
            this.gstNumber = gstMaster.getGstNumber();
        }
    }

}
