package com.avarioncraft.shopsystem.tradesystem;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SellRequestResponse {
	
	NO_PERMISSION(false, "Nicht gen�gend Rechte"), 
	MAX_STOCK(false, "Dieser Shop ist voll"), 
	NOT_ENOUGH_MONEY(false, "Dieser Shop hat kein Geld mehr"),
	NOT_ENOUGH_WARE(false, "Du hast nicht die n�tige Ware dazu"),
	NO_BUYING(false, "Dieser Shop kauft nichts"), 
	NOT_ENABLED(false, "Dieser Shop ist momentan nicht verf�gbar"), 
	WRONG_TIME(false, "Nicht zu dieser Uhrzeit benutzbar"), 
	CREATIVE_BLOCK(false, "Nicht im Creative m�glich"), 
	MONEY_ERROR(false, "Du kannst kein Geld mehr tragen"),
	SUCCESS(true, "Verkauft: x<amount> <ware> �e(+ <money>)");
	
	@Getter
	private final boolean successfull;
	@Getter
	private final String message;
}