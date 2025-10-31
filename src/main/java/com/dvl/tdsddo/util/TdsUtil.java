package com.dvl.tdsddo.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TdsUtil {
	public static LocalDateTime changeCurrentTimeToLocalDateTimeFromGmtToISTLocal() {
		// Get the current time in GMT (UTC)
		LocalDateTime gmtTime = LocalDateTime.now(ZoneOffset.UTC);

		// Convert GMT time to IST (Asia/Kolkata)
		ZoneId istZone = ZoneId.of("Asia/Kolkata");
		return gmtTime.atZone(ZoneOffset.UTC).withZoneSameInstant(istZone).toLocalDateTime();
	}
}
