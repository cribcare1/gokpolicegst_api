package com.dvl.tdsddo.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNoteRequest {

    private String creditNoteNumber;

    private Double creditNoteAmount;
    private Double mismatchAmount;

    private String reason;
}

