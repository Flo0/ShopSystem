package com.avarioncraft.shopsystem.adminshops;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.util.RayTraceResult;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.blocklinks.PersistentAdminShop;
import com.avarioncraft.shopsystem.manager.AdminShopManager;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.tradesystem.PermissionWare;
import com.avarioncraft.shopsystem.utils.TimeInput;
import com.google.common.collect.Lists;

import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.smartInv.ClickableItem;
import net.crytec.api.smartInv.SmartInventory;
import net.crytec.api.smartInv.buttons.InputButton;
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.smartInv.content.SlotPos;
import net.crytec.api.smartInv.inventories.ConfirmationGUI;
import net.crytec.api.util.F;
import net.crytec.api.util.UtilMath;
import net.crytec.api.util.UtilPlayer;
import net.crytec.api.util.UtilTime;
import net.crytec.api.util.language.LanguageHelper;
import net.crytec.shaded.org.apache.lang3.ArrayUtils;

public class AdminShopBuilder implements InventoryProvider{
	
	public static void open(Player player, AdminShop shop) {
		SmartInventory.builder().size(5).provider(new AdminShopBuilder(shop)).title("Adminshop Editor").build().open(player);
	}
	
	private AdminShopBuilder(AdminShop shop) {
		this.shop = shop;
		this.adminShopManager = ShopCore.getInstance().getAdminShopManager();
	}
	
	private final AdminShop shop;
	private final AdminShopManager adminShopManager;
	
	@Override
	public void init(Player player, InventoryContents contents) {
		
		contents.add(new InputButton(new ItemBuilder(Material.NAME_TAG).name("§6Shop namen ändern")
				.lore("")
				.lore("§eMomentan: §b" + shop.getDisplayName())
				.build(), shop.getDisplayName(), result -> {
			shop.setDisplayName(result);
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		int currentAmount = shop.getTradeWare() != null ? shop.getTradeWare().getAmount() : 0;
		
		contents.add(new InputButton(new ItemBuilder(Material.CHEST).name("§6Momentanen Lagerbestand ändern")
				.lore("")
				.lore("§eMomentan: §6" + shop.getAmount() + " §e/§6 " + shop.getBaseAmount() + " §7Einheiten.")
				.lore("§eKaufbare Items: §6" + (currentAmount * (shop.getTradeWare() == null ? 0 : shop.getTradeWare().getAmount())) + "§e/§6" + (currentAmount * shop.getBaseAmount())  )
				.build(), "" + shop.getAmount(), result -> {
			if(UtilMath.isInt(result)) {
				shop.setAmount(Integer.parseInt(result));
			}else {
				player.sendMessage(F.error("Das muss eine Ganzzahl sein."));
			}
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		contents.add(new InputButton(new ItemBuilder(Material.ENDER_CHEST)
				.name("§6Basis Lagerbestand ändern §e" + "(" + shop.getBaseAmount() +" Einheiten)")
				.lore("")
				.lore("§7Auf diesen Wert wird das Lager nach")
				.lore("§7" + shop.getRefillIntervall() + "ms wieder aufgefüllt.")
				.lore("§7Entspricht ca " + (shop.getRefillIntervall() / 1000) / 3600 + "h")
				.build(), "" + shop.getBaseAmount(), result -> {
			if(UtilMath.isInt(result)) {
				shop.setBaseAmount(Integer.parseInt(result));
			}else {
				player.sendMessage(F.error("Das muss eine Ganzzahl sein."));
			}
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		contents.add(new InputButton(new ItemBuilder(Material.WRITABLE_BOOK).name("§6Basispreis ändern §e" + "(" + shop.getBasePrice() +")")
				.lore("")
				.lore("§7Der Basispreis vor der Anpassung an")
				.lore("§7die Marktlage. Kann ohne Probleme")
				.lore("§7jederzeit geändert werden.")
				.build(), "" + shop.getBasePrice(), result -> {
			if(UtilMath.isDouble(result)) {
				shop.setBasePrice(Double.parseDouble(result));
			}else {
				player.sendMessage(F.error("Das muss eine Kommazahl sein."));
			}
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		boolean limited = shop.isLimited();
		
		contents.add(new ClickableItem(new ItemBuilder(Material.ACACIA_TRAPDOOR).name("§6Shop limitieren")
				.lore("")
				.lore("§7Momentan Limitiert: " + (limited ? "§aJa" : "§cNein"))
				.build(), event -> {
			this.shop.setLimited(!limited);
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		contents.add(new InputButton(new ItemBuilder(Material.COMPASS).name("§6Nachfüllzeit ändern")
				.lore("")
				.lore("§eNächste Nachfüllung:")
				.lore("§7" + ((shop.getNextRefill() > 0) ? UtilTime.when(shop.getNextRefill()) : "Niemals"))
				.lore("")
				.lore("§eNachfüllintervall: §7" + this.shop.getRefillDisplay())
				.lore("")
				.lore("§eDie Zeit wird aus deiner Eingabe errechnet.")
				.lore("")
				.lore("§eBeispiele:")
				.lore("§710s2m -> sind 10 Sek und 2 Min")
				.lore("§74000s -> sind 4000 Sek")
				.lore("§72w4d -> sind 2 Wochen + 4 Tage")
				.lore("")
				.lore("§7Der Shop wird nach der Eingabe technisch")
				.lore("§7bedingt neu aufgefüllt.")
				.build(), "" + shop.getRefillDisplay(), result -> {
					
					shop.setRefillIntervall(TimeInput.parse(result));
					shop.setRefillDisplay(result);
					shop.refill();
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					this.reOpen(player, contents);
		}));
		
		contents.add(ClickableItem.of(new ItemBuilder(Material.CLOCK).name("§6Öffnungszeit ändern")
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
				
		contents.add(new ClickableItem(new ItemBuilder(Material.PAPER).name("§6Öffentlich gelistet")
				.lore("")
				.lore("§7Momentan gelistet: " + (shop.isListed() ? "§aJa" : "§cNein"))
				.build(), event -> {
			this.shop.setListed(!shop.isListed());
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		contents.add(new InputButton(new ItemBuilder(Material.FLOWER_POT).name("§6Verkaufsmultiplikator")
				.lore("")
				.lore("§7Der Verkaufsmultiplikator oder auch weight.")
				.lore("")
				.lore("§eMomentan: §7" + shop.getSellWeight())
				.build(), "" + shop.getSellWeight(), result -> {
					
					if(UtilMath.isDouble(result)) {
						shop.setSellWeight((Double.parseDouble(result)));
					}else {
						player.sendMessage(F.error("Das muss eine Kommazahl sein."));
					}
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					this.reOpen(player, contents);
		}));
		
		boolean enabled = shop.isEnabled();
		
		contents.set(SlotPos.of(4,8), ClickableItem.of(new ItemBuilder(enabled ? Material.GREEN_WOOL : Material.RED_WOOL)
				.name("§6Shop freischalten")
				.lore("§7")
				.lore("§7Der Shop ist momentan " + (enabled ? "§afreigeschaltet" : "§cnicht freigeschaltet"))
				.build(), event ->{
			this.shop.setEnabled(!enabled);
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		
		contents.set(SlotPos.of(2,4), ClickableItem.of(shop.getEditorIcon(), event ->{
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.openTradeWareGUI(player);
		}));
		
		contents.set(SlotPos.of(4,0), ClickableItem.of(new ItemBuilder(enabled ? Material.GREEN_WOOL : Material.RED_WOOL)
				.name("§6Shop freischalten")
				.lore("§7")
				.lore("§7Der Shop ist momentan " + (enabled ? "§afreigeschaltet" : "§cnicht freigeschaltet"))
				.build(), event ->{
			this.shop.setEnabled(!enabled);
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			this.reOpen(player, contents);
		}));
		
		
		contents.set(SlotPos.of(4,3), ClickableItem.of(new ItemBuilder(Material.GREEN_TERRACOTTA).name("§a✔ Änderungen speichern").build(), event ->{
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
            AdminShopList.open(player);
		}));
		
		contents.set(SlotPos.of(4, 4), ClickableItem.of(new ItemBuilder(ShopCore.getInstance().getHeadProvider().get("LaserOrb"))
				.name("§eShop linken")
				.lore("")
				.lore("§7Verlinkt den Adminshop mit einem")
				.lore("§7Entity, NPC oder Block.")
				.build(), event ->{
					
					RayTraceResult ray = player.getWorld().rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(), 8D, FluidCollisionMode.NEVER, false, 1D, e -> !e.getUniqueId().equals(player.getUniqueId()));
						if(ray == null) {
						player.sendMessage(F.error("Nichts gefunden (zu weit weg?)"));
						return;
					}
					
					Entity entity = ray.getHitEntity();
					
					if(entity == null) {
						
						Block block = ray.getHitBlock();
						
						if(block == null) {
							player.sendMessage(F.error("Kein Block oder Entity gefunden"));
						}else {
							player.sendMessage(F.main("AdminShops", "Block " + block.getType().toString() + "§7 wurde verlinkt."));
							
							((PersistentAdminShop) ShopCore.getInstance().getPerBlockManager().createBlock("AdminShop", block.getLocation(), block.getBlockData())).setShopID(shop.getShopID());
						}
						
					}else {
						
						if (entity instanceof Player) {
							Player p = (Player) entity;
							if (p.isOnline()) {
								player.sendMessage(F.error("Spieler können nicht verlinkt werden."));
								return;
							}
						}
						
						
						player.sendMessage(F.main("AdminShops", "Entity " + LanguageHelper.getEntityDisplayName(entity) + "§7 wurde verlinkt."));
						entity.getScoreboardTags().add("AdminShop_" + shop.getShopID().toString());
					}
					
				}));
		
		contents.set(SlotPos.of(4,5), ClickableItem.of(new ItemBuilder(Material.BARRIER).name("§cShop löschen").build(), event ->{
			UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
			ConfirmationGUI.open(player, "Wirklich löschen?", confirm->{
				if(confirm) {
					this.adminShopManager.remove(shop);
					AdminShopList.open(player);
				}else {
					this.reOpen(player, contents);
				}
			});
		}));
		
	}
	
	private void openTimeChooser(Player player) {
		
		SmartInventory.builder().size(3).title("Öffnungszeiten").provider(new InventoryProvider() {

			@Override
			public void init(Player player, InventoryContents contents) {
				
				ArrayList<Integer> hours = Lists.newArrayList();
				
				IntStream.rangeClosed(1, 24).forEach(i ->{
					contents.set((i - 1), ClickableItem.of(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("§6" + i + ":00 §6Uhr").build(), event ->{
						hours.add(Integer.valueOf(i));
						shop.getTimeRange().implementRanges(ArrayUtils.toPrimitive(hours.toArray(new Integer[hours.size()])));
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
						this.reOpen(player, contents);
					}));
					
				});
				
				for(int hour : shop.getTimeRange().getHoursArray()) {
					hours.add(Integer.valueOf(hour));
					contents.set((hour - 1), ClickableItem.of(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).name("§6" + hour + ":00 §6Uhr").build(), event ->{
						hours.remove(Integer.valueOf(hour));
						shop.getTimeRange().implementRanges(ArrayUtils.toPrimitive(hours.toArray(new Integer[hours.size()])));
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
						this.reOpen(player, contents);
					}));
				}
				
				contents.set(SlotPos.of(2,8), ClickableItem.of(new ItemBuilder(Material.BARRIER).name("§a✔").build(), event ->{
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					AdminShopBuilder.open(player, shop);
				}));
			}
			
		}).build().open(player);
		
	}
	
	private void openTradeWareGUI(Player player) {
		
		SmartInventory.builder().size(1).title("Ware wählen").provider(new InventoryProvider() {
			
			@Override
			public void init(Player player, InventoryContents contents) {
				contents.add(ClickableItem.of(new ItemBuilder(Material.EMERALD).name("§7Item-Ware").build(), event ->{
					ShopCore.getInstance().getItemChooserManager().openChooser(player, item ->{
						if(item == null || item.getType().equals(Material.AIR)) {
							player.sendMessage(F.error("Das Feld kann nicht leer sein..."));
						}else {
							shop.setTradeWare(new ItemWare(item));
						}
						UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
						AdminShopBuilder.open(player, shop);
					}, false);
				}));
				contents.add(new InputButton(new ItemBuilder(Material.COMMAND_BLOCK).name("§7Permission-Ware")
						.enchantment(Enchantment.ARROW_DAMAGE)
						.setItemFlag(ItemFlag.HIDE_ENCHANTS)
						.build(), "Permission.beispiel", result -> {
					shop.setTradeWare(new PermissionWare(result));
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					AdminShopBuilder.open(player, shop);
				}));
				contents.set(SlotPos.of(0,8), ClickableItem.of(new ItemBuilder(Material.BARRIER).name("§a✔").build(), event ->{
					UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
					AdminShopBuilder.open(player, shop);
				}));
			}
			
		}).build().open(player);
		
	}

}
