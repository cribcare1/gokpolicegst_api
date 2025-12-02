package com.dvl.tdsddo.model;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer invoiceId;

    private Integer hsnId;

    private String serviceName;

    private BigDecimal quantity;

    private BigDecimal rate;

    private BigDecimal amount;

    private BigDecimal igstRate;
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;

    private BigDecimal igstValue;
    private BigDecimal cgstValue;
    private BigDecimal sgstValue;
}
