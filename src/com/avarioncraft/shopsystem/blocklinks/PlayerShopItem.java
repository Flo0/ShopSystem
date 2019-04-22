package com.avarioncraft.shopsystem.blocklinks;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.playershops.PlayerShop;
import com.avarioncraft.shopsystem.utils.UtilPlayer;

import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.itemstack.customitems.CustomPlaceableItem;
import net.crytec.api.util.F;
import net.crytec.api.util.anvilgui.AnvilGUI;

public class PlayerShopItem extends CustomPlaceableItem {

	public PlayerShopItem() {
		super(new NamespacedKey(ShopCore.getInstance(), "playershop-block"));
	}

	@Override
	public void onPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;
		
		event.setBuild(false);
		event.setCancelled(true);
		
		new AnvilGUI(event.getPlayer(), "shopname" , (p, input) -> {
			Optional<PlayerShop> shop = ShopCore.getInstance().getPlayerShopManager().createPlayerShop(p.getUniqueId(), input, event.getBlockPlaced().getLocation());
			
			if (!shop.isPresent()) {
				return null;
			}
			
			shop.get().openForEdit(p);
			UtilPlayer.takeItems(event.getPlayer().getInventory(), getItemStack(), 1);
			p.sendMessage(F.main("Spielershops", "Shop wurde erstellt."));
			return null;
		});
	}

	@Override
	protected ItemStack initItem() {
		return new ItemBuilder(Material.CHEST).name("§7Spielershop").build();
	}

}
