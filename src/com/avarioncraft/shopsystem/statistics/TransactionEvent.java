package com.avarioncraft.shopsystem.statistics;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.avarioncraft.shopsystem.tradesystem.TradeGood;

import lombok.Getter;
import lombok.Setter;

public class TransactionEvent extends Event{

	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}
	
	public TransactionEvent(UUID buyer, @Nullable UUID seller, @Nullable UUID shopID, TradeGood<?> tradeGood, double transactionBasePrice, double weight, TransactionOccasion occasion) {
		this.buyerID = buyer;
		this.sellerID = seller;
		this.shopID = shopID;
		this.tradeGood = tradeGood;
		this.transactionMoney = transactionBasePrice;
		this.weight = weight;
		this.occasion = occasion;
	}
	
	@Getter
	private final UUID buyerID;
	@Getter
	private final UUID sellerID;
	@Getter
	private final UUID shopID;
	@Getter @Setter
	private double transactionMoney;
	@Getter
	private final TradeGood<?> tradeGood;
	@Getter @Setter
	private double weight;
	@Getter
	private final TransactionOccasion occasion;
}
