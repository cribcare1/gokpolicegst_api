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
public class PanMaster extends BaseModel{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String panName;
    private String panNumber;
    private String address;
    private String email;
    private String mobile;

    private String pinCode;
    private String city;

    @Transient
    private  Boolean isEditable=Boolean.TRUE;

    @JsonIgnore
    private Integer createdBy;
    @JsonIgnore
    private String status;
}
