package com.avarioncraft.shopsystem.commands;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.manager.PlayerShopManager;
import com.avarioncraft.shopsystem.playershops.PlayerShop;

import net.crytec.API;
import net.crytec.acf.BaseCommand;
import net.crytec.acf.CommandIssuer;
import net.crytec.acf.annotation.CommandAlias;
import net.crytec.acf.annotation.CommandPermission;
import net.crytec.acf.annotation.Default;
import net.crytec.acf.annotation.Private;
import net.crytec.acf.annotation.Subcommand;
import net.crytec.acf.annotation.Syntax;
import net.crytec.acf.bukkit.contexts.OnlinePlayer;
import net.crytec.api.itemstack.customitems.CustomItem;
import net.crytec.api.util.F;
import net.crytec.api.util.UtilPlayer;
import net.crytec.api.util.language.LanguageHelper;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

@CommandAlias("playershop")
public class PlayerShopCommands extends BaseCommand {
	
	public PlayerShopCommands(ShopCore plugin) {
		this.shopManager = plugin.getPlayerShopManager();
	}
	
	private final PlayerShopManager shopManager;
	
	@Subcommand("create")
	@Syntax("<Shopname>")
	public void createShop(Player player, String displayname) {
		Optional<PlayerShop> shop = shopManager.createPlayerShop(player.getUniqueId(), displayname, player.getLocation().getBlock().getState().getLocation());
		if (!shop.isPresent()) return;
		
		shop.get().openForEdit(player);
	}
	
	@Subcommand("giveblocks")
	@CommandPermission("shop.giveblocks")
	public void giveBlocks(CommandIssuer issuer, OnlinePlayer target, @Default("1") Integer amount) {
		CustomItem item = API.getInstance().getCustomItemFactory().getCustomItemByKey(new NamespacedKey(ShopCore.getInstance(), "playershop-block"));
		target.getPlayer().getInventory().addItem(item.getItemStack().clone().asQuantity(amount));
		issuer.sendMessage(F.main("Shops", "Du hast " + target.getPlayer().getDisplayName() + "§7 erfolgreich §6" + amount + "§7 Shop Blöcke gegeben."));
		target.getPlayer().sendMessage(F.main("Shops", "Du hast " + amount + " Shop Blöcke erhalten."));
		UtilPlayer.playSound(target.getPlayer(), Sound.ENTITY_ITEM_PICKUP);
	}
	
	@Subcommand("edit")
	public void editShop(Player player, String id) {
		
		PlayerShop shop = shopManager.getPlayerShops().get(player.getUniqueId(), UUID.fromString(id));
		if (shop == null) return;
		
		shop.openForEdit(player);
	}
	
	@Subcommand("delete")
	@Private
	public void deleteShop(Player player, String id) {
		
		PlayerShop shop = shopManager.getPlayerShops().get(player.getUniqueId(), UUID.fromString(id));
		if (shop == null) return;
		
		String name = shop.getDisplayName();
		
		this.shopManager.deleteShop(player.getUniqueId(), UUID.fromString(id), true);
		
		player.sendMessage(F.main("Shop", name + " wurde gelöscht."));
		UtilPlayer.playSound(player, Sound.BLOCK_COMPARATOR_CLICK, 1, 0.75F);
	}
	
	@Subcommand("list")
	public void listShops(Player player) {
		if (shopManager.getShopsOfPlayer(player.getUniqueId()).isEmpty()) {
			player.sendMessage(F.error("Du hast keine Shops"));
			return;
		}
		
		player.sendMessage(F.main("Shops", "Deine Shops:"));
		for (PlayerShop shop : shopManager.getShopsOfPlayer(player.getUniqueId())) {
			ItemStack ware = null;
			if (shop.getTradeWare() != null) {
				ware = shop.getTradeWare().getIcon();
			}
			
			TextComponent builder = new TextComponent("§7Shop: §e" + ((shop.getDisplayName() == null) ? "Spielershop" : shop.getDisplayName()) + "§7 Ware: §6" + ((ware == null) ? "§cKeine Ware" : LanguageHelper.getItemDisplayName(ware) ) );
			builder.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/playershop edit " + shop.getShopID()));
			builder.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§7Klicke hier um den Shop zu editieren.")));
			
			TextComponent delete = new TextComponent(" §c§l[X]");
			delete.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/playershop delete " + shop.getShopID()));
			delete.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§cKlicke hier um den Shop zu löschen.")));
			builder.addExtra(delete);
			
			player.sendMessage(builder);
		}
	}
}