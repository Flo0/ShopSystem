package com.avarioncraft.shopsystem.tradesystem.json;

import java.lang.reflect.Type;

import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.crytec.api.util.UtilInv;

public class ItemWareSerializer implements JsonSerializer<ItemWare> {
	
	@Override
	public JsonElement serialize(ItemWare ware, Type type, JsonSerializationContext context) {
		JsonObject object = new JsonObject();
		String item = UtilInv.serializeItemStack(ware.value);
		object.addProperty("value", item);
		object.addProperty("type", ware.getClass().getSimpleName());
		return object;
	}

}
