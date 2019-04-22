package com.avarioncraft.shopsystem.playershops;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.blocklinks.PersistentPlayerShop;
import com.avarioncraft.shopsystem.statistics.TransactionEvent;
import com.avarioncraft.shopsystem.statistics.TransactionOccasion;
import com.avarioncraft.shopsystem.tradesystem.BuyRequestResponse;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.tradesystem.SellRequestResponse;
import com.avarioncraft.shopsystem.tradesystem.ShopHolder;
import com.avarioncraft.shopsystem.tradesystem.TradeGood;
import com.avarioncraft.shopsystem.tradesystem.TransactionIdentity;
import com.avarioncraft.shopsystem.utils.DateTickFormat;
import com.avarioncraft.shopsystem.utils.Eco;
import com.avarioncraft.shopsystem.utils.PermissionRegistrar;
import com.avarioncraft.shopsystem.utils.TimeRange;
import com.avarioncraft.shopsystem.utils.UtilPlayer;
import com.avarioncraft.shopsystem.utils.paintableMap.GraphRenderer;
import com.avarioncraft.shopsystem.utils.paintableMap.MapGraph;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;

import lombok.Getter;
import lombok.Setter;
import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.util.language.LanguageHelper;
import net.crytec.shaded.org.apache.lang3.ArrayUtils;

public class PlayerShop implements ShopHolder{
	
	public PlayerShop() {
		this.timeRange.implementRanges(IntStream.rangeClosed(1, 24).toArray());
		this.shopEditor = new PlayershopEditor(this);
	}
	
	private transient static final Permission defaultPermission = PermissionRegistrar.addPermission("shop.playershop.use", "Erlaubt das nutzen des Playershops", PermissionDefault.TRUE);
	
	@Getter @Setter
	private String displayName = "Spielershop";
	@Getter @Setter
	private UUID shopID;
	@Getter @Expose
	private ItemWare tradeWare = new ItemWare(new ItemBuilder(Material.BARRIER).name("§cKeine Ware vorhanden").build());
	@Getter @Setter
	private double sellPrice = 0.0D;
	@Getter @Setter
	private double buyPrice = 0.0D;
	@Getter @Setter
	private UUID ownerID;
	@Getter @Setter
	private boolean buyingShop = false;
	@Getter @Setter
	private boolean sellingShop = true;
	@Getter @Setter
	private Inventory physicalInventory;
	@Getter @Setter
	private boolean realTime = true;
	@Getter
	private final transient TimeRange timeRange = new TimeRange();
	@Getter @Setter
	private int sells = 0;
	@Getter @Setter
	private int buys = 0;
	@Getter @Setter
	private double bank = 0.0D;
	@Getter @Setter
	private double rentCost = 0.0D;
	@Getter @Setter
	private transient PersistentPlayerShop physicalShop;
	@Getter @Setter
	private int stampPeriod = 1;
	
	private long lastRent = 0;
	
	private transient boolean update = true;
	
	private LinkedList<Integer> sellMetric = Lists.newLinkedList(IntStream.range(0, 128).boxed().map(i -> 0).collect(Collectors.toList()));
	private LinkedList<Integer> buyMetric = Lists.newLinkedList(IntStream.range(0, 128).boxed().map(i -> 0).collect(Collectors.toList()));
	private int currentMaxSells = 10;
	
	public void stamp(int stampPeriod) {
		
		this.stampPeriod = stampPeriod;
		
		sellMetric.pop();
		sellMetric.add(this.sells);
		this.currentMaxSells = sellMetric.stream().mapToInt(i -> i).max().orElseGet(() -> 10);
		if(this.currentMaxSells < 10) this.currentMaxSells = 10;
		
		buyMetric.pop();
		buyMetric.add(this.buys);
		
		this.buys = 0;
		this.sells = 0;
		this.update = true;
	}
	
	public ItemStack getGraphMap() {
		
		if(!this.mapRendered) {
			
			MapView view = Bukkit.createMap(this.physicalShop.getLocation().getWorld());
			
			MapMeta meta = (MapMeta) this.mapItem.getItemMeta();
			view.setScale(Scale.FARTHEST);
			view.addRenderer(new GraphRenderer(new MapGraph() {

				@Override
				public String getTitle() {
					return ChatColor.stripColor(tradeWare.getWareDisplayName());
				}

				@Override
				public byte getBackColor() {
					return (byte) 34;
				}

				@Override
				public int getUpperBound() {
					return currentMaxSells;
				}

				@Override
				public ArrayList<int[]> getGraphValues() {
					
					int[] graph_sells = ArrayUtils.toPrimitive(sellMetric.toArray(new Integer[sellMetric.size()]));
					int[] graph_buys = ArrayUtils.toPrimitive(buyMetric.toArray(new Integer[buyMetric.size()]));
					
					return Lists.newArrayList(graph_sells, graph_buys);
				}

				@Override
				public byte[] getGraphColors() {
					return new byte[] {(byte) 77, (byte) 122};
				}

				@Override
				public String getValueUnit() {
					return " Waren ";
				}

				@Override
				public boolean checkUpdate() {
					if(update) {
						update = false;
						return true;
					}
					
					return false;
				}

				@Override
				public Pair<Integer, String> getDynamicRange() {
					return Pair.of(stampPeriod, "Min");
				}
				
			}));
			
			meta.setMapView(view);
			
			this.mapItem.setItemMeta(meta);
			this.mapRendered = true;
		}
		
		return ShopCore.getInstance().getItemSafetyManager().tagItem(this.mapItem.clone());
	}
	
	private transient boolean mapRendered = false;
	private final transient ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
	@Getter @Setter
	private transient boolean activeMap = false;
	
	Map<Long, Integer> shopBought = Maps.newHashMap();
	Map<Long, Integer> shopSold = Maps.newHashMap();
	Map<Long, Double> shopCapital = Maps.newHashMap();
	
	public void setTradeWare(ItemWare tradeWare) {
		ItemStack validItem = tradeWare.value;
		this.mapRendered = false;
		this.update = true;
		for(int slot = 0; slot < physicalInventory.getSize(); slot++) {
			ItemStack item = physicalInventory.getItem(slot);
			if(item != null && !item.asOne().isSimilar(validItem)) {
				physicalInventory.setItem(slot, null);
				this.getPhysicalShop().getLocation().getWorld().dropItemNaturally(this.getPhysicalShop().getLocation().clone().add(0.5, 1.5, 0.5), item);
			}
		}
		this.tradeWare = tradeWare;
	}
	
	public boolean payRent() {
		if(System.currentTimeMillis() - this.lastRent < 86400000L) return true;
		if(this.bank < this.rentCost) return false;
		this.lastRent = System.currentTimeMillis();
		this.bank -= this.rentCost;
		return true;
	}
	
	private final transient PlayershopEditor shopEditor;
	
	public void withdrawStoredMoney(Player player, double amount) {
		Eco.give(player, amount);
		this.bank -= amount;
	}
	
	public void depositStoredMoney(Player player, double amount) {
		Eco.withdraw(player, amount);
		this.bank += amount;
	}
	
	public int getFreeSpace() {
		if(this.physicalInventory == null) return 0;
		if(this.tradeWare == null) return 0;
		int singleMax = this.tradeWare.value.getMaxStackSize();
		return Arrays.stream(this.physicalInventory.getContents())
				.mapToInt(slot ->(slot == null ? singleMax : singleMax - slot.getAmount()))
				.sum();
	}
	
	public int getAmount() {
		return (this.physicalInventory.getContents().length * this.tradeWare.value.getMaxStackSize()) - this.getFreeSpace();
	}
	
	public void openInventory(Player player) {
		player.openInventory(this.physicalInventory);
	}
	
	public void openForEdit(Player player) {
		this.shopEditor.openForEdit(player);
	}
	
	public boolean isOpen(World world) {
		LocalTime time;
		if (this.realTime) {
			time = LocalTime.now();
		} else {
			Date date = DateTickFormat.ticksToDate(world.getTime());
			Instant instant = Instant.ofEpochMilli(date.getTime());
			time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
		}
		return this.timeRange.isInRange(time.getHour());
	}
	
	@Override
	public BuyRequestResponse playerBuyRequest(Player player, int amount) {
		
		double cost = amount * this.buyPrice;
		
		if(!this.sellingShop) return BuyRequestResponse.NO_SELLING;
		if(player.getGameMode().equals(GameMode.CREATIVE)) return BuyRequestResponse.CREATIVE_BLOCK;
		if(amount > this.getAmount()) return BuyRequestResponse.NO_STOCK;
		if(!Eco.has(player, cost)) return BuyRequestResponse.NO_MONEY;
		if(!player.hasPermission(defaultPermission)) return BuyRequestResponse.NO_PERMISSION;
		
		LocalTime time;
		if(this.realTime) {
			time = LocalTime.now();
		}else {
			Date date = DateTickFormat.ticksToDate(player.getWorld().getTime());
			Instant instant = Instant.ofEpochMilli(date.getTime());
		    time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
		}
		
		if(!this.timeRange.isInRange(time.getHour())) return BuyRequestResponse.WRONG_TIME;
		
		UUID playerID = player.getUniqueId();
		
		TradeGood<?> goods = this.tradeWare.getGood(TransactionIdentity.PLAYERSHOP.getTransactionString(this.shopID, playerID), cost, amount, this.shopID);
		
		TransactionEvent event = new TransactionEvent(playerID, this.ownerID, this.shopID, goods, cost, 1.0D, TransactionOccasion.PLAYER_SHOP_BUY);
		
		event.callEvent();
		
		Eco.withdraw(player, event.getTransactionMoney());
		
		goods.deliver(player);
		
		UtilPlayer.takeItems(this.physicalInventory, this.tradeWare.value, amount);
		
		this.setBank(bank + cost);
		
		this.sells += amount;
		
		return BuyRequestResponse.SUCCESS;
	}
	
	@Override
	public SellRequestResponse playerSellRequest(Player player, int amount) {

		double cost = this.sellPrice * amount;
		
		if(!player.hasPermission(defaultPermission)) return SellRequestResponse.NO_PERMISSION;
		if(!this.buyingShop) return SellRequestResponse.NO_BUYING;
		if(player.getGameMode().equals(GameMode.CREATIVE)) return SellRequestResponse.CREATIVE_BLOCK;
		if(!this.tradeWare.getGood(null, 0, amount, null).has(player)) return SellRequestResponse.NOT_ENOUGH_WARE;
		if(this.bank < cost) return SellRequestResponse.NOT_ENOUGH_MONEY;
		if(this.getFreeSpace() < amount) return SellRequestResponse.MAX_STOCK;
		
		LocalTime time;
		
		if(this.realTime) {
			time = LocalTime.now();
		}else {
			Date date = DateTickFormat.ticksToDate(player.getWorld().getTime());
			Instant instant = Instant.ofEpochMilli(date.getTime());
		    time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
		}
		
		if(!this.timeRange.isInRange(time.getHour())) return SellRequestResponse.WRONG_TIME;
		
		UUID playerID = player.getUniqueId();
		
		TradeGood<?> goods = this.tradeWare.getGood(TransactionIdentity.PLAYERSHOP.getTransactionString(this.shopID, playerID), cost, amount, this.shopID);
		
		if(!goods.has(player)) return SellRequestResponse.NOT_ENOUGH_WARE;
		
		TransactionEvent event = new TransactionEvent(playerID, this.ownerID, this.shopID, goods, cost, 1.0, TransactionOccasion.PLAYER_SHOP_SELL);
		
		if(!Eco.give(player, event.getTransactionMoney()).transactionSuccess()) return SellRequestResponse.MONEY_ERROR;
		
		goods.take(player);
		
		UtilPlayer.giveItems(this.physicalInventory, this.tradeWare.value, amount, false, null);
		this.setBank(this.bank - cost);
		
		this.buys += amount;
		
		return SellRequestResponse.SUCCESS;
	}
	
	private final String buySellLine() {
		StringBuilder buySellLine = new StringBuilder();
		
//		buySellLine.append("§f<§6L§f> ");
		buySellLine.append((this.sellingShop ? "§2Kaufen" : "§7Kaufen"));
		buySellLine.append("§7   ");
		buySellLine.append((this.buyingShop ? "§cVerkaufen" : "§7Verkaufen"));
//		buySellLine.append(" §f<§6R§f>");
		
		return buySellLine.toString();
	}
	
	private final String priceLine() {
		StringBuilder priceLine = new StringBuilder();
		
		priceLine.append("§2" + this.buyPrice);
		priceLine.append("§7 - §6⚖§7 - ");
		priceLine.append("§c" + this.sellPrice);
		
		return priceLine.toString();
	}
	
	@Override
	public ArrayList<Supplier<String>> getShopInfo(Location location) {
		ArrayList<Supplier<String>> lines = Lists.newArrayList();
		
		lines.add(() -> "§b" + LanguageHelper.getItemDisplayName(this.tradeWare.value));
		
		lines.add(() -> "«««««« §e§lShop§r »»»»»»");
		
		lines.add(() -> "" + (this.isOpen(location.getWorld()) ? "§a⌚ Offen ⌚" : "§c⌚ Geschlossen ⌚"));
		
		lines.add(() -> this.priceLine());
		
		lines.add(() -> this.buySellLine());
		
		return lines;
	}
	
	@Override
	public UUID getShopHolderID() {
		return this.shopID;
	}

}
