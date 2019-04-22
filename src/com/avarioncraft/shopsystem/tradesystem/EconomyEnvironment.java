package com.avarioncraft.shopsystem.tradesystem;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.adminshops.AdminShop.BuySell;
import com.avarioncraft.shopsystem.statistics.TransactionEvent;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.crytec.shaded.parsii.eval.Expression;
import net.crytec.shaded.parsii.eval.Parser;
import net.crytec.shaded.parsii.eval.Scope;
import net.crytec.shaded.parsii.eval.Variable;
import net.crytec.shaded.parsii.tokenizer.ParseException;

public class EconomyEnvironment implements Listener{
	
	public EconomyEnvironment(ShopCore plugin) {
		this.plugin = plugin;
		this.wareTransactionTimes = Maps.newHashMap();
		
		FileConfiguration config = plugin.getConfig();
		
		this.marketLimit = config.getLong("WareEconomyLimit");
		this.marketCooling = config.getInt("MarketCooling");
		this.varMarketLimit.setValue((double)this.marketLimit);
		
		try {
			this.marketFormular = Parser.parse(config.getString("MarketFormular"), scope);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		
		Bukkit.getScheduler().runTaskTimer(plugin, new MarketCoolingThread(), 0, 200);
		Bukkit.getPluginManager().registerEvents(this, plugin);
		
		File stats = new File(plugin.getDataFolder(), "adminshop-transactiondata.json");
		if (!stats.exists()) {
			try {
			stats.createNewFile();
			} catch (IOException ex) { }
		}
		
	}
	
	private final long marketLimit;
	private final int marketCooling;
	
	private final Scope scope = new Scope();
	private final Variable varMarketLimit = scope.getVariable("marketBound");
	private final Variable varWeight = scope.getVariable("weight");
	private final Variable varCurrent = scope.getVariable("market");
	private Expression marketFormular = null;
	
	private final ShopCore plugin;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	// EconomyID - TransactionTimes
	private final Map<UUID, Long> wareTransactionTimes;
	
	public double evaluateMarketPrice(TradeGood<?> good, double weigth) {
		if(good == null) return 0D;
		this.varWeight.setValue(weigth);
		this.varCurrent.setValue(this.wareTransactionTimes.getOrDefault(good.getEconomyID(), 0L));
		double eval = marketFormular.evaluate();
		return good.value * eval;
	}
	
	@EventHandler
	public void onTransaction(TransactionEvent event) {
		if(!event.getOccasion().toString().contains("ADMIN")) return;
		
		event.setTransactionMoney(this.evaluateMarketPrice(event.getTradeGood(), event.getWeight()));
		this.wareTransactionTimes.merge(event.getTradeGood().getEconomyID(), (event.getOccasion().getBuySell().equals(BuySell.SELL) ? -1L : 1L) * event.getTradeGood().amount, Long::sum);
		
	}
	
	public void save() {
		try (FileWriter writer = new FileWriter(new File(plugin.getDataFolder(), "adminshop-transactiondata.json"), false)) {
			writer.write(this.gson.toJson(this.wareTransactionTimes));
			writer.flush();
			writer.close();
		} catch (Exception ex) {
			
		}
	}
	
	@SuppressWarnings("serial")
	public void load() {
		File file = new File(plugin.getDataFolder(), "adminshop-transactiondata.json");
		Type type = new TypeToken<Map<UUID, Long>>() { }.getType();
		
		try (FileReader reader = new FileReader(file)) {
			Map<UUID, Long> map = gson.fromJson(reader, type);
			if (map == null) return;
			map.forEach( (uuid, id) -> this.wareTransactionTimes.put(uuid, id));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private final class MarketCoolingThread implements Runnable{

		@Override
		public void run() {
			
			wareTransactionTimes.entrySet().forEach(entry ->{
				long trTimes = entry.getValue();
				if(Math.abs(trTimes) < marketCooling) {
					entry.setValue(0L);
				}else {
					entry.setValue(trTimes + (trTimes < 0 ? 1 : -1) * marketCooling);
				}
				
			});
			
		}
		
	}
	
}
