package com.avarioncraft.shopsystem.commands;

import org.bukkit.entity.Player;
import com.avarioncraft.shopsystem.adminshops.AdminShopPurchaseList;
import net.crytec.acf.BaseCommand;
import net.crytec.acf.annotation.CommandAlias;
import net.crytec.acf.annotation.Default;
import net.crytec.acf.annotation.Description;

@CommandAlias("adminshop")
public class AdminShopCommands extends BaseCommand {
		
	@Default
	@Description("Öffnet das Kaufen-Menü")
	public void openPurchaseInterface(Player sender) {
		AdminShopPurchaseList.open(sender);
	}
}