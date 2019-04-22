package com.avarioncraft.shopsystem.tradesystem;

import java.lang.reflect.Type;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import net.crytec.api.util.language.LanguageHelper;

public class ItemWare extends TradeWare<ItemStack> {

	public ItemWare(ItemStack value) {
		super(value);
	}
	
	@Override
	public TradeGood<ItemStack> getGood(String tradeID, double value, int amount, UUID economyID) {
		return new ItemGood(tradeID, value, amount, this.value, economyID);
	}


	@Override
	public Type getType() {
		return ItemWare.class;
	}

	@Override
	public ItemStack getIcon() {
		return this.value;
	}

	@Override
	public String getWareDisplayName() {
		return LanguageHelper.getItemDisplayName(this.value);
	}

	@Override
	public int getAmount() {
		return this.value.getAmount();
	}
}