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
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.smartInv.content.Pagination;
import net.crytec.api.smartInv.content.SlotIterator;
import net.crytec.api.smartInv.content.SlotPos;
import net.crytec.api.smartInv.content.SlotIterator.Type;
import net.crytec.api.util.UtilPlayer;

public class AdminShopPurchaseList implements InventoryProvider{
	
	public AdminShopPurchaseList(){
		this.manager = ShopCore.getInstance().getAdminShopManager();
	}
	
	private final AdminShopManager manager;
	
	public static void open(Player player) {
		SmartInventory.builder().size(5).provider(new AdminShopPurchaseList()).title("Adminshop Liste").build().open(player);
	}
	
	@Override
	public void init(Player player, InventoryContents contents) {
		
		Pagination pagination = contents.pagination();
		ArrayList<ClickableItem> items = new ArrayList<ClickableItem>();
		
		this.manager.getShops().stream().filter(shop -> shop.isListed() && shop.isEnabled()).forEach(shop ->{
			items.add(ClickableItem.of(shop.getShopIcon(), event -> {
				UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
				AdminShopTransactionGUI.open(player, shop);
			}));
		});
		
		
		// Admin Editor Button
		if (player.hasPermission("shop.adminshop.edit")) {
		  contents.set(SlotPos.of(4, 4), ClickableItem.of(new ItemBuilder(Material.BOOKSHELF)
		      .name("§6Shop Editor")
		      .lore("")
		      .lore("§7Klicke hier um die Adminshops zu bearbeiten")
		      .lore("§7oder neue zu erstellen.")
		      .build(), e -> {
            AdminShopList.open(player);
            UtilPlayer.playSound(player, Sound.UI_BUTTON_CLICK, 0.7F, 0.95F);
		  }));
		}
		
		pagination.setItems(items.toArray(new ClickableItem[items.size()]));
		pagination.setItemsPerPage(27);
		
		SlotIterator slotIterator = contents.newIterator(Type.HORIZONTAL, 0, 0);
		slotIterator = slotIterator.allowOverride(false);
		pagination.addToIterator(slotIterator);
		
	}
	
}
