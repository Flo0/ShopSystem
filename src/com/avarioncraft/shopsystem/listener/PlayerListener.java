package com.avarioncraft.shopsystem.listener;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.manager.DiscountManager;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;

public class PlayerListener implements Listener {
	
	private final ShopCore plugin;
	private final LuckPermsApi perms;
	
	public PlayerListener(ShopCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
		this.perms = LuckPerms.getApi();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		User user = this.perms.getUser(event.getPlayer().getUniqueId());
		Optional<Contexts> contexts = perms.getContextForUser(user);
		
		double discount = 0;
		
		if (contexts.isPresent()) {
			MetaData meta = user.getCachedData().getMetaData(contexts.get());
			discount = Double.valueOf(meta.getMeta().getOrDefault(DiscountManager.DISCOUNT_FLAG, "0"));
		}
		event.getPlayer().setMetadata(DiscountManager.DISCOUNT_FLAG, new FixedMetadataValue(plugin, discount));
	}
}