package com.avarioncraft.shopsystem.tradesystem.json;

import java.lang.reflect.Type;

import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import lombok.SneakyThrows;
import net.minecraft.server.v1_13_R2.MojangsonParser;
import net.minecraft.server.v1_13_R2.NBTTagCompound;

public class ItemWareDeserializer implements JsonDeserializer<ItemWare> {

	@Override
	@SneakyThrows
	public ItemWare deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject obj = element.getAsJsonObject();
		NBTTagCompound comp = MojangsonParser.parse(obj.get("value").getAsString());
		net.minecraft.server.v1_13_R2.ItemStack cis = net.minecraft.server.v1_13_R2.ItemStack.a(comp);
		return new ItemWare(CraftItemStack.asBukkitCopy(cis));
	}
}