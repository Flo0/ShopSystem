package com.avarioncraft.shopsystem.statistics;

import com.avarioncraft.shopsystem.adminshops.AdminShop.BuySell;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TransactionOccasion {
	
	ADMIN_SHOP_SELL(BuySell.SELL), ADMIN_SHOP_BUY(BuySell.BUY), PLAYER_SHOP_SELL(BuySell.SELL), PLAYER_SHOP_BUY(BuySell.BUY), AUCTION_SELL(BuySell.SELL), AUCTION_BUY(BuySell.BUY);
	
	@Getter
	private final BuySell buySell;
}
