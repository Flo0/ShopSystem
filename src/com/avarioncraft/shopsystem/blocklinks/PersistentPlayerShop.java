package com.avarioncraft.shopsystem.blocklinks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.playershops.PlayerShop;
import com.avarioncraft.shopsystem.tradesystem.BuyRequestResponse;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.tradesystem.SellRequestResponse;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.handler.TouchHandler;
import com.gmail.filoghost.holographicdisplays.api.line.ItemLine;
import com.gmail.filoghost.holographicdisplays.api.line.TouchableLine;
import com.google.common.collect.Lists;

import lombok.Getter;
import net.crytec.api.persistentblocks.HologramLineFiller;
import net.crytec.api.persistentblocks.ItemFiller;
import net.crytec.api.persistentblocks.PersistentBlock;
import net.crytec.api.persistentblocks.TextFiller;
import net.crytec.api.persistentblocks.blocks.HolographicBlock;
import net.crytec.api.persistentblocks.blocks.InteractableBlock;
import net.crytec.api.persistentblocks.blocks.TickableBlock;
import net.crytec.api.smartInv.ClickableItem;
import net.crytec.api.smartInv.SmartInventory;
import net.crytec.api.smartInv.content.InventoryContents;
import net.crytec.api.smartInv.content.InventoryProvider;
import net.crytec.api.util.language.LanguageHelper;

public class PersistentPlayerShop extends PersistentBlock implements InteractableBlock, HolographicBlock, TickableBlock  {

	public PersistentPlayerShop() {
		this.holoLines = Lists.newArrayList();
		this.stateLinked = false;
	}
	
	@Getter
	private UUID shopID;
	@Getter
	private UUID ownerID;
	@Getter
	private PlayerShop shop;
	
	private Hologram hologram;
	
	public void setShop(PlayerShop shop) {
		this.shop = shop;
		this.ownerID = shop.getOwnerID();
		this.shopID = shop.getShopID();
	}
	
	@Override
	public void onInteract(PlayerInteractEvent event) {
		event.setCancelled(true);
		Player player = event.getPlayer();
		
		if (player.getGameMode() == GameMode.CREATIVE) {
			event.setCancelled(true);
		}
		
		if (player.getUniqueId().equals(this.ownerID)) {
			if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				this.shop.openForEdit(player);
			}else if(event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				event.setCancelled(false);
			}
			return;
		}
		
		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			BuyRequestResponse response = shop.playerBuyRequest(player, (player.isSneaking() ? 64 : 1));
			player.sendMessage(response.getMessage()
					.replace("<amount>", "" + (player.isSneaking() ? 64 : 1))
					.replace("<ware>",  LanguageHelper.getItemDisplayName(shop.getTradeWare().value))
					.replace("<money>", "" + shop.getBuyPrice() * (player.isSneaking() ? 64 : 1))
					);
		} else {
			SellRequestResponse response = shop.playerSellRequest(player, (player.isSneaking() ? 64 : 1));
			player.sendMessage(response.getMessage()
					.replace("<amount>", "" + (player.isSneaking() ? 64 : 1))
					.replace("<ware>",  LanguageHelper.getItemDisplayName(shop.getTradeWare().value))
					.replace("<money>", "" + shop.getSellPrice() * (player.isSneaking() ? 64 : 1))
					);
		}
	}
	
	Supplier<ItemStack> itemSupp = () ->{
		ItemWare ware = shop.getTradeWare();
		return ware == null ? new ItemStack(Material.BARRIER) : ware.value;
	};
	
	@Override
	protected void loadData(ConfigurationSection config) {
		this.shopID = UUID.fromString(config.getString("shopID"));
		this.ownerID = UUID.fromString(config.getString("ownerID"));
		this.shop = ShopCore.getInstance().getPlayerShopManager().getPlayerShops().get(ownerID, shopID);
		if(shop == null) {
			ShopCore.getInstance().getPerBlockManager().removePersistentBlock(this);
			return;
		}
		this.shop.setPhysicalShop(this);
		this.setupLines();
	}
	
	protected void onBreak(BlockBreakEvent event) {
		
	}

	@Override
	protected void onRemove() {
		ShopCore.getInstance().getPlayerShopManager().deleteShop(this.ownerID, shopID, false);
		if(this.location.getBlock().getState().getType().equals(Material.CHEST)) {
			this.location.getBlock().setType(Material.AIR);
		}
	}

	@Override
	protected void saveData(ConfigurationSection config) {
		config.set("ownerID", this.ownerID.toString());
		config.set("shopID", this.shopID.toString());
	}

	@Override
	public Hologram getHologram() {
		return this.hologram;
	}
	
	private final ArrayList<HologramLineFiller<?, ? extends TouchableLine>> holoLines;
	
	public void setupLines() {
		
		ArrayList<Supplier<String>> shopLines = this.shop.getShopInfo(this.getLocation());
		
		ItemLine iLine = this.hologram.appendItemLine(itemSupp.get());
		
		iLine.setTouchHandler(new TouchHandler() {
			
			@Override
			public void onTouch(Player player) {
				
				SmartInventory.builder()
				.type(InventoryType.DISPENSER)
				.title("§6Shop Ware")
				.provider(new InventoryProvider() {

					@Override
					public void init(Player player, InventoryContents contents) {
						
						contents.set(4, ClickableItem.empty(shop.getTradeWare().value.clone()));
						
					}
					
				})
				.build()
				.open(player);
				
			}
			
		});
		
		this.holoLines.add(new ItemFiller(itemSupp, iLine));
		
		shopLines.forEach(info ->{
			this.holoLines.add(new TextFiller(info, this.hologram.appendTextLine(info.get())));
		});
	}
	
	@Override
	public ArrayList<HologramLineFiller<?, ? extends TouchableLine>> getLines() {
		return this.holoLines;
	}

	@Override
	public int updateTime() {
		return 2;
	}

	@Override
	protected void postInit() {
		
		this.hologram = HologramsAPI.createHologram(ShopCore.getInstance(), super.location.clone().add(0.5, 3.5, 0.5));
		
	}

	@Override
	public int getTicks() {
		return 40;
	}

	@Override
	public void onTick() {
		if(!this.location.getBlock().getState().getBlockData().getAsString().equals(this.blockData)) {
			ShopCore plugin = ShopCore.getInstance();
			plugin.getPlayerShopManager().deleteShop(this.ownerID, this.shopID, false);
			Bukkit.getScheduler().runTaskLater(plugin, () -> this.delete(), 1) ;
		}
	}
}