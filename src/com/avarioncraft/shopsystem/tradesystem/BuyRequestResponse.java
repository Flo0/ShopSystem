package com.avarioncraft.shopsystem.tradesystem;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BuyRequestResponse {
	
	NO_PERMISSION(false, "Nicht gen�gend Rechte"), 
	NO_MONEY(false, "Nicht gen�gend Geld"), 
	NO_SELLING(false, "Dieser Shop verkauft nicht"), 
	NO_STOCK(false, "Zu wenige Items vorhanden"), 
	NOT_ENABLED(false, "Dieser Shop ist momentan nicht verf�gbar"), 
	WRONG_TIME(false, "Nicht zu dieser Uhrzeit erwerbbar"), 
	CREATIVE_BLOCK(false, "Nicht im creative m�glich"), 
	SUCCESS(true, "Gekauft: x<amount> <ware> �c(- <money>)");
	
	@Getter
	private final boolean successfull;
	@Getter
	private final String message;
}
