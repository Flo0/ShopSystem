package com.avarioncraft.shopsystem.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.blocklinks.PersistentPlayerShop;
import com.avarioncraft.shopsystem.playershops.PlayerShop;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.tradesystem.json.InventoryDeserializer;
import com.avarioncraft.shopsystem.tradesystem.json.InventorySerializer;
import com.avarioncraft.shopsystem.tradesystem.json.ItemWareDeserializer;
import com.avarioncraft.shopsystem.tradesystem.json.ItemWareSerializer;
import com.avarioncraft.shopsystem.utils.Eco;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Queues;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.Getter;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;
import net.crytec.api.persistentblocks.PersistentBlockManager;
import net.crytec.api.util.F;
import net.crytec.api.util.UtilPlayer;

public class PlayerShopManager {
	
	public PlayerShopManager(ShopCore core) {
		this.plugin = core;
		this.playerShops = HashBasedTable.<UUID, UUID, PlayerShop>create();
		this.perms = LuckPerms.getApi();
		this.shopFolder = new File(this.plugin.getDataFolder(), "playershops");
		this.invManager = core.getPlayershopInventoryManager();
		this.perManager = core.getPerBlockManager();
		if (!shopFolder.exists()) {
			shopFolder.mkdir();
		}
		
		gson = new GsonBuilder()
				.registerTypeAdapter(ItemWare.class, new ItemWareSerializer())
				.registerTypeAdapter(ItemWare.class, new ItemWareDeserializer())
				.registerTypeAdapter(Inventory.class, new InventorySerializer())
				.registerTypeAdapter(Inventory.class, new InventoryDeserializer())
				.setPrettyPrinting()
				.disableInnerClassSerialization()
				.create();
		
		this.creationPrice = plugin.getConfig().getDouble("playershops.creationPrice", 0);
		
		this.stampMins = core.getConfig().getInt("PlayerShopMapGraphTime");
		
		Bukkit.getScheduler().runTaskTimer(core, new RentThread(), 0, 72000L);
		Bukkit.getScheduler().runTaskTimer(core, new GraphStampThread(), 0, 1200L * this.stampMins);
	}
	
	private final double creationPrice;
	
	private final PersistentBlockManager perManager;
	
	private final int stampMins;
	
	private final File shopFolder;
	private final ShopCore plugin;
	private final Gson gson;
	//@Getter
	//private final Map<UUID, HashMap<UUID, PlayerShop>> playerShops;
	@Getter
	private Table<UUID, UUID, PlayerShop> playerShops;
	private final LuckPermsApi perms;
	private final PlayershopInventoryManager invManager;
	
	public static final String maxShopsMetaNode = "maximumshops";
	
	public Optional<PlayerShop> createPlayerShop(UUID owner, String displayName, Location location) {
		Player player = Bukkit.getPlayer(owner);
		User user = this.perms.getUser(owner);
		Optional<Contexts> contexts = perms.getContextForUser(user);
		
		if(!location.getBlock().getState().getType().equals(Material.AIR)) {
			player.sendMessage(F.error("An dieser Stelle kann kein Shop erstellt werden."));
			return Optional.empty();
		}
		
		int maxShops = 0;
		
		if (contexts.isPresent()) {
			MetaData meta = user.getCachedData().getMetaData(contexts.get());
			maxShops = Integer.valueOf(meta.getMeta().getOrDefault(maxShopsMetaNode, "0"));
		}
		
		if (this.getShopsOfPlayer(owner).size() >= maxShops && !player.isOp()) {
			player.sendMessage(F.error("Du hast bereits die maximale Anzahl an Shops erreicht."));
			return Optional.empty();
		}
		
		if (this.creationPrice > 0 && !Eco.has(player, this.creationPrice)) {
			player.sendMessage(F.error("Du hast nicht genug " + Eco.getEconomy().currencyNamePlural() + " um einen Shop zu erstellen."));
			return Optional.empty();
		}
		
		PlayerShop shop = new PlayerShop();
		shop.setDisplayName(displayName);
		shop.setOwnerID(owner);
		shop.setShopID(UUID.randomUUID());
		shop.setPhysicalInventory(Bukkit.createInventory(null, 54, displayName));
		
		BlockData data = Material.CHEST.createBlockData();
		((Directional)data).setFacing(player.getFacing().getOppositeFace());
		PersistentPlayerShop pshop = (PersistentPlayerShop) perManager.createBlock("PlayerShop", location, data);
		
		pshop.setShop(shop);
		pshop.setupLines();
		
		shop.setPhysicalShop(pshop);
		this.invManager.register(shop);
		this.playerShops.put(shop.getOwnerID(), shop.getShopID(), shop);
		return Optional.of(shop);
	}
	
	public void deleteShop(UUID owner, UUID shopID, boolean deletePersistent) {
		PlayerShop shop = this.playerShops.get(owner, shopID);
		if(deletePersistent) shop.getPhysicalShop().delete();
		ShopCore.getInstance().getPlayershopInventoryManager().unregister(shop);
		this.playerShops.remove(owner, shopID);
	}
	
	public Collection<PlayerShop> getShopsOfPlayer(UUID owner) {
		if (this.playerShops.containsRow(owner)) {
			return this.playerShops.row(owner).values();
		} else {
			return Collections.emptySet();
		}
	}
	
	public void saveAllShops() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		Set<UUID> rowKey = playerShops.rowKeySet();
		if(this.shopFolder.listFiles().length != 0) {
			for(File playerFile : this.shopFolder.listFiles()) {
				
				if(!rowKey.contains(UUID.fromString(playerFile.getName().substring(0, 36)))) {
					
					playerFile.delete();
					
				}
				
			}
		}
		
		for (UUID player : rowKey) {
			JsonArray shops = new JsonArray();
			File file = new File(this.shopFolder, player.toString() + ".json");
			try {
				if (!file.exists()) {
					file.createNewFile();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			for (PlayerShop shop : this.getShopsOfPlayer(player)) {
				shops.add(this.saveShop(shop));
			}
			
			if (this.getShopsOfPlayer(player).isEmpty()) {
				file.delete();
				continue;
			}
			
			try (FileWriter writer = new FileWriter(file, false)) {
				writer.write(gson.toJson(shops));
				writer.flush();
				writer.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public void loadAllShops() {
		Iterator<File> shops = FileUtils.iterateFiles(this.shopFolder, new String[] { "json" } , false);
		Gson gson = new GsonBuilder().create();
		
		while (shops.hasNext()) {
			File current = shops.next();
			
			try (FileReader reader = new FileReader(current)) {
				JsonArray array = gson.fromJson(reader, JsonArray.class);
				for (JsonElement element : array) {
					PlayerShop shop = this.loadShop(element.toString());
					this.playerShops.put(shop.getOwnerID(), shop.getShopID(), shop);
				}
				reader.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private JsonObject saveShop(PlayerShop shop) {
		JsonObject json = new JsonObject();
		json.add("shop", gson.fromJson(gson.toJson(shop), JsonElement.class));
		json.add("openhours", gson.fromJson(gson.toJson(shop.getTimeRange().getHoursArray()), JsonElement.class));
		return json;
	}
	
	private PlayerShop loadShop(String json) {
		JsonElement element = gson.fromJson(json, JsonElement.class);
		JsonObject object = element.getAsJsonObject();
		PlayerShop shop = gson.fromJson(object.get("shop"), PlayerShop.class);
		
		ShopCore.getInstance().getPlayershopInventoryManager().register(shop);
		
		int[] hours = gson.fromJson(object.get("openhours"), int[].class);
		shop.getTimeRange().implementRanges(hours);
        return shop;
	}
	
	private final class RentThread implements Runnable {
		
		private RentThread(ArrayDeque<PlayerShop> queue) {
			this.queue = queue;
		}
		
		protected RentThread() {}
		
		private long stopTime;
		private ArrayDeque<PlayerShop> queue = Queues.newArrayDeque();
		
		@Override
		public void run() {
			this.stopTime = System.currentTimeMillis() + 8;
			if(queue.isEmpty()) queue.addAll(playerShops.values());
			while(System.currentTimeMillis() < stopTime && !queue.isEmpty()) {
				PlayerShop shop = queue.poll();
				if(!shop.payRent()){
					Player owner = Bukkit.getPlayer(shop.getOwnerID());
					if(owner != null) {
						UtilPlayer.playSound(owner, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 1.2F);
						owner.sendMessage(F.main("PlayerShop", "Dein Spielershop " + F.elem(shop.getDisplayName()) + " wurde wegen nicht bezahlen der Rente gelöscht."));
					}
					
					deleteShop(shop.getOwnerID(), shop.getShopID(), true);
					
				}
			}
			if(!queue.isEmpty()) Bukkit.getScheduler().runTaskLater(plugin, new RentThread(this.queue), 1L);
		}
		
	}
	
	private final class GraphStampThread implements Runnable {
		
		private GraphStampThread(ArrayDeque<PlayerShop> queue) {
			this.queue = queue;
		}
		
		protected GraphStampThread() {}
		
		private long stopTime;
		private ArrayDeque<PlayerShop> queue = Queues.newArrayDeque();
		
		@Override
		public void run() {
			this.stopTime = System.currentTimeMillis() + 8;
			if(queue.isEmpty()) queue.addAll(playerShops.values());
			while(System.currentTimeMillis() < stopTime && !queue.isEmpty()) {
				queue.poll().stamp(stampMins);
			}
			if(!queue.isEmpty()) Bukkit.getScheduler().runTaskLater(plugin, new GraphStampThread(this.queue), 1L);
		}
		
	}
	
}