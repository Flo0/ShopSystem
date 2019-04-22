package com.avarioncraft.shopsystem.manager;

import java.util.Set;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.commands.StatisticCommands.ToplistTimespan;
import com.avarioncraft.shopsystem.sql.DatabaseManager;
import com.avarioncraft.shopsystem.statistics.TransactionOccasion;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.collect.Sets;

public class StatisticManager implements Runnable {
	
	public StatisticManager(ShopCore plugin) {
		this.plugin = plugin;
		this.dataManager = plugin.getDatabaseManager();
		this.holograms = Sets.newHashSet();
		
		
		Bukkit.getScheduler().runTaskTimer(plugin, this, 0, plugin.getConfig().getInt("TopListUpdate") * 1200);
	}
	
	private final ShopCore plugin;
	private final DatabaseManager dataManager;
	private final Set<ToplistHologram> holograms;
	
	public void createHologram(Location location, ToplistTimespan timespan, int limit) {
		this.holograms.add(new ToplistHologram(location, timespan, limit));
	}
	
	private class ToplistHologram {
		
		public ToplistHologram(Location location, ToplistTimespan timespan, int limit) {
			this.limit = limit;
			this.timespan = timespan;
			this.location = location;
			this.hologram = HologramsAPI.createHologram(plugin, location.clone().add(0, limit, 0));
			
			this.update();
		}
		
		private final Location location;
		private final int limit;
		private final ToplistTimespan timespan;
		private Hologram hologram;
		
		public void update() {
			
			if(this.hologram != null) this.hologram.delete();
			this.hologram = HologramsAPI.createHologram(plugin, location.clone().add(0, limit, 0));
			
			hologram.appendTextLine("§e<--- §fSpielershop Topliste:§6 " + timespan.getDisplayName() +"§e --->");
			
			ShopCore.newChain().asyncFirst(() -> dataManager.getToplist(limit, TransactionOccasion.PLAYER_SHOP_BUY, timespan))
			.syncLast(list -> IntStream.range(0, list.size()).forEachOrdered(i ->{
				
				String entry = list.get(i);
				this.hologram.appendTextLine("§6" + (i + 1) + ". §f" + entry);
				
			}));
			
			
		}
	}


	@Override
	public void run() {
		
		this.holograms.forEach(holo -> holo.update());
		
	}
	
}