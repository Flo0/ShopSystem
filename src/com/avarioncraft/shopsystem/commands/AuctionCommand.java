package com.avarioncraft.shopsystem.commands;

import java.util.stream.IntStream;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import com.avarioncraft.shopsystem.ShopCore;

import net.crytec.acf.BaseCommand;
import net.crytec.acf.annotation.CommandAlias;
import net.crytec.acf.annotation.CommandCompletion;
import net.crytec.acf.annotation.CommandPermission;
import net.crytec.acf.annotation.Default;
import net.crytec.acf.annotation.Subcommand;
import net.crytec.acf.annotation.Syntax;
import net.crytec.api.util.F;
import net.crytec.api.util.UUIDFetcher;
import net.crytec.api.util.language.LanguageHelper;

@CommandAlias("auktion")
public class AuctionCommand extends BaseCommand {
	
	public AuctionCommand(ShopCore core) {
		this.plugin = core;
	}
	
	private final ShopCore plugin;
	
	@Default
	public void openAuction(Player player) {
		plugin.getAuctionHouse().openAuctionFor(player);
	}
	
	
	@Subcommand("log")
	@CommandPermission("shop.auction.command.log")
	@CommandCompletion("@players @nothing")
	@Syntax("<Spieler> <Anzahl>")
	public void auctionLog(Player sender, String forWhom, int amount) {
		sender.sendMessage("§7<--------- §eGetting data from SQL §7--------->");
		ShopCore.newChain().asyncFirst(() -> plugin.getDatabaseManager().getFinishedAuctions(UUIDFetcher.getUUID(forWhom), amount))
		.syncLast(list -> {
			IntStream.range(0, list.size()).mapToObj(index -> list.get(index)).forEach(entry -> sender.sendMessage(entry));
			sender.sendMessage("§7<----------- §eDone (" + list.size() + " Einträge) §7----------->");
		}).execute();
		
	}
	
	@Subcommand("link")
	@CommandPermission("shop.auction.command.link")
	public void linkAuction(Player player) {
		
		RayTraceResult ray = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 8D, 2D, e -> !e.getUniqueId().equals(player.getUniqueId()) );
		if(ray == null) {
			player.sendMessage(F.error("Kein entity gefunden"));
			return;
		}
		
		Entity entity = ray.getHitEntity();
		
		if(entity == null) {
			player.sendMessage(F.error("Kein entity gefunden"));
			return;
		}
		
		if (entity instanceof Player) {
			Player p = (Player) entity;
			if (p.isOnline()) {
				player.sendMessage(F.error("Spieler können nicht verlinkt werden."));
				return;
			}
		}
		
		player.sendMessage(F.main("Auktionen", "Entity " + LanguageHelper.getEntityDisplayName(entity) + "§7 wurde verlinkt."));
		entity.getScoreboardTags().add("Auctioneer");
		
	}
}
