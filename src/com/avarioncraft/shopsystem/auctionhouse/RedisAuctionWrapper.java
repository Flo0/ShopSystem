package com.avarioncraft.shopsystem.auctionhouse;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.auctionhouse.AuctionHouseGUI.AuctionSortOrder;
import com.avarioncraft.shopsystem.utils.UtilTime;
import com.google.gson.JsonObject;

import lombok.Getter;
import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.smartInv.ClickableItem;
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.util.F;
import net.crytec.api.util.UtilInv;
import net.crytec.api.util.UtilMath;
import net.crytec.api.util.UtilPlayer;
import net.crytec.api.util.anvilgui.AnvilGUI;
import net.crytec.api.util.language.LanguageHelper;

public class RedisAuctionWrapper {
	
	public RedisAuctionWrapper(JsonObject json, AuctionHouse serverHouse) {
		this.serverHouse = serverHouse;
		this.item = UtilInv.deserializeItemStack(json.get("Item").getAsString());
		this.name = LanguageHelper.getItemDisplayName(this.item);
		this.amount = this.item.getAmount();
		this.basePrice = json.get("BasePrice").getAsDouble();
		this.currentPrice = json.get("CurrentPrice").getAsDouble();
		this.profitMargin = json.get("ProfitMargin").getAsDouble();
		this.ownerName = json.get("OwnerName").getAsString();
		this.bidderName = json.get("BidderName").getAsString();
		this.auctionID = UUID.fromString(json.get("AuctionID").getAsString());
		this.runoutTime = LocalDateTime.parse(json.get("Runout").getAsString());
		this.creationTime = LocalDateTime.parse(json.get("Start").getAsString());
		this.ownerID = UUID.fromString(json.get("OwnerID").getAsString());
		this.bidderID = UUID.fromString(json.get("BidderID").getAsString());
		this.server = json.get("Server").getAsString();
	}
	
	public RedisAuctionWrapper(AuctionEntry serverLinkedEntry, AuctionHouse serverHouse) {
		this.serverHouse = serverHouse;
		this.item = serverLinkedEntry.getWare().value;
		this.name = LanguageHelper.getItemDisplayName(this.item);
		this.amount = this.item.getAmount();
		this.basePrice = serverLinkedEntry.getInitialPrice();
		this.currentPrice = serverLinkedEntry.getCurrentPrice();
		this.profitMargin = serverLinkedEntry.getProfitMargin();
		this.ownerName = serverLinkedEntry.getOwnerName();
		this.bidderName = serverLinkedEntry.getCurrentBidderName();
		this.auctionID = serverLinkedEntry.getAuctionID();
		this.runoutTime = serverLinkedEntry.getAuctionEnd();
		this.creationTime = serverLinkedEntry.getAuctionStart();
		this.ownerID = serverLinkedEntry.getOwner().getPlayerID();
		this.bidderID = serverLinkedEntry.getBidder().getPlayerID();
		this.server = serverLinkedEntry.getServer();
	}
	
	private final AuctionHouse serverHouse;
	
	@Getter
	private final String name;
	@Getter
	private final int amount;
	@Getter
	private final double basePrice;
	@Getter
	private final double currentPrice;
	@Getter
	private final double profitMargin;
	@Getter
	private final String ownerName;
	@Getter
	private final String bidderName;
	@Getter
	private final ItemStack item;
	@Getter
	private final UUID auctionID;
	@Getter
	private final LocalDateTime runoutTime;
	@Getter
	private final LocalDateTime creationTime;
	@Getter
	private final UUID bidderID;
	@Getter
	private final UUID ownerID;
	@Getter
	private final String server;
	
	public boolean isBuyable() {
		return this.serverHouse.isValidAuction(this.auctionID);
	}
	
	public JsonObject asJson() {
		JsonObject json = new JsonObject();
		
		json.addProperty("Item", UtilInv.serializeItemStack(this.item));
		json.addProperty("BasePrice", this.basePrice);
		json.addProperty("CurrentPrice", this.currentPrice);
		json.addProperty("ProfitMargin", this.profitMargin);
		json.addProperty("OwnerName", this.ownerName);
		json.addProperty("BidderName", this.bidderName);
		json.addProperty("AuctionID", this.auctionID.toString());
		json.addProperty("Runout", this.runoutTime.toString());
		json.addProperty("Start", this.creationTime.toString());
		json.addProperty("OwnerID", this.ownerID.toString());
		json.addProperty("BidderID", this.bidderID.toString());
		json.addProperty("Server", this.server);
		
		return json;
	}
	
	public AuctionEntry asAuctionEntry() {
		AuctionAccount owner = ShopCore.getInstance().getAuctionHouse().getAccountManager().loadFromDisk(this.ownerID);
		AuctionAccount bidder = ShopCore.getInstance().getAuctionHouse().getAccountManager().loadFromDisk(this.bidderID);
		
		AuctionEntry entry = new AuctionEntry(getItem(), getBasePrice(), getCreationTime(), getRunoutTime(), getAuctionID(), owner);
		entry.setBidder(bidder);
		entry.setCurrentPrice(getCurrentPrice());
		return entry;
	}
	
	@Override
	public String toString() {
		return this.asJson().toString();
	}
	
	
	public Supplier<ClickableItem> getAuctionIcon(Player player, InventoryContents contents, InventoryProvider provider, AuctionSortOrder order){
		return () ->{
			return this.getCurrentIcon(player, contents, provider, order);
		};
	}
	
	
	private ClickableItem getCurrentIcon(Player player, InventoryContents contents, InventoryProvider provider, AuctionSortOrder order) {
		ItemBuilder builder = new ItemBuilder(this.item.clone());
		
		builder.lore("");
		builder.lore("§7<-------- §e⚖ §fAuktion§e ⚖ §7-------->");
		builder.lore("");
		builder.lore("§eServer: §7" + this.server);
		builder.lore("§eVerkäufer: §7" + this.ownerName);
		builder.lore("§eStartpreis: §7" + this.basePrice + "$");
		builder.lore("§eHöchstes Gebot:");
		builder.lore("");
		builder.lore("§f" + this.currentPrice + "$ §7von §f" + this.bidderName);
		builder.lore("");
		builder.lore("§eLäuft aus in: §7");
		builder.lore("§7" + UtilTime.diffBetween(LocalDateTime.now(), this.runoutTime));
		builder.lore("");
		builder.lore("§6Linksklicke, um zu bieten.");
		
		return ClickableItem.of(builder.build(), (event) ->{
			
			UtilPlayer.playSound(player, Sound.BLOCK_COMPARATOR_CLICK, 0.7F, 0.7F);
			
			if(!this.isBuyable()) {
				player.sendMessage(F.error("Diese Auktion befindet sich auf einem anderen Server oder ist bereits ausgelaufen."));
				return;
			}
			
			UUID buyerID = player.getUniqueId();
			
			if(buyerID.equals(this.ownerID)) {
				player.sendMessage(F.error("Du kannst nicht auf deinen eigenen Auktionen bieten."));
				return;
			}
			
			if(buyerID.equals(this.bidderID)) {
				player.sendMessage(F.error("Du kannst dich nicht selbst überbieten."));
				return;
			}
			
			new AnvilGUI(player, "" + this.currentPrice, (pl, input) -> {
				
				if(UtilMath.isDouble(input)) {
					if(this.serverHouse.getAuctionEntry(this.auctionID)
							.makeBid(this.serverHouse.getAccountManager().getAccountOf(pl.getUniqueId()).get(), Double.parseDouble(input))) {
						player.sendMessage(F.main("Auktionshaus", "Gebot wurde abgegeben."));
						Bukkit.getScheduler().runTaskLater(ShopCore.getInstance(), () -> this.serverHouse.openAuctionFor(player, order), 1);
						UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.35F);
					}else {
						player.sendMessage(F.error("Gebot ist ungültig. Nicht genügend Geld oder zu niedrig."));
						Bukkit.getScheduler().runTaskLater(ShopCore.getInstance(), () -> provider.reOpen(player, contents), 1);
						UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
					}
				}else {
					player.sendMessage(F.error("Du kannst nur eine Kommazahl bieten."));
					Bukkit.getScheduler().runTaskLater(ShopCore.getInstance(), () -> provider.reOpen(player, contents), 1);
					UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
				}
				
				return null;
			});
			
		});
	}
	
	public Optional<AuctionEntry> getServerLinkedEntry(){
		return Optional.ofNullable(this.serverHouse.getAuctionEntry(auctionID));
	}
}
