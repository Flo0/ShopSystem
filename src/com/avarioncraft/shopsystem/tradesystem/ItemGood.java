package com.avarioncraft.shopsystem.tradesystem;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.utils.UtilPlayer;
import com.google.gson.JsonObject;

import net.crytec.api.util.UtilInv;

public class ItemGood extends TradeGood<ItemStack>{

	public ItemGood(String tradeID, double value, int amount, ItemStack good, UUID economyID) {
		super(tradeID, value, amount, good, economyID);
	}

	@Override
	public void deliver(Player player) {
		
		UtilPlayer.giveItems(player.getInventory(), this.good, amount, true, player.getLocation());
		
	}

	@Override
	public boolean has(Player player) {
		return UtilPlayer.hasItems(player.getInventory(), this.good, this.amount);
	}

	@Override
	public void take(Player player) {
		UtilPlayer.takeItems(player.getInventory(), this.good, this.amount);
	}

	@Override
	public JsonObject asJson() {
		JsonObject json = new JsonObject();
		json.addProperty("Type", "Item");
		json.addProperty("Item", UtilInv.serializeItemStack(super.good));
		return json;
	}

}
