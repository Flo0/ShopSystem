package com.avarioncraft.shopsystem.auctionhouse.json;

import java.lang.reflect.Type;

import com.avarioncraft.shopsystem.auctionhouse.AuctionAccount;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AuctionAccountJson implements JsonSerializer<AuctionAccount>, JsonDeserializer<AuctionAccount> {

	@Override
	public AuctionAccount deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
//		JsonObject object = element.getAsJsonObject();
//		UUID uuid = UUID.fromString(object.get("owner").getAsString());
//		AuctionAccount account = new AuctionAccount(uuid);
//		
//		
//		
//		return account;
		return null;
	}

	@Override
	public JsonElement serialize(AuctionAccount account, Type type, JsonSerializationContext context) {
		JsonObject object = new JsonObject();

		return object;
	}

}
