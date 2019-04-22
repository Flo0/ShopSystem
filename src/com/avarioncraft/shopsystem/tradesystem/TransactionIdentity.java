package com.avarioncraft.shopsystem.tradesystem;

import java.util.UUID;

public enum TransactionIdentity {
	
	ADMINSHOP, PLAYERSHOP, AUCTION;
	
	public String getTransactionString(UUID seller, UUID buyer) {
		return this.toString() + "#" + System.currentTimeMillis() + "#" + seller.toString() + "#" + seller.toString();
	}
	
}
