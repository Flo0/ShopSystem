package com.avarioncraft.shopsystem.tradesystem;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SellRequestResponse {
	
	NO_PERMISSION(false, "Nicht genügend Rechte"), 
	MAX_STOCK(false, "Dieser Shop ist voll"), 
	NOT_ENOUGH_MONEY(false, "Dieser Shop hat kein Geld mehr"),
	NOT_ENOUGH_WARE(false, "Du hast nicht die nötige Ware dazu"),
	NO_BUYING(false, "Dieser Shop kauft nichts"), 
	NOT_ENABLED(false, "Dieser Shop ist momentan nicht verfügbar"), 
	WRONG_TIME(false, "Nicht zu dieser Uhrzeit benutzbar"), 
	CREATIVE_BLOCK(false, "Nicht im Creative möglich"), 
	MONEY_ERROR(false, "Du kannst kein Geld mehr tragen"),
	SUCCESS(true, "Verkauft: x<amount> <ware> §e(+ <money>)");
	
	@Getter
	private final boolean successfull;
	@Getter
	private final String message;
}