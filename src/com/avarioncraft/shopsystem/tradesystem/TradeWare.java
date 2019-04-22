package com.avarioncraft.shopsystem.tradesystem;

import java.lang.reflect.Type;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public abstract class TradeWare<T> {

	public TradeWare(T value) {
		this.value = value;
	}
	
	public T value;

	public abstract TradeGood<T> getGood(String tradeID, double value, int amount, UUID economyID);

	public abstract Type getType();

	public abstract ItemStack getIcon();
	
	public abstract String getWareDisplayName();
	
	public abstract int getAmount();
}
