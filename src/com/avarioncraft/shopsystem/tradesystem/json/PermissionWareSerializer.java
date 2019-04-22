package com.avarioncraft.shopsystem.tradesystem.json;

import java.lang.reflect.Type;

import com.avarioncraft.shopsystem.tradesystem.PermissionWare;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PermissionWareSerializer implements JsonSerializer<PermissionWare> {
	
	@Override
	public JsonElement serialize(PermissionWare ware, Type type, JsonSerializationContext context) {
		JsonObject object = new JsonObject();
		object.addProperty("value", ware.value);
		object.addProperty("type", ware.getClass().getSimpleName());
		return object;
	}

}
