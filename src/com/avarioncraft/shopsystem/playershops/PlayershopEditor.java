package com.avarioncraft.shopsystem.playershops;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.utils.Eco;
import com.google.common.collect.Lists;

import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.smartInv.ClickableItem;
import net.crytec.api.smartInv.SmartInventory;
import net.crytec.api.smartInv.buttons.InputButton;
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.smartInv.content.SlotPos;
import net.crytec.api.util.F;
import net.crytec.api.util.UtilMath;
import net.crytec.api.util.UtilPlayer;
import net.crytec.api.util.anvilgui.AnvilGUI;
import net.crytec.shaded.org.apache.lang3.ArrayUtils;

public class PlayershopEditor implements InventoryProvider{
	
	public PlayershopEditor(PlayerShop shop) {
		this.shop = shop;
		this.host = SmartInventory.builder().size(4).provider(this).title("Shop - " + shop.getDisplayName()).build();
		this.plugin = ShopCore.getInstance();
	}
	
	public void openForEdit(Player player) {
		this.host.open(player);
	}
	
	private final PlayerShop shop;
	private SmartInventory host;
	private final ShopCore plugin;
	
	@Override
	public void init(Player player, InventoryContents contents) {
		
		contents.fill(ClickableItem.empty(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build()));
		
		contents.set(SlotPos.of(0, 2), new InputButton(new ItemBuilder(Material.GOLD_INGOT)
				.name("§6Verkaufspreis ändern")
				.lore("")
				.lore("§eMomentan: §6" + shop.getBuyPrice())
				.lore("")
				.lore("§7Setzt den Preis, mit dem dein Shop")
				.lore("§7ware verkauft.")
				.build(), "" + shop.getBuyPrice(), input -> {
			
					if(UtilMath.isDouble(input)) {
						shop.setBuyPrice(Double.valueOf(input));
					}else {
						player.sendMessage(F.main("SpielerShop", "Die Eingabe muss eine Kommazahl sein."));
					}
					
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					this.reOpen(player, contents);
		}));
		
		contents.set(SlotPos.of(0, 3),new InputButton(new ItemBuilder(Material.IRON_INGOT)
				.name("§6Kaufpreis ändern")
				.lore("")
				.lore("§eMomentan: §6" + shop.getSellPrice())
				.lore("")
				.lore("§7Setzt den Preis, mit dem dein Shop")
				.lore("§7ware kauft.")
				.build(), "" + shop.getSellPrice(), input -> {
			
					if(UtilMath.isDouble(input)) {
						shop.setSellPrice(Double.valueOf(input));
					}else {
						player.sendMessage(F.main("SpielerShop", "Die Eingabe muss eine Kommazahl sein."));
					}
					
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					this.reOpen(player, contents);
		}));
		
		ItemBuilder policyBuilder = new ItemBuilder(Material.BARRIER);
		
		if(shop.isBuyingShop()) {
			if(shop.isSellingShop()) {
				policyBuilder.type(Material.JUNGLE_BOAT);
			}else {
				policyBuilder.type(Material.OAK_BOAT);
			}
		}else {
			if(shop.isSellingShop()) {
				policyBuilder.type(Material.BIRCH_BOAT);
			}else {
				policyBuilder.type(Material.ACACIA_BOAT);
			}
		}
		
		policyBuilder.name("§6Shop politik")
		.lore("")
		.lore("§eKauf: " + (shop.isBuyingShop() ? "§aJa" : "§cNein"))
		.lore("§eVerkauf: " + (shop.isSellingShop() ? "§aJa" : "§cNein"));
		
		contents.set(SlotPos.of(0, 4),new ClickableItem(policyBuilder.build(), event -> {
			
			if(shop.isBuyingShop()) {
				if(shop.isSellingShop()) {
					shop.setBuyingShop(false);
				}else {
					shop.setBuyingShop(false);
				}
			}else {
				if(shop.isSellingShop()) {
					shop.setBuyingShop(true);
					shop.setSellingShop(false);
				}else {
					shop.setBuyingShop(true);
					shop.setSellingShop(true);
				}
			}
			
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		contents.set(SlotPos.of(0, 5),ClickableItem.of(new ItemBuilder(Material.CLOCK).name("§6Öffnungszeit ändern")
				.lore("")
				.lore("§eRechtsklick, um Zeittyp zu ändern.")
				.lore("")
				.lore("§eZeittyp: §6" + (shop.isRealTime() ? "Echtzeit" : "Minecraftzeit"))
				.lore("")
				.lore("§eMomentan: " + shop.getTimeRange().getValidTimes().asRanges()
						.stream()
						.map(e -> "§e" + e.lowerEndpoint() + ":00 §7-§e " + e.upperEndpoint() + ":00§7")
						.collect(Collectors.joining(", ")))
				.build(), event -> {
					
					if(event.isRightClick()) {
						shop.setRealTime(!shop.isRealTime());
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
						this.reOpen(player, contents);
						return;
					}
					
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					this.openTimeChooser(player);
		}));
		
		contents.set(SlotPos.of(0, 6),ClickableItem.of(new ItemBuilder(Material.MAP).name("§6Grafische Statistik")
				.lore("")
				.lore("§7Gibt dir einen Graphen, der dir live eine")
				.lore("§7grafische Statistik über diesen Shop anzeigt.")
				.build(), event -> {
					
					if(this.shop.isActiveMap()) return;
					this.shop.setActiveMap(true);
					player.getInventory().addItem(this.shop.getGraphMap());
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						plugin.getItemSafetyManager().clearInventory(player.getInventory());
						this.shop.setActiveMap(false);
					}, 160L);
					this.reOpen(player, contents);
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
		}));
		
		ItemBuilder tradeBuilder = new ItemBuilder(Material.BEDROCK).name("§cKeine Ware vorhanden").lore("").lore("§6Linksklicke zum ändern");
		ItemWare ware = shop.getTradeWare();
		
		if(ware != null) {
			tradeBuilder = new ItemBuilder(ware.value.clone()).lore("").lore("§6Linksklicke mit einem Item zum ändern");
		}
		
		contents.set(SlotPos.of(2, 4), new ClickableItem(tradeBuilder.build(), event -> {
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			ItemStack cursor = event.getCursor();
			if(cursor == null || cursor.getType().equals(Material.AIR)) {
				return;
			}
			shop.setTradeWare(new ItemWare(cursor.asOne()));
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			com.avarioncraft.shopsystem.utils.UtilPlayer.giveItems(event.getView().getPlayer().getInventory(), cursor, 1, true, event.getView().getPlayer().getLocation());
			event.getView().setCursor(null);
			this.reOpen(player, contents);
			
		}));
		
		contents.set(SlotPos.of(3, 0), new ClickableItem(new ItemBuilder(Material.GOLD_BLOCK)
				.name("§6Kapital")
				.lore("")
				.lore("§eMomentan: §6" + shop.getBank() + " §e$")
				.lore("")
				.lore("§eMiete: §6" + shop.getRentCost() + " $ §e/§6 Tag §7(Echtzeit)")
				.lore("")
				.lore("§7Hier wird das Geld von deinen")
				.lore("§7Transaktionen gespeichert.")
				.lore("")
				.lore("§7Außerdem bezahlst du deine Miete")
				.lore("§7mit diesem Kapital.")
				.lore("")
				.lore("§6Linksklicke zum einzahlen")
				.lore("§6Rechtsklicke zum abheben")
				.build(), event -> {
					
					if(event.isLeftClick()) {
						new AnvilGUI(plugin, player, "" + Eco.getEconomy().getBalance(player), (pl, result) -> {
							
							if(UtilMath.isDouble(result)) {
								
								double amount = Double.parseDouble(result);
								if(Eco.has(player, amount)) {
									shop.depositStoredMoney(player, amount);
								}else {
									player.sendMessage(F.error("So viel Geld hast du nicht."));
								}
								
							}else {
								player.sendMessage(F.error("Du kannst nur eine Kommazahl einzahlen."));
							}
							
							Bukkit.getScheduler().runTaskLater(plugin, () -> this.reOpen(player, contents), 1);
							return null;
						});
					}else {
						new AnvilGUI(plugin, player, "" + shop.getBank(), (pl, result) -> {
							
							if(UtilMath.isDouble(result)) {
								double amount = Double.parseDouble(result);
								if(amount <= shop.getBank()) {
									shop.withdrawStoredMoney(player, amount);
								}else {
									player.sendMessage(F.error("So viel Geld ist nicht im shop."));
								}
								
								
							}else {
								player.sendMessage(F.error("Du kannst nur eine Kommazahl abheben."));
							}
							
							Bukkit.getScheduler().runTaskLater(plugin, () -> this.reOpen(player, contents), 1);
							return null;
						});
					}
					
					
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
		}));
		
		contents.set(SlotPos.of(3, 8), new ClickableItem(new ItemBuilder(Material.CHEST)
				.name("§6Zum Shopinventar")
				.build(), event -> {
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
				shop.openInventory(player);
		}));
	}
	
	private void openTimeChooser(Player player) {
		
		SmartInventory.builder().size(3).title("Öffnungszeiten").provider(new InventoryProvider() {

			@Override
			public void init(Player player, InventoryContents contents) {
				
				ArrayList<Integer> hours = Lists.newArrayList();
				
				IntStream.rangeClosed(1, 24).forEach(i ->{
					contents.set((i - 1), ClickableItem.of(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("§f" + i + ":00 §6Uhr").build(), event ->{
						hours.add(Integer.valueOf(i));
						shop.getTimeRange().implementRanges(ArrayUtils.toPrimitive(hours.toArray(new Integer[hours.size()])));
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
						this.reOpen(player, contents);
					}));
					
				});
				
				for(int hour : shop.getTimeRange().getHoursArray()) {
					hours.add(Integer.valueOf(hour));
					contents.set((hour - 1), ClickableItem.of(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).name("§f" + hour + ":00 §6Uhr").build(), event ->{
						hours.remove(Integer.valueOf(hour));
						shop.getTimeRange().implementRanges(ArrayUtils.toPrimitive(hours.toArray(new Integer[hours.size()])));
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
						this.reOpen(player, contents);
					}));
				}
				
				contents.set(SlotPos.of(2,8), ClickableItem.of(new ItemBuilder(Material.BARRIER).name("§a✔").build(), event ->{
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					openForEdit(player);
				}));
			}
			
		}).build().open(player);
		
	}

}
