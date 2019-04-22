package com.avarioncraft.shopsystem.tradesystem;

import java.util.UUID;

import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class TradeGood<T> {
	
	public final String tradeID;
	public final double value;
	public final int amount;
	protected final T good;
	@Getter
	private final UUID economyID;
	
	public abstract JsonObject asJson();
	public abstract void deliver(Player player);
	public abstract boolean has(Player player);
	public abstract void take(Player player);
}
