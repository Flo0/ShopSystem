package com.avarioncraft.shopsystem.manager;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.playershops.PlayerShop;
import com.google.common.collect.Maps;

import net.crytec.api.util.F;
import net.crytec.api.util.language.LanguageHelper;

public class PlayershopInventoryManager implements Listener{
	
	public PlayershopInventoryManager(ShopCore plugin) {
		this.shopInventorys = Maps.newHashMap();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	private final Map<Inventory, PlayerShop> shopInventorys;
	
	public void register(PlayerShop shop) {
		this.shopInventorys.put(shop.getPhysicalInventory(), shop);
	}
	
	public void unregister(PlayerShop shop) {
		if(shop == null) return;
		this.shopInventorys.remove(shop.getPhysicalInventory());
	}
	
	@EventHandler
	public void onShopInvClick(InventoryCloseEvent event) {
		Inventory inv = event.getInventory();
		if(!this.shopInventorys.containsKey(inv)) return;
		PlayerShop shop = this.shopInventorys.get(inv);
		ItemStack validItem = shop.getTradeWare().value;
		
		for(int slot = 0; slot < inv.getSize(); slot++) {
			ItemStack item = inv.getItem(slot);
			if(item != null && !item.asOne().isSimilar(validItem)) {
				inv.setItem(slot, null);
				shop.getPhysicalShop().getLocation().getWorld().dropItemNaturally(shop.getPhysicalShop().getLocation().clone().add(0.5, 1.5, 0.5), item);
				event.getPlayer().sendMessage(F.error("Dieses Item ist nicht in diesem Shop verkaufbar: " + LanguageHelper.getItemName(item)));
			}
		}
		
	}
}
