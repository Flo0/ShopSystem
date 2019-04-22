package com.avarioncraft.shopsystem.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class Eco {
	
	public static boolean setupEconomy(JavaPlugin host) {
		RegisteredServiceProvider<Economy> rsp = host.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}
	
	@Getter
	private static Economy economy = null;
	
	public static boolean has(OfflinePlayer player, double amount) {
		return economy.has(player, amount);
	}
	
	public static EconomyResponse withdraw(OfflinePlayer player, double amount) {
		return economy.withdrawPlayer(player, amount);
	}
	
	public static EconomyResponse give(OfflinePlayer player, double amount) {
		return economy.depositPlayer(player, amount);
	}
}
