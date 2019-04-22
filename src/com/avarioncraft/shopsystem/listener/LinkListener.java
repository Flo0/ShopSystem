package com.avarioncraft.shopsystem.listener;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.adminshops.AdminShopTransactionGUI;

public class LinkListener implements Listener{
	
	@EventHandler
	public void onEntityInteract(PlayerInteractAtEntityEvent event) {
		
		if(!event.getHand().equals(EquipmentSlot.HAND)) return;
		
		Entity entity = event.getRightClicked();
		
		if(entity.getScoreboardTags().contains("Auctioneer")) {
			ShopCore.getInstance().getAuctionHouse().openAuctionFor(event.getPlayer());
			event.setCancelled(true);
		}
		
		Optional<String> adminTag = entity.getScoreboardTags().stream().filter(tag -> tag.contains("AdminShop_")).findAny();
		
		if(adminTag.isPresent()) {
			AdminShopTransactionGUI.open(event.getPlayer(), ShopCore.getInstance().getAdminShopManager().getShop(UUID.fromString(adminTag.get().split("_")[1])));
			event.setCancelled(true);
		}
		
	}
	
	@EventHandler
	public void villagerOpen(InventoryOpenEvent event) {
		Inventory inv = event.getInventory();
		if(!inv.getType().equals(InventoryType.MERCHANT)) return;
		if(inv.getHolder() instanceof Villager) {
			Villager entity = (Villager) inv.getHolder();
			
			if(entity.getScoreboardTags().contains("Auctioneer")) {
				ShopCore.getInstance().getAuctionHouse().openAuctionFor((Player)event.getView().getPlayer());
				event.setCancelled(true);
			}
			
			Optional<String> adminTag = entity.getScoreboardTags().stream().filter(tag -> tag.contains("AdminShop_")).findAny();
			
			if(adminTag.isPresent()) {
				AdminShopTransactionGUI.open((Player)event.getView().getPlayer(), ShopCore.getInstance().getAdminShopManager().getShop(UUID.fromString(adminTag.get().split("_")[1])));
				event.setCancelled(true);
			}
		}
	}
	
}
