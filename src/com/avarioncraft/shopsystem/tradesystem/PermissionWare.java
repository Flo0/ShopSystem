package com.avarioncraft.shopsystem.tradesystem;

import java.lang.reflect.Type;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import net.crytec.api.itemstack.ItemBuilder;

public class PermissionWare extends TradeWare<String>{

	public PermissionWare(String value) {
		super(value);
	}

	@Override
	public TradeGood<String> getGood(String tradeID, double value, int amount, UUID economyID) {
		return new PermissionGood(tradeID, value, amount, this.value, economyID);
	}

	@Override
	public Type getType() {
		return PermissionWare.class;
	}

	@Override
	public ItemStack getIcon() {
		return new ItemBuilder(Material.COMMAND_BLOCK)
				.enchantment(Enchantment.ARROW_DAMAGE)
				.setItemFlag(ItemFlag.HIDE_ENCHANTS)
				.name("§ePermission:")
				.lore("")
				.lore("§f" + this.value)
				.build();
	}

	@Override
	public String getWareDisplayName() {
		return "§ePermission: §f" + this.value;
	}

	@Override
	public int getAmount() {
		return 1;
	}
}