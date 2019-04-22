package com.avarioncraft.shopsystem.tradesystem.json;

import java.lang.reflect.Type;

import com.avarioncraft.shopsystem.tradesystem.PermissionWare;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import lombok.SneakyThrows;

public class PermissionWareDeserializer implements JsonDeserializer<PermissionWare> {

	@Override
	@SneakyThrows
	public PermissionWare deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject obj = element.getAsJsonObject();
		String v = obj.get("value").getAsString();
		return new PermissionWare(v);
	}
}
