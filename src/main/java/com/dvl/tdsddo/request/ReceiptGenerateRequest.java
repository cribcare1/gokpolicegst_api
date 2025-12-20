package com.dvl.tdsddo.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReceiptGenerateRequest {
    private List<ReceiptEntry> entries;

    @Getter @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReceiptEntry {
        private Integer ddoId;
        private Integer gstId;
        @Override
        public String toString() {
            return "ReceiptEntry{gstId=" + gstId + ", ddoId=" + ddoId + "}";
        }


    }
}

