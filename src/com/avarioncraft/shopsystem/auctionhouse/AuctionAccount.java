package com.avarioncraft.shopsystem.auctionhouse;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.utils.Eco;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.Setter;
import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.smartInv.ClickableItem;
import net.crytec.api.smartInv.SmartInventory;
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.smartInv.content.Pagination;
import net.crytec.api.smartInv.content.SlotIterator;
import net.crytec.api.smartInv.content.SlotIterator.Type;
import net.crytec.api.smartInv.content.SlotPos;
import net.crytec.api.util.UtilPlayer;

public class AuctionAccount {
	
	public AuctionAccount(UUID playerID) {
		this.playerID = playerID;
		this.itemVault = Lists.newArrayList();
		this.ownedAuctions = Sets.newHashSet();
		this.biddingAuctions = Sets.newHashSet();
	}
	
	@Getter
	private final UUID playerID;
	@Getter
	private final Set<UUID> ownedAuctions;
	@Getter
	private final Set<UUID> biddingAuctions;
	private final ArrayList<ItemStack> itemVault;
	@Getter @Setter
	private boolean playerOnline = false;
	
	public void openContent(Player player) {
		
		SmartInventory.builder().size(5).title("Auktionslager").provider(new InventoryProvider() {

			@Override
			public void init(Player player, InventoryContents contents) {
				
				Pagination pagination = contents.pagination();
				
				pagination.setItems(itemVault.stream().map(item -> ClickableItem.of(item, event ->{
					
					itemVault.remove(item);
					UtilPlayer.giveItems(player, item, 1, true);
					this.reOpen(player, contents);
					
				})).toArray(ClickableItem[]::new));
				
				pagination.setItemsPerPage(36);
				
				SlotIterator slotIterator = contents.newIterator(Type.HORIZONTAL, 0, 0);
				slotIterator = slotIterator.allowOverride(false);
				pagination.addToIterator(slotIterator);
				
				contents.set(SlotPos.of(4, 4), ClickableItem.of(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("ChestHead"))
						.name("§eZu den Auktionen")
						.build()
						, event ->{
					ShopCore.getInstance().getAuctionHouse().openAuctionFor(player);
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
				}));
			}
			
		}).build().open(player);
		
	}
	
	public boolean hasReference() {
		return !(this.ownedAuctions.isEmpty() && this.biddingAuctions.isEmpty() && !this.playerOnline);
	}
	
	public void withdraw(double amount) {
		Eco.withdraw(Bukkit.getOfflinePlayer(this.playerID), amount);
	}
	
	public boolean has(double amount) {
		return Eco.has(Bukkit.getOfflinePlayer(this.playerID), amount);
	}
	
	public void give(double amount) {
		Eco.give(Bukkit.getOfflinePlayer(this.playerID), amount);
	}
	
	public void storeItem(ItemStack item) {
		this.itemVault.add(item);
	}
	
	public void load(FileConfiguration config) {
		if(config.isSet("ItemVault")) {
			ConfigurationSection vaultSection = config.getConfigurationSection("ItemVault");
			for(String key : vaultSection.getKeys(false)) {
				this.itemVault.add(vaultSection.getItemStack(key));
			}
		}
		
		if(config.isSet("BiddingAuctions")) {
			ConfigurationSection biddingSection = config.getConfigurationSection("BiddingAuctions");
			for(String key : biddingSection.getKeys(false)) {
				this.biddingAuctions.add(UUID.fromString(biddingSection.getString(key)));
			}
		}
		
		if(config.isSet("OwnedAuctions")) {
			ConfigurationSection ownedSection = config.getConfigurationSection("OwnedAuctions");
			for(String key : ownedSection.getKeys(false)) {
				this.biddingAuctions.add(UUID.fromString(ownedSection.getString(key)));
			}
		}
	}
	
	public void save(FileConfiguration config) {
		ConfigurationSection vaultSection = config.createSection("ItemVault");
		ConfigurationSection biddingAuctions = config.createSection("BiddingAuctions");
		ConfigurationSection ownedAuctions = config.createSection("OwnedAuctions");
		
		for(int index = 0; index < this.itemVault.size(); index++) {
			vaultSection.set("" + index, itemVault.get(index));
		}
		
		int biddingIndex = 0;
		for(UUID biddingID : this.biddingAuctions) {
			biddingAuctions.set("" + biddingIndex++, biddingID.toString());
		}
		
		int ownerIndex = 0;
		for(UUID owningID : this.biddingAuctions) {
			ownedAuctions.set("" + ownerIndex++, owningID.toString());
		}
		
	}
}
