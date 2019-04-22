package com.avarioncraft.shopsystem.utils.itemchooser;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Consumer;

public class ItemChooserGui {
	
	protected ItemChooserGui(Consumer<ItemStack> consumer, Inventory inv, boolean asOne) {
		this.itemConsumer = consumer;
		this.inv = inv;
		this.asOne = asOne;
	}
	
	protected final Consumer<ItemStack> itemConsumer;
	protected final Inventory inv;
	protected final boolean asOne;
	
	protected void open(Player player){
		player.openInventory(inv);
	}
	
}
