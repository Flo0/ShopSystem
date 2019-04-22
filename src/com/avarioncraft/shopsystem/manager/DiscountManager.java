package com.avarioncraft.shopsystem.manager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.crytec.api.nbt.NBTItem;
import net.crytec.api.util.UtilMath;

public class DiscountManager{
	
	public static final String DISCOUNT_FLAG = "adminShopDiscount";
	
	public static double calculateDiscountMultiplier(Player player) {
		
		double itemMulti = getItemStackMulti(player.getInventory().getItemInMainHand().clone());
		double permMulti = getPermissionMulti(player);
		
		if(itemMulti != 1) player.getInventory().clear(player.getInventory().getHeldItemSlot());
		
		return itemMulti * permMulti;
		
	}
	
	private static double getPermissionMulti(Player player) {
		return player.hasMetadata(DISCOUNT_FLAG) ? player.getMetadata(DISCOUNT_FLAG).get(0).asDouble() : 0D;
	}
	
	private static double getItemStackMulti(ItemStack item) {
		
		if(item == null || item.getType().equals(Material.AIR)) return 1.0D;
		NBTItem nbt = new NBTItem(item);
		if(!nbt.hasKey(DISCOUNT_FLAG)) return 1.0D;
		
		return nbt.getDouble(DISCOUNT_FLAG);
	}
	
	public String displayOf(ItemStack item) {
		if(item == null || item.getType().equals(Material.AIR)) return "0%";
		NBTItem nbt = new NBTItem(item);
		if(!nbt.hasKey(DISCOUNT_FLAG)) return "0%";
		
		return UtilMath.unsafeRound(nbt.getDouble(DISCOUNT_FLAG) * 100, 1) + "%";
	}
	
}
