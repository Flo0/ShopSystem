package com.avarioncraft.shopsystem.manager;

import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.crytec.api.nbt.NBTItem;

public class ItemSafetyManager implements Listener, Predicate<ItemStack>{
	
	private final String safetyFlag = "__TRACKED__";
	
	public ItemSafetyManager(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public ItemStack tagItem(ItemStack item) {
		NBTItem nbt = new NBTItem(item);
		nbt.addCompound(this.safetyFlag);
		return nbt.getItem();
	}

	@Override
	public boolean test(ItemStack item) {
		if(item == null) return false;
		return new NBTItem(item).hasKey(this.safetyFlag);
	}
	
	public void clearInventory(Inventory inv) {
		IntStream.range(0, inv.getSize()).forEach(index ->{
			ItemStack item = inv.getItem(index);
			if(this.test(item)) {
				inv.setItem(index, null);
				item = null;
			}
		});
	}
	
	@EventHandler
	public void onItemClick(InventoryClickEvent event) {
		if(event.getInventory() == null) return;
		ItemStack current = event.getCurrentItem();
		if(current == null) return;
		if(!this.test(current)) return;
		event.setCancelled(true);
		current = null;
	}
	
	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if(!this.test(event.getItemDrop().getItemStack())) return;
		event.setCancelled(true);
		event.getItemDrop().remove();
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		event.getDrops().removeIf(this);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.clearInventory(event.getPlayer().getInventory());
	}
	
	@EventHandler
	public void onQuit(PlayerJoinEvent event) {
		this.clearInventory(event.getPlayer().getInventory());
	}
	
	@EventHandler
	public void onHandClick(PlayerInteractEvent event) {
		ItemStack item = event.getItem();
		if(!this.test(item)) return;
		event.setCancelled(true);
		item = null;
	}
	
	@EventHandler
	public void onEntityClick(PlayerInteractEntityEvent event) {
		ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
		if(!this.test(item)) return;
		event.setCancelled(true);
		event.getPlayer().getInventory().setItemInMainHand(null);
	}
	
}
