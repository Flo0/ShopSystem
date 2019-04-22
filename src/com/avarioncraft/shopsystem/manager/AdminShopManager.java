package com.avarioncraft.shopsystem.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.adminshops.AdminShop;
import com.avarioncraft.shopsystem.statistics.TransactionEvent;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.tradesystem.PermissionWare;
import com.avarioncraft.shopsystem.tradesystem.json.ItemWareDeserializer;
import com.avarioncraft.shopsystem.tradesystem.json.ItemWareSerializer;
import com.avarioncraft.shopsystem.tradesystem.json.PermissionWareDeserializer;
import com.avarioncraft.shopsystem.tradesystem.json.PermissionWareSerializer;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;

public class AdminShopManager implements Listener{
	
	public AdminShopManager(ShopCore plugin) {
		this.plugin = plugin;
		this.adminShops = Maps.newHashMap();
		this.categorys = Maps.newHashMap();
		types.put(ItemWare.class.getSimpleName(), new TypeToken<ItemWare>() { }.getType());
		types.put(PermissionWare.class.getSimpleName(), new TypeToken<PermissionWare>() { }.getType());
		
		gson = new GsonBuilder()
				.registerTypeAdapter(ItemWare.class, new ItemWareSerializer())
				.registerTypeAdapter(ItemWare.class, new ItemWareDeserializer())
				.registerTypeAdapter(PermissionWare.class, new PermissionWareSerializer())
				.registerTypeAdapter(PermissionWare.class, new PermissionWareDeserializer())
				.setPrettyPrinting()
				.disableInnerClassSerialization()
				.create();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
		Bukkit.getScheduler().runTaskTimer(plugin, new AdminShopTickThread(), 20, 20);
	}
	
	private final ShopCore plugin;
	private final Map<UUID, AdminShop> adminShops;
	@Getter
	private final Map<String, Set<AdminShop>> categorys;
	
	private final HashMap<String, Type> types = Maps.newHashMap();
	
	private final Gson gson;
	
	@EventHandler
	public void adminShopTransaction(TransactionEvent event) {
		if(!event.getOccasion().toString().contains("ADMIN")) return;
		this.onAdminShopTransaction(event);
	}
	
	private void onAdminShopTransaction(TransactionEvent event) {
		event.setTransactionMoney(event.getTransactionMoney() * DiscountManager.calculateDiscountMultiplier(Bukkit.getPlayer(event.getBuyerID())));
	}
	
	public AdminShop getShop(UUID shopID) {
		return this.adminShops.get(shopID);
	}
	
	public Collection<AdminShop> getShops(){
		return this.adminShops.values();
	}
	
	public AdminShop createShop() {
		AdminShop shop = new AdminShop();
		shop.setShopID(UUID.randomUUID());
		this.adminShops.put(shop.getShopID(), shop);
		this.categorys.putIfAbsent(shop.getCategory(), Sets.newHashSet());
		this.categorys.get(shop.getCategory()).add(shop);
		return shop;
	}
	
	private AdminShop loadShop(String json) {
		JsonElement element = gson.fromJson(json, JsonElement.class);
		JsonObject object = element.getAsJsonObject();
		
		AdminShop shop = gson.fromJson(object.get("shop"), AdminShop.class);
		
		int[] hours = gson.fromJson(object.get("openhours"), int[].class);
		shop.getTimeRange().implementRanges(hours);
		
		if (object.get("ware") != null) {
			JsonObject ware = object.getAsJsonObject("ware");
			Type type = types.get(ware.get("type").getAsString());
			shop.setTradeWare(gson.fromJson(ware, type));
		}
        return shop;
	}
	
	private JsonElement saveShop(AdminShop shop) {
		JsonElement element = gson.fromJson(gson.toJson(shop), JsonElement.class);
		
		JsonObject json = new JsonObject();
		json.add("shop", element);
		
		
		json.add("openhours", gson.fromJson(gson.toJson(shop.getTimeRange().getHoursArray()), JsonElement.class));
		
		if (shop.getTradeWare() != null) {			
			json.add("ware", gson.fromJson(gson.toJson(shop.getTradeWare(), shop.getTradeWare().getType()), JsonElement.class));
		}
		return json;
	}
	
	
	public void loadFromFile() {
		File file = new File(plugin.getDataFolder(), "adminshops.json");
		if (!file.exists()) return;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try (FileReader reader = new FileReader(file)) {
			
			JsonArray array = gson.fromJson(reader, JsonArray.class);
			for (JsonElement element : array) {
				AdminShop shop = this.loadShop(element.toString());
				this.adminShops.put(shop.getShopID(), shop);
				this.categorys.putIfAbsent(shop.getCategory(), Sets.newHashSet());
				this.categorys.get(shop.getCategory()).add(shop);
			}
			
			reader.close();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	public void saveToFile() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonArray shops = new JsonArray();
		
		for (AdminShop shop : this.adminShops.values()) {
			shops.add(this.saveShop(shop));
		}

		File file = new File(plugin.getDataFolder(), "adminshops.json");
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		try (FileWriter writer = new FileWriter(file, false)) {

			writer.write(gson.toJson(shops));
			writer.flush();
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	public void remove(AdminShop shop) {
		this.adminShops.remove(shop.getShopID());
		this.categorys.get(shop.getCategory()).remove(shop);
		if(this.categorys.get(shop.getCategory()).isEmpty()) {
			this.categorys.remove(shop.getCategory());
		}
	}
	
	private final class AdminShopTickThread implements Runnable{

		@Override
		public void run() {
			
			getShops().forEach(shop -> {
				if(shop.shouldRefill()) shop.refill();
			});
			
		}
		
	}
	
}
