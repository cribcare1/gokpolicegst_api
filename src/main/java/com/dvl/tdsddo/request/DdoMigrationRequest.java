package com.dvl.tdsddo.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DdoMigrationRequest {

    @NotNull(message = "From GST ID is required")
    private Integer fromGstId;

    @NotNull(message = "To GST ID is required")
    private Integer toGstId;

    @NotEmpty(message = "List of DDO IDs is required")
    private List<Integer> ddoIds;

   // @NotNull(message = "Updated by (admin ID) is required")
    private Integer updatedBy;
}
