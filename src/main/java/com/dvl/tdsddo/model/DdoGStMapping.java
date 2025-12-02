package com.dvl.tdsddo.model;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ddo_gst_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DdoGStMapping extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ddo_id", nullable = false)
    private Integer ddoId;

    @Column(name = "from_gst",nullable = false)
    private Integer fromGst;

    @Column(name = "to_gst",nullable = false)
    private Integer toGST;

    @Column(name = "status", length = 10, nullable = false)
    private String status = TdsDdoConstant.ACTIVE;
}
