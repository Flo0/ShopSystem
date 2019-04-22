package com.avarioncraft.shopsystem.adminshops;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.avarioncraft.shopsystem.adminshops.AdminShop.BuySell;
import com.avarioncraft.shopsystem.tradesystem.BuyRequestResponse;
import com.avarioncraft.shopsystem.tradesystem.SellRequestResponse;
import com.avarioncraft.shopsystem.utils.Eco;

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

public class AdminShopTransactionGUI implements InventoryProvider{
	
	public static void open(Player player, AdminShop shop) {
		SmartInventory.builder().size(3).title(shop.getDisplayName()).provider(new AdminShopTransactionGUI(shop)).build().open(player);
	}
	
	private AdminShopTransactionGUI(AdminShop shop) {
		this.shop = shop;
	}
	
	private final AdminShop shop;
	
	@Override
	public void init(Player player, InventoryContents contents) {
		
		double kaufPreis = this.shop.evaluatePrice(BuySell.BUY);
		double verkaufPreis = this.shop.evaluatePrice(BuySell.SELL);
		
		//Kauf icons
		
		if (shop.isListed()) {
			contents.set(SlotPos.of(0, 0), ClickableItem.of(new ItemBuilder(Material.BARRIER).name("§7Zurück").build(), e -> {
				AdminShopPurchaseList.open(player);
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK);
			}));
		}
		
		if(shop.isSelling()) {
			contents.set(SlotPos.of(1,3), ClickableItem.of(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
					.name("§61x §7Kaufen")
					.lore("")
					.lore("§eKosten: §6" + kaufPreis + " " + Eco.getEconomy().currencyNamePlural())
					.build(), event->{
						
						BuyRequestResponse response = this.shop.playerBuyRequest(player, 1);
						
						if(response.isSuccessfull()) {
							player.sendMessage(F.main("AdminShop", response.getMessage()
									.replace("<amount>", "" + 1)
									.replace("<ware>", shop.getTradeWare().getWareDisplayName())
									.replace("<money>", "" + kaufPreis + " " + Eco.getEconomy().currencyNamePlural())));
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.35F);
						}else {
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
							player.sendMessage(F.error(response.getMessage()));
						}
						
						this.reOpen(player, contents);
						
			}));
			contents.set(SlotPos.of(1,2), new InputButton(new ItemBuilder(Material.GREEN_STAINED_GLASS)
					.name("§6Beliebige Anzahl kaufen")
					.lore("")
					.lore("§eKosten: §6" + kaufPreis + " " + Eco.getEconomy().currencyNamePlural())
					.build(), "" + 1, result->{
						
						int amount = 1;
						if(UtilMath.isInt(result)) {
							amount = Integer.parseInt(result);
						}else {
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
							player.sendMessage(F.error("Du kannst nur ganzzahlig kaufen."));
							return;
						}
						
						BuyRequestResponse response = this.shop.playerBuyRequest(player, amount);
						if(response.isSuccessfull()) {
							player.sendMessage(F.main("AdminShop", response.getMessage()
									.replace("<amount>", "" + amount)
									.replace("<ware>", shop.getTradeWare().getWareDisplayName())
									.replace("<money>", "" + (kaufPreis * amount) + " " + Eco.getEconomy().currencyNamePlural())));
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.35F);
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.25F);
						}else {
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
							player.sendMessage(F.error(response.getMessage()));
						}
						
						this.reOpen(player, contents);
			}));
		}
		
		// Verkauf icons
		
		if(shop.isBuying()) {
			contents.set(SlotPos.of(1,5), ClickableItem.of(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
					.name("§61x §7Verkaufen")
					.lore("")
					.lore("§eEinkommen: §6" + verkaufPreis + " " + Eco.getEconomy().currencyNamePlural())
					.build(), event->{
						
						SellRequestResponse response = this.shop.playerSellRequest(player, 1);
						if(response.isSuccessfull()) {
							player.sendMessage(F.main("AdminShop", response.getMessage()
									.replace("<amount>", "" + 1)
									.replace("<ware>", shop.getTradeWare().getWareDisplayName())
									.replace("<money>", "" + verkaufPreis + " " + Eco.getEconomy().currencyNamePlural())));
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.35F);
						}else {
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
							player.sendMessage(F.error(response.getMessage()));
						}
						
						this.reOpen(player, contents);
						
			}));
			contents.set(SlotPos.of(1,6), new InputButton(new ItemBuilder(Material.RED_STAINED_GLASS)
					.name("§6Beliebige Anzahl verkaufen")
					.lore("")
					.lore("§eEinkommen: §6" + verkaufPreis + " " + Eco.getEconomy().currencyNamePlural())
					.build(), "" + 1, result->{
						
						int amount = 1;
						if(UtilMath.isInt(result)) {
							amount = Integer.parseInt(result);
						}else {
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
							player.sendMessage(F.error("Du kannst nur ganzzahlig verkaufen."));
							return;
						}
						
						SellRequestResponse response = this.shop.playerSellRequest(player, amount);
						if(response.isSuccessfull()) {
							player.sendMessage(F.main("AdminShop", response.getMessage()
									.replace("<amount>", "" + amount)
									.replace("<ware>", shop.getTradeWare().getWareDisplayName())
									.replace("<money>", "" + (verkaufPreis * amount) + " " + Eco.getEconomy().currencyNamePlural())));
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.35F);
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.25F);
						}else {
							UtilPlayer.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 0.75F);
							player.sendMessage(F.error(response.getMessage()));
						}
						this.reOpen(player, contents);
			}));
		}
		
		// Item
		
		contents.set(SlotPos.of(1, 4), ClickableItem.empty(shop.getShopIcon()));
		
	}

}
