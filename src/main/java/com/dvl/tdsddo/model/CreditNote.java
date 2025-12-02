package com.dvl.tdsddo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNote extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer invoiceId;

    private String creditNoteNumber;

    private Double creditNoteAmount; // actual credit applied
    private Double mismatchAmount;   // system detected difference

    private String reason;           // human reason
}
