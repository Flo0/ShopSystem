package com.avarioncraft.shopsystem.utils;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;
import net.crytec.api.util.UtilInv;

public class UtilPlayer {
	
	static private final LuckPermsApi perms = LuckPerms.getApi();
	
	public static String getMeta(UUID uuid, String key, String defaultValue) {
		User user = perms.getUser(uuid);
		Optional<Contexts> contexts = perms.getContextForUser(user);

		if (contexts.isPresent()) {
			MetaData meta = user.getCachedData().getMetaData(contexts.get());
			return meta.getMeta().getOrDefault(key, defaultValue);
		} else {
			return defaultValue;
		}
	}
	
	public static void giveItems(Inventory inv, ItemStack item, int amount, boolean dropExcess, Location dropLocation) {
		
		int stackAmount = item.getAmount();
		int stackSize = item.getMaxStackSize();
		int stacks = (int)((double)(amount * stackAmount) / (double)stackSize);
		int left = (int)((double)(amount * stackAmount) % (double)stackSize);
		
		ItemStack singleItem = item.asOne();
		
		System.out.println("Inventory: " + inv.getType() + " First Empty Slot: " + inv.firstEmpty());
		
		if(stacks != 0) {
			singleItem.setAmount(stackSize);
			IntStream.range(0, stacks).forEach(i ->{
				UtilInv.insert(inv, singleItem.clone(), dropExcess, dropLocation);
			});
		}
		
		if(left != 0) {
			singleItem.setAmount(left);
			UtilInv.insert(inv, singleItem.clone(), dropExcess, dropLocation);
		}
		
	}
	
	public static boolean hasItems(Inventory inv, ItemStack item, int amount) {
		
		int fullAmount = amount * item.getAmount();
		
		return fullAmount <= IntStream.range(0, 36)
				.mapToObj(i -> inv.getItem(i))
				.filter(i -> i != null && i.isSimilar(item))
				.mapToInt(i -> i.getAmount())
				.sum();
		
	}
	
	public static int takeItems(Inventory inv, ItemStack item, int amount) {
		
		int fullAmount = amount * item.getAmount();
		
		ItemStack current;
		int currentAmount, left = fullAmount;
		
		for(int i = 0; i < inv.getSize(); i++) {
			current = inv.getItem(i);
			if(current == null || !current.isSimilar(item)) continue;
			currentAmount = current.getAmount();
			if(currentAmount < left) {
				left -= currentAmount;
				inv.clear(i);
				continue;
			}else {
				current.subtract(left);
				return 0;
			}
		}
		
		return left;
	}
	
}
