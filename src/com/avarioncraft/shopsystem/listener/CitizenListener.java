package com.avarioncraft.shopsystem.listener;

import java.util.Optional;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;

public class CitizenListener implements Listener {

	@EventHandler
	public void onSpawn(NPCSpawnEvent event) {
		if (event.getNPC().data().has("auktion")) {
			event.getNPC().getEntity().addScoreboardTag("Auctioneer");
		}else if(event.getNPC().data().has("adminshop")) {
			event.getNPC().getEntity().addScoreboardTag("AdminShop_" + event.getNPC().data().get("adminshop"));
		}
	}
	
	@EventHandler
	public void onDespawn(NPCDespawnEvent event) {
		if (event.getNPC().getEntity().getScoreboardTags().contains("Auctioneer")) {
			event.getNPC().data().setPersistent("auktion", true);
		}else {
			Optional<String> adminID = event.getNPC().getEntity().getScoreboardTags().stream().filter(tag -> tag.startsWith("AdminShop_")).findAny();
			if(adminID.isPresent()) {
				event.getNPC().data().setPersistent("adminshop", adminID.get().split("_")[1]);
			}
		}
	}
	
	@EventHandler
	public void onDisable(PluginDisableEvent event) {
		if (event.getPlugin().getName().equals("Citizens")) {
			CitizensAPI.getNPCRegistry().forEach(npc -> {
				if (npc.getEntity().getScoreboardTags().contains("Auctioneer")) {
					npc.data().setPersistent("auktion", true);
				}else {
					Optional<String> adminID = npc.getEntity().getScoreboardTags().stream().filter(tag -> tag.startsWith("AdminShop_")).findAny();
					if(adminID.isPresent()) {
						npc.data().setPersistent("adminshop", adminID.get().split("_")[1]);
					}
				}
			});
		}
		
		if (event.getPlugin().getName().equals("Shop")) {
			CitizensAPI.getNPCRegistry().forEach(npc -> {
				if (npc.getEntity().getScoreboardTags().contains("Auctioneer")) {
					npc.data().setPersistent("auktion", true);
				}else {
					Optional<String> adminID = npc.getEntity().getScoreboardTags().stream().filter(tag -> tag.startsWith("AdminShop_")).findAny();
					if(adminID.isPresent()) {
						npc.data().setPersistent("adminshop", adminID.get().split("_")[1]);
					}
				}
			});
		}
	}
	
}
