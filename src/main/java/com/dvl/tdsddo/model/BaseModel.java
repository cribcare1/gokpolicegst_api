package com.dvl.tdsddo.model;

import com.dvl.tdsddo.util.TdsUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
public class BaseModel {
    @JsonIgnore
    private LocalDateTime createdDate;
    @JsonIgnore
    private LocalDateTime updatedDate;
    @JsonIgnore
    private Integer updateBy;
    @PrePersist
    protected void onCreate() {
        this.createdDate = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();
        this.updatedDate = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();
    }

    @PostUpdate
    protected void onUpdate() {
        this.updatedDate = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();
    }
}
