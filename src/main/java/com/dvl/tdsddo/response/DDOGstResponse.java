package com.dvl.tdsddo.response;

import lombok.Getter;
import lombok.Setter;

public interface  DDOGstResponse{
    Integer getUserId();
    Integer getCurrentGstId();
    Integer getGstDdoMappingId();
    String getDdoName();
    String getMobile();
    String getEmail();
    String getDdoCode();
    String getCity();
    String getAddress();
    String getGstName();
    String getGstNumber();
    String getPinCode();
    String getArea();
    String getIsEditable();
    String getDdoTan();
    String getTanGstIn();
}
