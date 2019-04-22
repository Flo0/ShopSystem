package com.avarioncraft.shopsystem.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.sql.DatabaseManager;
import com.avarioncraft.shopsystem.statistics.TransactionEvent;

public class StatisticListener implements Listener{
	
	public StatisticListener(ShopCore plugin) {
		this.dataManager = plugin.getDatabaseManager();
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	private final DatabaseManager dataManager;
	
	@EventHandler
	public void defaultStat(TransactionEvent event) {
		this.dataManager.logShopTransaction(event);
	}
	
}