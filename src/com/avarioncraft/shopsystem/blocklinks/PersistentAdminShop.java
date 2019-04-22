package com.avarioncraft.shopsystem.blocklinks;

import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.adminshops.AdminShopTransactionGUI;

import lombok.Setter;
import net.crytec.api.persistentblocks.PersistentBlock;
import net.crytec.api.persistentblocks.blocks.InteractableBlock;

public class PersistentAdminShop extends PersistentBlock implements InteractableBlock{
	
	@Setter
	private UUID shopID;
	
	@Override
	public void onInteract(PlayerInteractEvent event) {
		AdminShopTransactionGUI.open(event.getPlayer(), ShopCore.getInstance().getAdminShopManager().getShop(shopID));
	}

	@Override
	protected void loadData(ConfigurationSection config) {
		this.shopID = UUID.fromString(config.getString("ShopID"));
		
	}

	@Override
	protected void onBreak(BlockBreakEvent event) {
		
	}

	@Override
	protected void onRemove() {
		
	}

	@Override
	protected void postInit() {
		
	}

	@Override
	protected void saveData(ConfigurationSection config) {
		config.set("ShopID", this.shopID.toString());
	}

}
