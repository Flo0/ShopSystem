package com.avarioncraft.shopsystem.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.adminshops.AdminShopPurchaseList;
import com.avarioncraft.shopsystem.sql.DatabaseManager;
import com.avarioncraft.shopsystem.statistics.TransactionOccasion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.crytec.acf.BaseCommand;
import net.crytec.acf.annotation.CommandAlias;
import net.crytec.acf.annotation.CommandCompletion;
import net.crytec.acf.annotation.CommandPermission;
import net.crytec.acf.annotation.Default;
import net.crytec.acf.annotation.Description;
import net.crytec.acf.annotation.Optional;
import net.crytec.acf.annotation.Single;
import net.crytec.acf.annotation.Subcommand;
import net.crytec.acf.annotation.Syntax;
import net.crytec.api.util.F;
import net.crytec.api.util.UUIDFetcher;
import net.crytec.taskchain.TaskChain;
import net.crytec.taskchain.TaskChainAbortAction;

@CommandAlias("metrics")
public class StatisticCommands extends BaseCommand {
	
	public StatisticCommands(ShopCore plugin) {
		this.dataManager = plugin.getDatabaseManager();
	}
	
	private final DatabaseManager dataManager;
	
	@Default
	@Description("Gibt dir die l")
	public void baseMetric(Player sender) {
		AdminShopPurchaseList.open(sender);
	}
	
	@Subcommand("masterlog")
	@CommandPermission("shop.metrics.masterlog")
	@Description("Erstellt ein chronologisch sortierten, vollständigen verlauf.")
	@CommandCompletion("@Players @nothing")
	@Syntax("[Spieler] [LogLänge]")
	public void masterLog(Player sender, @Optional @Single String player, @Default("" + -1) int amount) {
		
		sender.sendMessage("");
		UUID playerID = null;
		String name = "global";
		
		if(player != null) {
			name = player;
			playerID = UUIDFetcher.getUUID(player);
		}
		
		final UUID searchID = playerID;
		final int searchAmount = amount;
		final String searchName = name;
		
		Bukkit.getScheduler().runTaskAsynchronously(ShopCore.getInstance(), () ->{
			
			sender.sendMessage("Starte SQL-query");
			List<String> gloablLog = this.dataManager.getGlobalLog(searchID, searchAmount);
			sender.sendMessage("Fetched §e" + gloablLog.size() + "§f entrys.");
			File folder = new File(ShopCore.getInstance().getDataFolder() + File.separator + "GlobalLogs");
			if(!folder.exists()) folder.mkdirs();
			String fileName = "Log_" + searchName + "_" + LocalDateTime.now().toString().replace(":", "_") + ".txt";
			System.out.println(fileName);
			File logFile = new File(folder, fileName);
			sender.sendMessage("Erstelle datei §eShop/GlobalLogs/" + fileName);
			try {
				logFile.createNewFile();
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(logFile));
				osw.write("Einträge vom " + LocalDateTime.now().toString() + "\n");
				osw.write("Insgesamt: " + gloablLog.size() + " Einträge\n");
				osw.write("Für: " + (searchName == null ? "Alle" : searchName) + "\n\n");
				sender.sendMessage("Schreibe Log asynchron...");
				
				IntStream.range(0, gloablLog.size()).mapToObj(index -> gloablLog.get(index) + "\n").forEachOrdered(entry ->{
					try {
						osw.write(entry);
					} catch (IOException e) {
						e.printStackTrace();
						sender.sendMessage("§cFehler beim schreiben! -> StackTrace ansehen");
					}
				});
				
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
				sender.sendMessage("§cFehler beim schreiben! -> StackTrace ansehen");
			}
			
			sender.sendMessage("Logdatei wurde mit §e" + gloablLog.size() + "§f Einträgen geschrieben.");
		});
	}
	
	@Subcommand("toplist")
	@CommandPermission("shop.metrics.toplist")
	@CommandCompletion("@nothing @ToplistScheme @ToplistTimespan")
	@Description("Öffnet den Editor des Adminshops.")
	@Syntax("<Einträge> <Scheme> <Zeitspanne>")
	public void listShops(Player sender, int amount, ToplistScheme scheme, ToplistTimespan timespan) {
		
		ShopCore.newChain().asyncFirst(() ->{
			return this.dataManager.getToplist(amount, TransactionOccasion.PLAYER_SHOP_BUY, timespan);
		}).abortIf(list -> list.isEmpty(), new TaskChainAbortAction<String,String,String>(){
			
			@Override
			public void onAbort(TaskChain<?> chain, String arg1, String arg2, String arg3) {
				sender.sendMessage(F.error("Es gibt keine Einträge."));
			}
			
		}).syncLast((list) ->{
			
			switch(scheme) {
			case CHAT:
				sender.sendMessage("§e<--- §fSpielershop Topliste§6 " + timespan.getDisplayName() + " §e --->");
				IntStream.range(0, 2).mapToObj(i -> "§6" + (i + 1) + ": §f" + list.get(i)).forEachOrdered(entry -> sender.sendMessage(entry));
				sender.sendMessage("§e<--------- ------------------------- --------->");
				break;
			case FILE:
				File statFolder = new File(ShopCore.getInstance().getDataFolder() + File.separator + "Toplists");
				if(!statFolder.exists()) statFolder.mkdirs();
				
				String fileName = "PlayerShopToplist_" + LocalDateTime.now().toString() + ".txt";
				
				File topFile = new File(statFolder, fileName);
				
				sender.sendMessage(F.main("System", "Schreibe §e" + fileName + " §7Zeitspanne:§e " + timespan.displayName + "§7 mit §e" + amount + "§7Einträgen in den Ordner§e " + "Toplists"));
				sender.sendMessage(F.main("System", "Dies kann je nach Größe etwas Zeit in Anspruch nehmen."));
				try {
					topFile.createNewFile();
					OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(topFile));
					osw.write("Einträge vom " + LocalDateTime.now().toString() + "\n");
					osw.write("Insgesamt: " + amount + " Einträge\n");
					osw.write("Zeitspanne: " + timespan.getDisplayName() + "\n\n");
					IntStream.range(0, 2).mapToObj(i -> "§6" + (i + 1) + ": " + list.get(i) + "\n").forEachOrdered(line -> {
						try {
							osw.write(line);
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
					osw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				sender.sendMessage(F.main("System", "File wurde geschrieben."));
				break;
			case HOLOGRAM:
				this.createToplistHologram(amount, sender.getLocation().getBlock().getLocation().add(0.5, 0.5 + amount, 0.5), timespan);
				break;
			default:
				break;
			}
			
		})
		.execute();
		
	}
	
	private void createToplistHologram(int amount, Location location, ToplistTimespan timespan) {
		ShopCore.getInstance().getStatisticManager().createHologram(location, timespan, amount);
	}
	
	@AllArgsConstructor
	public static enum ToplistTimespan {
		
		DAY("der letzten 24 Stunden", 1),
		WEEK("der letzten 7 Tage", 7),
		MONTH("des letzten Monats", 30),
		GLOBAL("absolut", 1000);
		
		@Getter
		private final String displayName;
		@Getter
		private final int days;
		
	}
	
	public static enum ToplistScheme {
		
		CHAT, FILE, HOLOGRAM;
		
	}
}
