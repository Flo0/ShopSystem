package com.avarioncraft.shopsystem.auctionhouse;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServerLoadEvent.LoadType;

import com.avarioncraft.shopsystem.ShopCore;
import com.google.common.collect.Maps;

public class AuctionAccountManager implements Listener {
	
	public AuctionAccountManager(ShopCore plugin) {
		this.accounts = Maps.newHashMap();
		this.accountFolder = new File(plugin.getDataFolder(), "AuctionAccounts");
		if(!this.accountFolder.exists()) this.accountFolder.mkdirs();
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	private final File accountFolder;
	private final Map<UUID, AuctionAccount> accounts;
	
	public Optional<AuctionAccount> getAccountOf(UUID playerID) {
		return Optional.ofNullable(this.accounts.get(playerID));
	}
	
	public boolean hasOnlineAccount(UUID playerID) {
		if(!this.accounts.containsKey(playerID)) return false;
		return this.accounts.get(playerID).isPlayerOnline();
	}
	
	public void editAccount(UUID playerID, Consumer<AuctionAccount> editConsumer) {
		AuctionAccount account;
		boolean isTemp = false;
		
		if(!this.hasOnlineAccount(playerID)) {
			account = this.loadFromDisk(playerID);
			isTemp = true;
		}else {
			account = this.accounts.get(playerID);
		}
		
		editConsumer.accept(account);
		
		if(isTemp) {
			this.saveToDisk(playerID);
		}
		
	}
	
	public AuctionAccount loadFromDisk(UUID playerID) {
		File accountFile = new File(this.accountFolder, playerID.toString() + ".yml");
		AuctionAccount account = new AuctionAccount(playerID);
		if(!accountFile.exists()) return account;
		FileConfiguration accountConfig = YamlConfiguration.loadConfiguration(accountFile);
		account.load(accountConfig);
		return account;
	}
	
	private void saveToDisk(UUID playerID) {
		File accountFile = new File(this.accountFolder, playerID.toString() + ".yml");
		FileConfiguration accountConfig = YamlConfiguration.loadConfiguration(accountFile);
		if(this.hasOnlineAccount(playerID)) {
			this.accounts.get(playerID).save(accountConfig);
			try {
				accountConfig.save(accountFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void saveOnlineAccounts() {
		this.accounts.keySet().forEach(id -> this.saveToDisk(id));
	}
	
	public void unloadAccount(UUID accountID) {
		this.saveToDisk(accountID);
		this.accounts.remove(accountID);
	}
	
	@EventHandler
	public void onServerReload(ServerLoadEvent event) {
		if (event.getType() != LoadType.RELOAD) return;
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			UUID playerID = player.getUniqueId();
			AuctionAccount account;

			if (this.accounts.containsKey(playerID)) {
				account = this.accounts.get(playerID);
			} else {
				account = this.loadFromDisk(playerID);
				this.accounts.put(playerID, account);
			}

			account.setPlayerOnline(true);
		}

	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		
		UUID playerID = event.getPlayer().getUniqueId();
		AuctionAccount account;
		
		if(this.accounts.containsKey(playerID)) {
			account = this.accounts.get(playerID);
		}else {
			account = this.loadFromDisk(playerID);
			this.accounts.put(playerID, account);
		}
		
		account.setPlayerOnline(true);
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		UUID playerID = event.getPlayer().getUniqueId();
		this.saveToDisk(playerID);
		AuctionAccount account = this.accounts.get(playerID);
		if(!account.hasReference()) {
			this.accounts.remove(playerID);
		}
	}
}
