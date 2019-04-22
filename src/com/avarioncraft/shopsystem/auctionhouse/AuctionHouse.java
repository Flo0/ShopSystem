package com.avarioncraft.shopsystem.auctionhouse;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.auctionhouse.AuctionHouseGUI.AuctionSortOrder;
import com.avarioncraft.shopsystem.statistics.TransactionEvent;
import com.avarioncraft.shopsystem.statistics.TransactionOccasion;
import com.avarioncraft.shopsystem.tradesystem.TransactionIdentity;
import com.google.common.collect.Maps;

import lombok.Getter;
import net.crytec.api.util.F;
import net.crytec.api.util.UtilPlayer;

public class AuctionHouse implements Runnable{
	
	public AuctionHouse(AuctionAccountManager accountManager) {
		this.auctionEntrys = Maps.newHashMap();
		this.accountManager = accountManager;
		this.auctionGUI = new AuctionHouseGUI(this);
		
		Bukkit.getScheduler().runTaskTimer(ShopCore.getInstance(), this, 20, 200);
	}
	
	// AuctionID, AuctionEntry
	private final Map<UUID, AuctionEntry> auctionEntrys;
	@Getter
	private final AuctionAccountManager accountManager;
	@Getter
	private final AuctionHouseGUI auctionGUI;
	
	public void openAuctionFor(Player player) {
		this.openAuctionFor(player, AuctionSortOrder.NONE);
	}
	
	public void load() {
		ShopCore.getInstance().getLogger().info("Loading auctions...");
		ShopCore.getInstance().getDatabaseManager().loadAuctionsForServer(ShopCore.getInstance().getServername()).forEach(wrapper -> {
			this.auctionEntrys.put(wrapper.getAuctionID(), wrapper.asAuctionEntry());
		});
		ShopCore.getInstance().getLogger().info("Loading " + this.auctionEntrys.size() + " auctions.");
	}
	
	private void checkRunouts() {
		
		ShopCore.getTaskChainFactory().newChain()
		.asyncFirst(() -> ShopCore.getInstance().getDatabaseManager().getAndDeleteExpiredAuctions(ShopCore.getInstance().getServername()))
		.abortIf(set -> set.isEmpty())
		.syncLast(set -> set.forEach(id -> {
			this.endAuction(id);
		}))
		.execute();
		
	}
	
	public void openAuctionFor(Player player, AuctionSortOrder order) {
		this.auctionGUI.openFor(player, order);
	}
	
	public boolean isValidAuction(UUID auctionID) {
		return auctionEntrys.containsKey(auctionID);
	}
	
	public AuctionEntry getAuctionEntry(UUID auctionID) {
		return this.auctionEntrys.get(auctionID);
	}
	
	public void startAuction(Player player, ItemStack item, int timeMin, double startPrice) {
		
		long sec = timeMin * 60;
		AuctionAccount ownerAccount = this.accountManager.getAccountOf(player.getUniqueId()).get();
		AuctionEntry entry = new AuctionEntry(item, startPrice, LocalDateTime.now(), LocalDateTime.now().plusSeconds(sec), UUID.randomUUID(), ownerAccount);
		ownerAccount.getOwnedAuctions().add(entry.getAuctionID());
		entry.setBidder(this.accountManager.getAccountOf(player.getUniqueId()).get());
		entry.setCurrentPrice(startPrice);
		this.auctionEntrys.put(entry.getAuctionID(), entry);
		ShopCore.getInstance().getDatabaseManager().createAuction(entry);
		
	}
	
	public void endAuction(UUID auctionID) {
		AuctionEntry auction = this.auctionEntrys.get(auctionID);
		
		auction.cashOut();
		auction.distributeItems();
		
		ShopCore.getInstance().getDatabaseManager().deleteAuction(auction);
		
		AuctionAccount ownerAccount = auction.getOwner();
		if(ownerAccount.isPlayerOnline()) {
			
			Player owner = Bukkit.getPlayer(ownerAccount.getPlayerID());
			owner.sendMessage(F.main("Auktionen", "Eine deiner Auktionen ist ausgelaufen."));
			owner.sendMessage(F.main("Auktionen", "" + auction.getWare().getWareDisplayName() + " §ffür §e" + auction.getCurrentPrice() + "$ §fvon §e" + auction.getCurrentBidderName()));
			UtilPlayer.playSound(owner, Sound.BLOCK_NOTE_BLOCK_PLING, 0.75F, 1.2F);
		}
		ownerAccount.getOwnedAuctions().remove(auctionID);
		
		AuctionAccount bidderAccount = auction.getBidder();
		if(bidderAccount.isPlayerOnline() && !bidderAccount.equals(ownerAccount)) {
			
			Player bidder = Bukkit.getPlayer(bidderAccount.getPlayerID());
			bidder.sendMessage(F.main("Auktionen", "Du hast eine Auktion gewonnen."));
			bidder.sendMessage(F.main("Auktionen", "" + auction.getWare().getWareDisplayName() + " §7für §e" + auction.getCurrentPrice() + "$"));
			bidder.sendMessage(F.main("Auktionen", "Hole die Ware an deinem §eAuktionskonto§7 ab."));
			UtilPlayer.playSound(bidder, Sound.BLOCK_NOTE_BLOCK_PLING, 0.75F, 1.2F);
		}
		bidderAccount.getBiddingAuctions().remove(auctionID);
		
		if(bidderAccount != ownerAccount) {
			new TransactionEvent(bidderAccount.getPlayerID(),
					ownerAccount.getPlayerID(),
					auctionID,
					auction.getWare().getGood(TransactionIdentity.AUCTION.getTransactionString(ownerAccount.getPlayerID(), bidderAccount.getPlayerID()), auction.getCurrentPrice(), 1, auctionID),
					0D, 0D, TransactionOccasion.AUCTION_SELL).callEvent();
			
			//Log successfully auction
			ShopCore.getInstance().getDatabaseManager().logAuction(auction);
		}
		
		if(!bidderAccount.hasReference()) {
			accountManager.unloadAccount(bidderAccount.getPlayerID());
		}
		if(!ownerAccount.hasReference()) {
			accountManager.unloadAccount(ownerAccount.getPlayerID());
		}
		
		this.auctionEntrys.remove(auctionID);
	}

	@Override
	public void run() {
		this.checkRunouts();
		
	}
	
	
}
