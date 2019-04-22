package com.avarioncraft.shopsystem.tradesystem;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ShopHolder {
	
	public abstract TradeWare<?> getTradeWare();
	public abstract BuyRequestResponse playerBuyRequest(Player player, int amount);
	public abstract SellRequestResponse playerSellRequest(Player player, int amount);
	public abstract ArrayList<Supplier<String>> getShopInfo(Location location);
	public abstract UUID getShopHolderID();
	
}
