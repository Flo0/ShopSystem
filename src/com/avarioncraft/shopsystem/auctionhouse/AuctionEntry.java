package com.avarioncraft.shopsystem.auctionhouse;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.sql.DatabaseManager;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.utils.Eco;

import lombok.Getter;
import lombok.Setter;

public class AuctionEntry {
	
	public AuctionEntry(ItemStack item, double initialPrice, LocalDateTime auctionStart, LocalDateTime endDate, UUID auctionID, AuctionAccount owner) {
		this.ware = new ItemWare(item);
		this.initialPrice = initialPrice;
		this.auctionID = auctionID;
		this.owner = owner;
		this.ownerName = Bukkit.getOfflinePlayer(owner.getPlayerID()).getName();
		this.auctionStart = auctionStart;
		this.auctionEnd = endDate;
		this.server = ShopCore.getInstance().getServername();
	}
	
	@Getter
	private final UUID auctionID;
	
	@Getter
	private final ItemWare ware;
	
	@Getter
	private final double initialPrice;
	
	@Getter @Setter
	private double currentPrice;
	
	@Getter
	private final AuctionAccount owner;
	
	@Getter @Setter
	private AuctionAccount bidder;
	
	@Getter
	private LocalDateTime auctionStart;
	
	@Getter
	private LocalDateTime auctionEnd;
	
	@Getter
	private String server;
	
	@Getter
	private final String ownerName;
	
	public final RedisAuctionWrapper getWrappedAuction() {
		return new RedisAuctionWrapper(this, ShopCore.getInstance().getAuctionHouse());
	}
	
	@Getter @Setter
	private String currentBidderName = "§fKeinem";
	
	public double getProfitMargin() {
		return this.currentPrice - this.initialPrice;
	}
	
	public void cashOut() {
		Eco.give(Bukkit.getOfflinePlayer(this.owner.getPlayerID()), this.currentPrice);
	}
	
	public void distributeItems() {
		this.bidder.storeItem(this.ware.value);
	}
	
	public boolean isOver() {
		return LocalDateTime.now().isAfter(this.auctionEnd);
	}
	
	public boolean makeBid(AuctionAccount bidderAccount, double price) {
		if (price <= this.currentPrice) return false;
		if (this.isOver()) return false;
		if (!bidderAccount.has(price)) return false;
		
		if (this.bidder != null) {
			this.bidder.give(this.currentPrice);
			this.bidder.getBiddingAuctions().remove(this.auctionID);
		}
		
		bidderAccount.withdraw(price);
		this.currentBidderName = Bukkit.getOfflinePlayer(bidderAccount.getPlayerID()).getName();
		this.bidder = bidderAccount;
		this.bidder.getBiddingAuctions().add(this.auctionID);
		this.currentPrice = price;
		
		ShopCore plugin = ShopCore.getInstance();
		DatabaseManager dbMan = plugin.getDatabaseManager();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dbMan.updateAuction(this));
		
		return true;
	}

}
