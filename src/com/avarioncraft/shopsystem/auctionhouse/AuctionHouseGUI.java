package com.avarioncraft.shopsystem.auctionhouse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.sql.AuctionList;
import com.avarioncraft.shopsystem.utils.Eco;
import com.avarioncraft.shopsystem.utils.UtilTime;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.smartInv.ClickableItem;
import net.crytec.api.smartInv.SmartInventory;
import net.crytec.api.smartInv.buttons.InputButton;
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.smartInv.content.Pagination;
import net.crytec.api.smartInv.content.SlotIterator;
import net.crytec.api.smartInv.content.SlotPos;
import net.crytec.api.smartInv.content.SlotIterator.Type;
import net.crytec.api.util.F;
import net.crytec.api.util.UtilMath;
import net.crytec.api.util.UtilPlayer;
import net.crytec.api.util.anvilgui.AnvilGUI;

public class AuctionHouseGUI implements InventoryProvider{
	
	
	
	public AuctionHouseGUI(AuctionHouse auctionHouse) {
		this.auctionHouse = auctionHouse;
		this.requestCashe = Maps.newHashMap();
		this.REQUEST_CASHE_TIME_MS = ShopCore.getInstance().getConfig().getInt("auctions.casheTime") * 1000L;
		Arrays.stream(AuctionSortOrder.values()).forEach(order -> this.requestCashe.put(order, Pair.of(0L, Lists.newLinkedList())));
		this.mirror = this;
		this.auctionBasePrice = ShopCore.getInstance().getConfig().getDouble("auctions.basePrice");
		this.pricePerMinute = ShopCore.getInstance().getConfig().getDouble("auctions.pricePerMinute");
	}
	
	private final ItemStack moneyHead = ShopCore.getInstance().getHeadProvider().get("MoneyHead");
	private final ItemStack chestHead = ShopCore.getInstance().getHeadProvider().get("ChestHead");
	private final ItemStack goldHead = ShopCore.getInstance().getHeadProvider().get("GoldBlock");
	private final ItemStack greenClock = ShopCore.getInstance().getHeadProvider().get("GreenClock");
	private final ItemStack blueClock = ShopCore.getInstance().getHeadProvider().get("BlueClock");
	private final ItemStack purpleClock = ShopCore.getInstance().getHeadProvider().get("PurpleClock");
	private final ItemStack redClock = ShopCore.getInstance().getHeadProvider().get("RedClock");
	private final ItemStack letterBox = ShopCore.getInstance().getHeadProvider().get("LetterBox");
	private final ItemStack paperStack = ShopCore.getInstance().getHeadProvider().get("PaperStack");
	private final Map<AuctionSortOrder, Pair<Long, LinkedList<RedisAuctionWrapper>>> requestCashe;
	private final long REQUEST_CASHE_TIME_MS;
	private final AuctionHouse auctionHouse;
	private final double auctionBasePrice;
	private final double pricePerMinute;
	
	private final AuctionHouseGUI mirror;
	
	public void openFor(Player player, AuctionSortOrder order) {
		Pair<Long, LinkedList<RedisAuctionWrapper>> cashedPair = this.requestCashe.get(order);
		
		if (cashedPair.getKey() + this.REQUEST_CASHE_TIME_MS >= System.currentTimeMillis()) {
			
			SmartInventory.builder()
			.size(5)
			.provider(this)
			.title("Auktionshaus")
			.build()
			.openFor(player)
			.data("Order", order)
			.data("Auctions", cashedPair.getValue())
			.open();
		} else {
			ShopCore.newChain()
			.asyncFirstFuture(new AuctionList(ShopCore.getInstance().getDatabaseManager(), order))
			.abortIfNull()
			.syncLast((auctions) -> {
				
				SmartInventory.builder()
				.size(5)
				.provider(this)
				.title("Auktionshaus")
				.build()
				.openFor(player)
				.data("Order", order)
				.data("Auctions", auctions)
				.open();
				
				this.requestCashe.put(order, Pair.of(System.currentTimeMillis(), auctions));
				
			})
			.execute();
			
		}
	}
	
	@Override
	public void init(Player player, InventoryContents contents) {
		
		LinkedList<RedisAuctionWrapper> auctions = contents.property("Auctions", Lists.newLinkedList());
		AuctionSortOrder order = contents.property("Order");
		
		Pagination pagination = contents.pagination();
		pagination.setItemsPerPage(36);
		
		pagination.setItems(auctions.stream().map(auc -> auc.getAuctionIcon(player, contents, this, order).get()).toArray(ClickableItem[]::new));
		
		SlotIterator slotIterator = contents.newIterator(Type.HORIZONTAL, 0, 0);
		slotIterator = slotIterator.allowOverride(false);
		pagination.addToIterator(slotIterator);
		
		contents.set(SlotPos.of(4, 3),ClickableItem.of(new ItemBuilder(this.paperStack.clone())
				.name("§eSortierung auswählen")
				.lore("")
				.lore("§eMomentan: §f" + order.getDisplayName())
				.build()
				, event ->{
					SmartInventory.builder()
					.size(2)
					.provider(this.orderChooser)
					.title("Sortierung wählen")
					.build()
					.openFor(player)
					.open();
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
				}));
		
		contents.set(SlotPos.of(4, 4),ClickableItem.of(new ItemBuilder(this.chestHead.clone())
				.name("§eAuktion starten")
				.build()
				, event ->{
					SmartInventory.builder()
					.size(5)
					.provider(this.auctionCreator)
					.title("Auktion starten")
					.build()
					.openFor(player)
					.open();
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
				}));
		
		contents.set(SlotPos.of(4, 5),ClickableItem.of(new ItemBuilder(this.letterBox.clone())
				.name("§eAuktionslager")
				.lore("")
				.lore("§7Hier werden deine gewonnenen")
				.lore("§7Waren gelagert.")
				.build()
				, event ->{
					this.auctionHouse.getAccountManager().getAccountOf(player.getUniqueId()).get().openContent(player);
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
				}));
		
		
		
	}
	
	private InventoryProvider auctionCreator = new InventoryProvider() {
		
		@Override
		public void init(Player player, InventoryContents contents) {
			
			ItemStack emptyIcon = new ItemBuilder(chestHead.clone())
					.name("§eAuktions Ware")
					.lore("")
					.lore("§7Klicke hier mit einem Item auf")
					.lore("§7der Maus, um die Auktions-Ware")
					.lore("§7zu setzen.")
					.build();
			
			int min = contents.property("min", 0);
			int h = contents.property("h", 0);
			int d = contents.property("d", 0);
			int w = contents.property("w", 0);
			
			double price = auctionBasePrice + ((min + (60 * h) + (60 * 24 * d) + (60 * 24 * 7 * w)) * pricePerMinute);
			double auctionBetPrice = contents.property("AuctionBetPrice", 0D);
			
			ItemStack ware = contents.property("Ware", emptyIcon);
			
			contents.set(SlotPos.of(1, 1), ClickableItem.of(ware, event ->{
				ItemStack cursorItem = event.getCursor();
				ItemStack slotItem = event.getCurrentItem();
				
				if(!slotItem.isSimilar(emptyIcon)) {
					
					UtilPlayer.giveItems(player, slotItem, 1, true);
					if(cursorItem == null || cursorItem.getType().equals(Material.AIR)) {
						contents.setProperty("Ware", emptyIcon);
						this.reOpen(player, contents);
					}else {
						contents.setProperty("Ware", cursorItem);
						event.getView().setCursor(null);
						this.reOpen(player, contents);
					}
					
				}
				
				if(cursorItem == null || cursorItem.getType().equals(Material.AIR) || !slotItem.isSimilar(emptyIcon)) return;
				
				contents.setProperty("Ware", cursorItem);
				event.getView().setCursor(null);
				
				this.reOpen(player, contents);
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
			}));
			
			contents.set(SlotPos.of(0, 4), ClickableItem.of(new ItemBuilder(moneyHead.clone()).name("§eAuktionskosten: ")
					.lore("§f" + price + "$")
					.lore("")
					.lore("§7Der Preis berechnet sich aus einem")
					.lore("§7Basispreis von " + auctionBasePrice + "$")
					.lore("§7Und einem Preis pro Auktionsminute")
					.lore("§7von " + pricePerMinute + "$")
					.lore("")
					.lore("§7Bezahlbar: " + (Eco.has(player, price) ? "§aJa" : "§cNein"))
					.lore("")
					.lore("§6Klicke zum starten der Auktion")
					.build(), event ->{
						
						int timeInMin = min + (60 * h) + (60 * 24 * d) + (60 * 24 * 7 * w);
						
						int maxMin = Integer.parseInt(com.avarioncraft.shopsystem.utils.UtilPlayer.getMeta(player.getUniqueId(), "MaxAuctionMinutes", "1"));
						
						if(maxMin < min) {
							player.sendMessage(F.error("Deine längste Auktion kann maximal §e" + UtilTime.diffBetween(LocalDateTime.now(), LocalDateTime.now().plusMinutes(timeInMin)) + "§7 lang sein."));
							this.reOpen(player, contents);
							return;
						}
						
						int maxAuctions = Integer.parseInt(com.avarioncraft.shopsystem.utils.UtilPlayer.getMeta(player.getUniqueId(), "MaxAuctions", "0"));
						int currentAuctions = auctionHouse.getAccountManager().getAccountOf(player.getUniqueId()).get().getOwnedAuctions().size();
						
						if(currentAuctions >= maxAuctions) {
							player.sendMessage(F.error("Du hast bereits §e" + maxAuctions + "§7 von §e" + maxAuctions + "§7 Auktionen laufen."));
							this.reOpen(player, contents);
							return;
						}
						
						if(Eco.has(player, price)) {
							if(!ware.isSimilar(emptyIcon)) {
								Eco.withdraw(player, price);
								auctionHouse.startAuction(player, ware, timeInMin, auctionBetPrice);
								auctionHouse.openAuctionFor(player);
								player.sendMessage(F.main("Auktionen", "Du hast jetzt §e" + (currentAuctions + 1) + "§7 von §e" + maxAuctions + "§7 Auktionen laufen."));
								UtilPlayer.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.35F);
								return;
							}else {
								player.sendMessage(F.error("Du musst eine Ware auswählen."));
								this.reOpen(player, contents);
							}
							
						}else {
							player.sendMessage(F.error("Du hast nicht genug Geld, um das zu bezahlen."));
							this.reOpen(player, contents);
						}
						
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
					}));
			
			contents.set(SlotPos.of(1, 7), new InputButton(new ItemBuilder(goldHead.clone())
					.name("§eStartgebot:")
					.lore("§f" + auctionBetPrice + "$")
					.lore("")
					.lore("§6Klicke zum ändern")
					.build(), "" + auctionBetPrice, input -> {
						if(UtilMath.isDouble(input)) {
							contents.setProperty("AuctionBetPrice", Double.parseDouble(input));
						}else {
							player.sendMessage(F.error("Du kannst nur eine Kommazahl als Startgebot setzen."));
						}
						this.reOpen(player, contents);
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
			}));
			
			
			contents.set(SlotPos.of(1, 4), ClickableItem.of(new ItemBuilder(greenClock.clone())
					.name("§eMinuten: §f" + min)
					.lore("")
					.lore("§a+ §6< Links | Rechts >§c -")
					.amount((min == 0 ? 1 : min))
					.build(), event ->{
						if(event.isLeftClick()) {
							if(min < 59) {
								contents.setProperty("min", min + 1);
							}
						}else {
							if(min > 0) {
								contents.setProperty("min", min - 1);
							}
						}
						this.reOpen(player, contents);
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
			}));
			
			contents.set(SlotPos.of(2, 4), ClickableItem.of(new ItemBuilder(blueClock.clone())
					.name("§eStunden: §f" + h)
					.lore("")
					.lore("§a+ §6< Links | Rechts >§c -")
					.amount((h == 0 ? 1 : h))
					.build(), event ->{
						if(event.isLeftClick()) {
							if(h < 23) {
								contents.setProperty("h", h + 1);
							}
						}else {
							if(h > 0) {
								contents.setProperty("h", h - 1);
							}
						}
						this.reOpen(player, contents);
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
			}));
			
			contents.set(SlotPos.of(3, 4), ClickableItem.of(new ItemBuilder(purpleClock.clone())
					.name("§eTage: §f" + d)
					.lore("")
					.lore("§a+ §6< Links | Rechts >§c -")
					.amount((d == 0 ? 1 : d))
					.build(), event ->{
						if(event.isLeftClick()) {
							if(d < 6) {
								contents.setProperty("d", d + 1);
							}
						}else {
							if(d > 0) {
								contents.setProperty("d", d - 1);
							}
						}
						this.reOpen(player, contents);
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
			}));
			
			contents.set(SlotPos.of(4, 4), ClickableItem.of(new ItemBuilder(redClock.clone())
					.name("§eWochen: §f" + w)
					.lore("")
					.lore("§a+ §6< Links | Rechts >§c -")
					.amount((w == 0 ? 1 : w))
					.build(), event ->{
						if(event.isLeftClick()) {
							contents.setProperty("w", w + 1);
						}else {
							if(w > 0) {
								contents.setProperty("w", w - 1);
							}
						}
						this.reOpen(player, contents);
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
			}));
			
		}
		
	};
	
	private InventoryProvider orderChooser = new InventoryProvider() {

		@Override
		public void init(Player player, InventoryContents contents) {
			
			for(AuctionSortOrder order : AuctionSortOrder.values()) {
				
				contents.add(order.getChooser(player, auctionHouse.getAuctionGUI()));
				
			}
			
			contents.set(SlotPos.of(1, 8), ClickableItem.of(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("BaseMonitor")).name("§eSuchen...").build(), event ->{
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
				new AnvilGUI(player, "Eingabe", (pl, input) -> {
					
					ShopCore.newChain()
					.asyncFirst(() -> ShopCore.getInstance().getDatabaseManager().auctionSearch(input))
					.abortIfNull()
					.syncLast((auctions) -> {
						
						SmartInventory.builder()
						.size(5)
						.provider(mirror)
						.title("Auktionshaus")
						.build()
						.openFor(player)
						.data("Order", AuctionSortOrder.NONE)
						.data("Auctions", auctions)
						.open();
						
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
					})
					.execute();
					
					return null;
				});
			}));
			
		}
		
	};
	
	@AllArgsConstructor
	public enum AuctionSortOrder{
		
		TIME_ASC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("GreenClock")).name("§eZeit aufsteigend").build(), "Zeit aufsteigend", "runout", "ASC"), 
		TIME_DESC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("RedClock")).name("§eZeit absteigend").build(), "Zeit absteigend", "runout", "DESC"), 
		ALPHABETICAL_ASC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("Oak_A")).name("§eAlphabetisch aufsteigend").build(), "Alphabetisch aufsteigend", "itemname", "ASC"),
		ALPHABETICAL_DESC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("Oak_Z")).name("§eAlphabetisch absteigend").build(), "Alphabetisch absteigend", "itemname", "DESC"),
		PRICE_ASC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("GoldArrowUp")).name("§ePreis aufsteigend").build(), "Preis aufsteigend", "currentprice", "ASC"),
		PRICE_DESC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("GoldArrowDown")).name("§ePreis absteigend").build(), "Preis absteigend", "currentprice", "DESC"),
		AMOUNT_ASC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("OakArrowUp")).name("§eMenge aufsteigend").build(), "Menge aufsteigend", "amount", "ASC"),
		AMOUNT_DESC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("OakArrowDown")).name("§eMenge absteigend").build(), "Menge absteigend", "amount", "DESC"),
		OWNER_ASC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("StevePlushie")).name("§eBesitzer aufsteigend").build(), "Besitzer aufsteigend", "ownername", "ASC"),
		OWNER_DESC(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("HerobrinePlushie")).name("§eBesitzer absteigend").build(), "Besitzer absteigend", "ownername", "DESC"),
		NONE(new ItemBuilder(Material.BARRIER).name("§eKeine Sortierung").build(), "Keine Sortierung", "none", "DESC");
		
		private final ItemStack icon;
		
		@Getter
		private final String displayName;
		
		@Getter
		private final String key;
		@Getter
		private final String mode;
		
		public ClickableItem getChooser(Player player, AuctionHouseGUI auctionGUI) {
			return ClickableItem.of(this.icon, event ->{
				auctionGUI.openFor(player, this);
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
			});
		}
	}

}
