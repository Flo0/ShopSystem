package com.avarioncraft.shopsystem.tradesystem;

import java.util.UUID;

import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import net.crytec.api.util.F;

public class PermissionGood extends TradeGood<String>{

	public PermissionGood(String tradeID, double value, int amount, String good, UUID economyID) {
		super(tradeID, value, amount, good, economyID);
	}

	@Override
	public void deliver(Player player) {
		Node node = LuckPerms.getApi().buildNode(super.good).build();
		User user = LuckPerms.getApi().getUser(player.getUniqueId());
		DataMutateResult result = user.setPermission(node);
		
		if (result.wasFailure()) {
			player.sendMessage(F.error("Permission konnte nicht gesetzt werden!"));
		}
	}

	@Override
	public boolean has(Player player) {
		return player.hasPermission(this.good);
	}

	@Override
	public void take(Player player) {
		Node node = LuckPerms.getApi().buildNode(super.good).build();
		User user = LuckPerms.getApi().getUser(player.getUniqueId());
		DataMutateResult result = user.unsetPermission(node);
		if (result.wasFailure()) {
			player.sendMessage(F.error("Permission konnte nicht entfernt werden!"));
		}
	}

	@Override
	public JsonObject asJson() {
		JsonObject json = new JsonObject();
		json.addProperty("Type", "Permission");
		json.addProperty("Permission", super.good);
		return json;
	}
}