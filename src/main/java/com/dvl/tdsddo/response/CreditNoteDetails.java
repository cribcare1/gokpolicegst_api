package com.dvl.tdsddo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreditNoteDetails {
    private Integer id;

    private Integer invoiceId;

    private String creditNoteNumber;
    private String invoiceNumber;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate creditNoteDate;
    private Double baseAmount;       // original invoice amount

    private Double creditNoteAmount; // actual credit applied

    private String eInvoiceStatus;
    private String irnStatus;
    private String eInvoicePreView;
    private String reason;           // human reason
    private String customerName;
    private String finalInvoiceNumber;
}
