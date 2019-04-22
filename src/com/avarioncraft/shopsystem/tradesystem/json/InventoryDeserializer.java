package com.avarioncraft.shopsystem.tradesystem.json;

import java.lang.reflect.Type;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.crytec.api.util.UtilInv;

public class InventoryDeserializer implements JsonDeserializer<Inventory> {

	@Override
	public Inventory deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
		
		JsonObject obj = element.getAsJsonObject();
		int size = obj.get("size").getAsInt();
		Inventory inventory = Bukkit.createInventory(null, size);
		
		ItemStack[] content = new ItemStack[size];
		
		JsonArray array = obj.get("content").getAsJsonArray();
		
		for (int pos = 0; pos < size; pos++) {
			content[pos] = UtilInv.deserializeItemStack(array.get(pos).getAsString());
		}
		
		inventory.setContents(content);
		return inventory;
	}
}