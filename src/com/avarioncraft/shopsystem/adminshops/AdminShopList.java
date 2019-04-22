package com.avarioncraft.shopsystem.adminshops;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.manager.AdminShopManager;

import net.crytec.api.itemstack.ItemBuilder;
import net.crytec.api.smartInv.ClickableItem;
import net.crytec.api.smartInv.SmartInventory;
import net.crytec.api.smartInv.buttons.InputButton;
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.smartInv.content.Pagination;
import net.crytec.api.smartInv.content.SlotIterator;
import net.crytec.api.smartInv.content.SlotIterator.Type;
import net.crytec.api.smartInv.content.SlotPos;
import net.crytec.api.util.UtilPlayer;

public class AdminShopList implements InventoryProvider{
	
	public AdminShopList(){
		this.manager = ShopCore.getInstance().getAdminShopManager();
	}
	
	private final AdminShopManager manager;
	
	public static void open(Player player) {
		SmartInventory.builder().size(5).provider(new AdminShopList()).title("Adminshop Liste").build().open(player);
	}
	
	@Override
	public void init(Player player, InventoryContents contents) {
		
		Pagination pagination = contents.pagination();
		ArrayList<ClickableItem> items = new ArrayList<ClickableItem>();
		
		contents.fillRow(4, ClickableItem.empty(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build()));
		
		this.manager.getShops().forEach(shop ->{
			items.add(ClickableItem.of(shop.getListIcon(), event -> {
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
				AdminShopBuilder.open(player, shop);
			}));
		});
		
		contents.set(SlotPos.of(4, 4), new InputButton(new ItemBuilder(Material.EMERALD).name("§7Neuen Shop erstellen").build(), "Name" , input -> {
			AdminShop shop = manager.createShop();
			shop.setDisplayName(input);
			AdminShopBuilder.open(player, shop);
		}));
		
	      contents.set(SlotPos.of(4, 0), ClickableItem.of(new ItemBuilder(Material.BARRIER).name("§7Zurück zur Übersicht").build(), e -> {
	        AdminShopPurchaseList.open(player);
	        UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 0.95F);
        }));
		
		pagination.setItems(items.toArray(new ClickableItem[items.size()]));
		pagination.setItemsPerPage(27);
		
		SlotIterator slotIterator = contents.newIterator(Type.HORIZONTAL, 0, 0);
		slotIterator = slotIterator.allowOverride(false);
		pagination.addToIterator(slotIterator);
		
	}
	
}
