package com.avarioncraft.shopsystem;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.avarioncraft.shopsystem.auctionhouse.AuctionAccountManager;
import com.avarioncraft.shopsystem.auctionhouse.AuctionHouse;
import com.avarioncraft.shopsystem.blocklinks.PersistentAdminShop;
import com.avarioncraft.shopsystem.blocklinks.PersistentPlayerShop;
import com.avarioncraft.shopsystem.blocklinks.PlayerShopItem;
import com.avarioncraft.shopsystem.commands.AdminShopCommands;
import com.avarioncraft.shopsystem.commands.AuctionCommand;
import com.avarioncraft.shopsystem.commands.PlayerShopCommands;
import com.avarioncraft.shopsystem.commands.StatisticCommands;
import com.avarioncraft.shopsystem.commands.StatisticCommands.ToplistScheme;
import com.avarioncraft.shopsystem.commands.StatisticCommands.ToplistTimespan;
import com.avarioncraft.shopsystem.listener.CitizenListener;
import com.avarioncraft.shopsystem.listener.LinkListener;
import com.avarioncraft.shopsystem.listener.PlayerListener;
import com.avarioncraft.shopsystem.listener.StatisticListener;
import com.avarioncraft.shopsystem.manager.AdminShopManager;
import com.avarioncraft.shopsystem.manager.ItemSafetyManager;
import com.avarioncraft.shopsystem.manager.PlayerShopManager;
import com.avarioncraft.shopsystem.manager.PlayershopInventoryManager;
import com.avarioncraft.shopsystem.manager.StatisticManager;
import com.avarioncraft.shopsystem.sql.DatabaseConnectionHandler;
import com.avarioncraft.shopsystem.sql.DatabaseManager;
import com.avarioncraft.shopsystem.tradesystem.EconomyEnvironment;
import com.avarioncraft.shopsystem.utils.Eco;
import com.avarioncraft.shopsystem.utils.HeadProvider;
import com.avarioncraft.shopsystem.utils.itemchooser.ItemChooserManager;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import net.crytec.API;
import net.crytec.acf.InvalidCommandArgument;
import net.crytec.acf.PaperCommandManager;
import net.crytec.api.persistentblocks.PersistentBlockManager;
import net.crytec.shaded.org.apache.lang3.EnumUtils;
import net.crytec.taskchain.BukkitTaskChainFactory;
import net.crytec.taskchain.TaskChain;
import net.crytec.taskchain.TaskChainFactory;

public class ShopCore extends JavaPlugin {
	
	@Getter
	private static ShopCore instance;
	
	@Getter
	private PersistentBlockManager perBlockManager;
	@Getter
	private StatisticManager statisticManager;
	@Getter
	private AdminShopManager adminShopManager;
	@Getter
	private PlayerShopManager playerShopManager;
	@Getter
	private ItemChooserManager itemChooserManager;
	@Getter
	private EconomyEnvironment economyEnvironment;
	@Getter
	private PlayershopInventoryManager playershopInventoryManager;
	@Getter
	private HeadProvider headProvider;
	@Getter
	private AuctionHouse auctionHouse;
	@Getter
	private ItemSafetyManager itemSafetyManager;
	
	@Getter
	private DatabaseConnectionHandler handler;
	@Getter
	private DatabaseManager databaseManager;
	@Getter
	private static TaskChainFactory taskChainFactory;
	
	private final LocalDate testmode = LocalDate.of(2019, 04, 25);
	
	@Override
	public void onLoad() {
		instance = this;
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdir();
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		
		if (LocalDate.now().isAfter(testmode)) {
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		Eco.setupEconomy(this);
		this.headProvider = new HeadProvider(this);
		taskChainFactory = BukkitTaskChainFactory.create(this);
		
		Bukkit.getPluginManager().registerEvents(new LinkListener(), this);
		
		File config = new File(this.getDataFolder(), "config.yml");
		if (!config.exists()) {
			this.saveResource("config.yml", true);
			this.reloadConfig();
			Bukkit.getConsoleSender().sendMessage("[" + this.getName() + "] §2Konfiguration wurde gespeichert.");
		}
		
		this.perBlockManager = API.getInstance().getPersistentBlocks();
		this.handler = new DatabaseConnectionHandler(this);
		
		new PlayerListener(this);
		
		this.auctionHouse = new AuctionHouse(new AuctionAccountManager(this));
		this.databaseManager = new DatabaseManager(this.handler, this.auctionHouse);
		new StatisticListener(this);
		this.auctionHouse.load();
		
		this.playershopInventoryManager = new PlayershopInventoryManager(this);
		this.adminShopManager = new AdminShopManager(this);
		this.playerShopManager = new PlayerShopManager(this);
		
		this.statisticManager = new StatisticManager(this);
		this.itemChooserManager = new ItemChooserManager(this);
		
		// Load Adminshops
		this.adminShopManager.loadFromFile();
		
		// Load Playershops
		this.playerShopManager.loadAllShops();
		
		this.itemSafetyManager = new ItemSafetyManager(this);
		
		this.economyEnvironment = new EconomyEnvironment(this);
		this.economyEnvironment.load();
		
		this.perBlockManager.registerPersistentBlock("PlayerShop", () -> new PersistentPlayerShop());
		this.perBlockManager.registerPersistentBlock("AdminShop", () -> new PersistentAdminShop());
		
		PaperCommandManager commandManager = new PaperCommandManager(this);
		
		ImmutableSet<String> toplistSchemes = ImmutableSet.copyOf(Arrays.stream(ToplistScheme.values()).map(scheme -> scheme.toString()).collect(Collectors.toSet()));
		ImmutableSet<String> toplistTimes = ImmutableSet.copyOf(Arrays.stream(ToplistTimespan.values()).map(scheme -> scheme.toString()).collect(Collectors.toSet()));
		
		commandManager.getCommandCompletions().registerAsyncCompletion("ToplistTimespan", c -> {
			return toplistTimes;
		});
		
		commandManager.getCommandCompletions().registerAsyncCompletion("ToplistScheme", c -> {
			return toplistSchemes;
		});
		
		commandManager.getCommandContexts().registerContext(ToplistTimespan.class, c -> {
			
			String input = c.popFirstArg();
			
			if (!EnumUtils.isValidEnum(ToplistTimespan.class, input)) {
				throw new InvalidCommandArgument("Diesen ToplistScheme gibt es nicht.");
			} else {
				return ToplistTimespan.valueOf(input.toUpperCase());
			}
		});
		
		commandManager.getCommandContexts().registerContext(ToplistScheme.class, c -> {
			
			String input = c.popFirstArg();
			
			if (!EnumUtils.isValidEnum(ToplistScheme.class, input)) {
				throw new InvalidCommandArgument("Diesen ToplistScheme gibt es nicht.");
			} else {
				return ToplistScheme.valueOf(input.toUpperCase());
			}
		});
		
		API.getInstance().getCustomItemFactory().registerItem(new PlayerShopItem());
		
		commandManager.registerCommand(new AdminShopCommands());
		commandManager.registerCommand(new PlayerShopCommands(this));
		commandManager.registerCommand(new AuctionCommand(this));
		commandManager.registerCommand(new StatisticCommands(this));
		
		commandManager.enableUnstableAPI("help");
		
		
		if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
			Bukkit.getPluginManager().registerEvents(new CitizenListener(), this);
			Bukkit.getConsoleSender().sendMessage("§2Citizens support aktiviert");
		}
		
	}
	
	@Override
	public void onDisable() {
		this.getAdminShopManager().saveToFile();
		this.getPlayerShopManager().saveAllShops();
		this.economyEnvironment.save();
		this.getHandler().shutdown();
	}
	
	public String getServername() {
		if (API.getInstance().getRedisManager() == null) {
			return this.getConfig().getString("auctions.servername", "defaultserver");
		} else {
			return API.getInstance().getRedisManager().getServer();
		}
	}
	
	public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }
}
