package com.avarioncraft.shopsystem.tradesystem;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BuyRequestResponse {
	
	NO_PERMISSION(false, "Nicht genügend Rechte"), 
	NO_MONEY(false, "Nicht genügend Geld"), 
	NO_SELLING(false, "Dieser Shop verkauft nicht"), 
	NO_STOCK(false, "Zu wenige Items vorhanden"), 
	NOT_ENABLED(false, "Dieser Shop ist momentan nicht verfügbar"), 
	WRONG_TIME(false, "Nicht zu dieser Uhrzeit erwerbbar"), 
	CREATIVE_BLOCK(false, "Nicht im creative möglich"), 
	SUCCESS(true, "Gekauft: x<amount> <ware> §c(- <money>)");
	
	@Getter
	private final boolean successfull;
	@Getter
	private final String message;
}
