package com.avarioncraft.shopsystem.adminshops;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.statistics.TransactionEvent;
import com.avarioncraft.shopsystem.statistics.TransactionOccasion;
import com.avarioncraft.shopsystem.tradesystem.BuyRequestResponse;
import com.avarioncraft.shopsystem.tradesystem.SellRequestResponse;
import com.avarioncraft.shopsystem.tradesystem.ShopHolder;
import com.avarioncraft.shopsystem.tradesystem.TradeGood;
import com.avarioncraft.shopsystem.tradesystem.TradeWare;
import com.avarioncraft.shopsystem.tradesystem.TransactionIdentity;
import com.avarioncraft.shopsystem.utils.DateTickFormat;
import com.avarioncraft.shopsystem.utils.Eco;
import com.avarioncraft.shopsystem.utils.PermissionRegistrar;
import com.avarioncraft.shopsystem.utils.TimeRange;
import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;

import lombok.Getter;
import lombok.Setter;
import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.util.UtilMath;
import net.crytec.api.util.UtilTime;
import net.crytec.api.util.language.LanguageHelper;

public class AdminShop implements ShopHolder{
	
	public AdminShop() {
		this.timeRange.implementRanges(IntStream.rangeClosed(1, 24).toArray());
	}
	
	@Expose
	private transient static final Permission defaultPermission = PermissionRegistrar.addPermission("shop.adminshop.use", "Erlaubt das nutzen des Adminshops", PermissionDefault.TRUE);
	
	@Setter
	private String displayName = "Adminshop";
	@Getter @Setter
	private UUID shopID;
	@Getter @Setter @Expose
	private transient TradeWare<?> tradeWare;
	@Getter @Setter
	private int amount = 0;
	@Getter @Setter
	private int baseAmount = 0;
	@Getter @Setter
	private double basePrice = 0D;
	@Getter @Setter
	private boolean limited = false;
	@Getter @Setter
	private long refillIntervall = 0L;
	@Getter @Setter
	private String refillDisplay = "";
	@Getter @Setter
	private long nextRefill = 0L;
	@Getter @Setter @Expose
	private transient Permission permission = defaultPermission;
	@Getter @Setter
	private boolean enabled = true;
	@Getter @Setter
	private boolean realTime = false;
	@Getter @Setter
	private boolean buying = true;
	@Getter @Setter
	private boolean selling = true;
	@Getter @Setter
	private boolean listed = true;
	@Getter
	private final transient TimeRange timeRange = new TimeRange();
	@Getter @Setter
	private double sellWeight = 0.5D;
	@Getter
	private int buys = 0;
	@Getter
	private int sells = 0;
	@Getter @Setter
	private String category = "keine";
	
	public ItemStack getEditorIcon() {
		ItemStack item = (this.tradeWare == null) ? new ItemBuilder(Material.BEDROCK).name("§7Keine Ware vorhanden...").build() : this.tradeWare.getIcon().clone();
		
		return new ItemBuilder(item)
				.lore("")
				.lore("§6Klicken, um die Ware zu ändern.")
				.build();
	}
	
	public ItemStack getShopIcon() {
		
		if(this.tradeWare == null) return new ItemBuilder(Material.BEDROCK)
				.name("§7Keine Ware vorhanden...")
				.build();
		
		ItemBuilder builder = new ItemBuilder(this.tradeWare.getIcon().clone())
				.name("§b" + LanguageHelper.getItemDisplayName(this.tradeWare.getIcon()))
				.lore("")
				.lore("§eBasisPreis: §7" + this.basePrice)
				.lore("§eMomentaner Kaufpreis: §7" + this.evaluatePrice(BuySell.BUY))
				.lore("§eMomentaner Ankaufpreis: §7" + this.evaluatePrice(BuySell.SELL))
				.lore("");
		
		        
		        if (this.isLimited()) {
		        builder.lore("§7Du kannst §e" + (this.amount * this.tradeWare.getAmount()) + " §7Waren kaufen.");
		        builder.lore("§7Du kannst §e" + ((this.baseAmount - this.amount) * tradeWare.getAmount()) + " §7Waren verkaufen.");
				
				if (this.amount >= this.baseAmount) {
					builder.lore("")
					.lore("§cLagerkapazität erreicht!")
					.lore("§7Ankaufspreis um §450%§7 reduziert.");
				    }
		        }
				
				if (!this.refillDisplay.isEmpty()) {
					builder.lore("")
					.lore("§eNachfüllintervall: §7" + (this.refillDisplay.isEmpty() ? "nie" : "alle " + this.refillDisplay))
					.lore("")
					.lore("§eNächste Auffüllung: §7" + ((nextRefill == 0) ? "Niemals" : UtilTime.when(this.nextRefill)));
				}
				
				builder.lore("").lore("§eShop ist momentan: " + (enabled ? "§aOffen" : "§cGeschlossen"));

		return builder.build();

	}
	
	public ItemStack getListIcon() {
		
		ItemStack item = (this.tradeWare == null) ? new ItemBuilder(Material.BEDROCK).name("§7Keine Ware vorhanden...").build() : this.tradeWare.getIcon().clone();
		int amount = this.tradeWare == null ? 0 : this.tradeWare.getAmount();
		
		return new ItemBuilder(item)
				.name("§b" + LanguageHelper.getItemDisplayName(item))
				.lore("")
				.lore("§6[" + (this.listed ? "§aGelistet" : "§cNicht gelistet") + "§6]")
				.lore("")
				.lore("§eBasisPreis: §7" + this.basePrice)
				.lore("§eMomentaner Kaufpreis: §7" + this.evaluatePrice(BuySell.BUY))
				.lore("§eMomentaner Ankaufpreis: §7" + this.evaluatePrice(BuySell.SELL))
				.lore("")
				.lore("§eKurs: §7" + getBaseRation(BuySell.BUY) + "% §e|§7 " + getBaseRation(BuySell.SELL) + "%")
				.lore("")
				.lore("§eAnzahl: §7" + (this.amount * amount) + " §e/§7 " + (this.baseAmount * amount) + " §e| " + (limited ? "§cLimitiert" : "§aUnlimitiert"))
				.lore("§eNachfüllzeit: §7" + this.refillDisplay)
				.lore("")
				.lore("§eKaufPermission: §7" + this.permission.getName())
				.lore("")
				.lore("§eEchtzeit: " + (realTime ? "§aJa" : "§cNein"))
				.lore("")
				.lore("§e" + this.sells + "§7 verkauft")
				.lore("§e" + this.buys + "§7 eingekauft")
				.lore("")
				.lore("§eShop ist momentan: " + (enabled ? "§aOffen" : "§cGeschlossen"))
				.lore("")
				.lore("§e[§6Klicken, um Shop zu editieren§e]")
				.build();
	}
	
	public double getBaseRation(BuySell buySell) {
		return (100 / this.basePrice) * this.evaluatePrice(buySell);
	}
	
	public void refill() {
		this.nextRefill = this.refillIntervall + System.currentTimeMillis();
		this.amount = this.baseAmount;
	}
	
	public boolean shouldRefill() {
		if(!this.limited) return false;
		return this.nextRefill < System.currentTimeMillis();
	}
	
	public double evaluatePrice(BuySell buySell) {
		if(this.tradeWare == null) return 0D;
		
		double price = UtilMath.unsafeRound(ShopCore.getInstance().getEconomyEnvironment().evaluateMarketPrice(this.tradeWare.getGood("", this.basePrice, 1, this.shopID), (buySell.equals(BuySell.BUY) ? 1 : this.sellWeight)), 2);
		if(this.limited && this.amount >= this.amount) price *= 0.5;
		return price;
	}
	
	@Override
	public SellRequestResponse playerSellRequest(Player player, int amount) {
		
		double cost = this.evaluatePrice(BuySell.SELL) * amount;
		if(this.tradeWare == null) return SellRequestResponse.NO_BUYING;
		if(!this.buying) return SellRequestResponse.NO_BUYING;
		if(player.getGameMode().equals(GameMode.CREATIVE) && !player.isOp()) return SellRequestResponse.CREATIVE_BLOCK;
		if(this.limited && (this.amount + amount) > this.baseAmount) return SellRequestResponse.MAX_STOCK;
		if(!player.hasPermission(this.permission)) return SellRequestResponse.NO_PERMISSION;
		if(!this.enabled) return SellRequestResponse.NOT_ENABLED;
		
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
		
		TradeGood<?> goods = this.tradeWare.getGood(TransactionIdentity.ADMINSHOP.getTransactionString(this.shopID, playerID), cost, amount, this.shopID);
		
		if(!goods.has(player)) return SellRequestResponse.NOT_ENOUGH_WARE;
		
		TransactionEvent event = new TransactionEvent(playerID, null, this.shopID, goods, cost, this.sellWeight, TransactionOccasion.ADMIN_SHOP_SELL);
		
		if(!Eco.give(player, event.getTransactionMoney()).transactionSuccess()) return SellRequestResponse.MONEY_ERROR;
		
		goods.take(player);
		
		if(this.limited) this.amount += amount;
		
		this.buys += amount;
		
		return SellRequestResponse.SUCCESS;
	}
	
	@Override
	public BuyRequestResponse playerBuyRequest(Player player, int amount) {
		
		double cost = this.evaluatePrice(BuySell.BUY) * amount;
		if(this.tradeWare == null) return BuyRequestResponse.NO_SELLING;
		if(!this.selling) return BuyRequestResponse.NO_SELLING;
		if(player.getGameMode().equals(GameMode.CREATIVE) && !player.isOp()) return BuyRequestResponse.CREATIVE_BLOCK;
		if(this.limited && this.amount < amount) return BuyRequestResponse.NO_STOCK;
		if(!player.hasPermission(this.permission)) return BuyRequestResponse.NO_PERMISSION;
		if(!this.enabled) return BuyRequestResponse.NOT_ENABLED;
		
		if(!Eco.has(player, cost)) return BuyRequestResponse.NO_MONEY;
		
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
		
		TradeGood<?> goods = this.tradeWare.getGood(TransactionIdentity.ADMINSHOP.getTransactionString(this.shopID, playerID), cost, amount, this.shopID);
		
		TransactionEvent event = new TransactionEvent(playerID, null, this.shopID, goods, cost, 1.0D, TransactionOccasion.ADMIN_SHOP_BUY);
		
		event.callEvent();
		
		Eco.withdraw(player, event.getTransactionMoney());
		
		goods.deliver(player);
		
		if(this.limited) this.amount -= amount;
		
		this.sells += amount;
		
		return BuyRequestResponse.SUCCESS;
	}
	
	public enum BuySell{
		BUY, SELL;
	}
	
	public String getDisplayName() {
		return ChatColor.translateAlternateColorCodes('&', this.displayName);
	}

	@Override
	public ArrayList<Supplier<String>> getShopInfo(Location loc) {
		return Lists.newArrayList();
	}

	@Override
	public UUID getShopHolderID() {
		return this.getShopID();
	}
}