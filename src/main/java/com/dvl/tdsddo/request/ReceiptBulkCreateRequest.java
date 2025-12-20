package com.dvl.tdsddo.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReceiptBulkCreateRequest {
    private List<ReceiptCreationRequest> receipts;
}
