package com.dvl.tdsddo.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNoteResponse {
    private String creditNoteNumber;
    private BigDecimal creditNoteAmount;
    private BigDecimal mismatchAmount;
    private String reason;
}
