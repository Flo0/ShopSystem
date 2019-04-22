package com.avarioncraft.shopsystem.tradesystem.json;

import java.lang.reflect.Type;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.crytec.api.util.UtilInv;

public class InventorySerializer implements JsonSerializer<Inventory> {

	@Override
	public JsonElement serialize(Inventory inventory, Type type, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("inventorytype", inventory.getType().toString());
		obj.addProperty("size", inventory.getSize());
		JsonArray items = new JsonArray();
		for (ItemStack item : inventory.getContents()) {
			items.add(UtilInv.serializeItemStack(item));
		}
		obj.add("content", items);
		return obj;
	}
}