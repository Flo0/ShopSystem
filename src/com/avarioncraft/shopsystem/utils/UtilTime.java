package com.avarioncraft.shopsystem.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class UtilTime {
	
	public static String diffBetween(LocalDateTime fromDateTime, LocalDateTime toDateTime) {

		LocalDateTime tempDateTime = LocalDateTime.from( fromDateTime );

		long years = tempDateTime.until( toDateTime, ChronoUnit.YEARS);
		tempDateTime = tempDateTime.plusYears( years );

		long months = tempDateTime.until( toDateTime, ChronoUnit.MONTHS);
		tempDateTime = tempDateTime.plusMonths( months );

		long days = tempDateTime.until( toDateTime, ChronoUnit.DAYS);
		tempDateTime = tempDateTime.plusDays( days );


		long hours = tempDateTime.until( toDateTime, ChronoUnit.HOURS);
		tempDateTime = tempDateTime.plusHours( hours );

		long minutes = tempDateTime.until( toDateTime, ChronoUnit.MINUTES);
		tempDateTime = tempDateTime.plusMinutes( minutes );

		long seconds = tempDateTime.until( toDateTime, ChronoUnit.SECONDS);
		
		StringBuilder timeBuilder = new StringBuilder();
		
		timeBuilder.append((years > 0 ? years + " Jahre " : ""));
		timeBuilder.append((months > 0 ? months + " Monate " : ""));
		timeBuilder.append((days > 0 ? days + " Tage " : ""));
		timeBuilder.append((hours > 0 ? hours + " Stunden " : ""));
		timeBuilder.append((minutes > 0 ? minutes + " Minuten " : ""));
		timeBuilder.append((seconds > 0 ? seconds + " Sekunden" : ""));
		
		return timeBuilder.toString();
	}
	
}
