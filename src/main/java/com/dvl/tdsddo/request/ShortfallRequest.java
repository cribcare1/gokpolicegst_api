package com.dvl.tdsddo.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShortfallRequest {
    private Integer invoiceId;
    private Double amount;
}