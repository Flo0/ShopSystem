package com.avarioncraft.shopsystem.utils.itemchooser;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Consumer;

import com.avarioncraft.shopsystem.ShopCore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.crytec.api.itemstack.ItemBuilder;

public class ItemChooserManager implements Listener{
	
	public ItemChooserManager(ShopCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		this.chooserInvs = Maps.newHashMap();
	}
	
	private final ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("").build();
	private final Map<Inventory, ItemChooserGui> chooserInvs;
	private final List<String> lore = Lists.newArrayList("", "§7Clicke mit einem item", "§7hier hin.");
	
	public void openChooser(Player player, Consumer<ItemStack> consumer, boolean asOne) {
		Inventory inv = Bukkit.createInventory(player, InventoryType.DISPENSER, "Item in Mitte legen");
		IntStream.rangeClosed(0, 8).filter(i -> i != 4).forEach(i -> inv.setItem(i, filler));
		inv.setItem(4, new ItemBuilder(Material.BARRIER).name("§cKein Item ausgewählt").build());
		ItemChooserGui chooser = new ItemChooserGui(consumer, inv, asOne);
		this.chooserInvs.put(inv, chooser);
		chooser.open(player);
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent event) {
		Inventory inv = event.getClickedInventory();
		if(!chooserInvs.containsKey(inv)) return;
		ItemStack cursor = event.getCursor();
		if(event.getSlot() == 4 && cursor != null && !cursor.getType().equals(Material.AIR)) {
			inv.setItem(4, (this.chooserInvs.get(inv).asOne ? cursor.clone().asOne() : cursor.clone()));
		}
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent event) {
		Inventory inv = event.getInventory();
		if(!chooserInvs.containsKey(inv)) return;
		Bukkit.getScheduler().runTaskLater(ShopCore.getInstance(), () ->{
			ItemStack item = inv.getItem(4);
			this.chooserInvs.get(inv).itemConsumer.accept((item.getLore() == this.lore ? null : item));
			this.chooserInvs.remove(inv);
		}, 1);
	}
}
