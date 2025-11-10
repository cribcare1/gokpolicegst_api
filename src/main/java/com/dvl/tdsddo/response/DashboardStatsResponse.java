package com.dvl.tdsddo.response;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {
    private long totalPan;
    private long totalBankAccounts;
    private long totalDdo;
    private long totalGst;
    private long totalHsn;
}
