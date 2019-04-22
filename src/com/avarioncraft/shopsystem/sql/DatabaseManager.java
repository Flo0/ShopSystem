package com.avarioncraft.shopsystem.sql;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;

import com.avarioncraft.shopsystem.ShopCore;
import com.avarioncraft.shopsystem.auctionhouse.AuctionEntry;
import com.avarioncraft.shopsystem.auctionhouse.AuctionHouse;
import com.avarioncraft.shopsystem.auctionhouse.AuctionHouseGUI.AuctionSortOrder;
import com.avarioncraft.shopsystem.auctionhouse.RedisAuctionWrapper;
import com.avarioncraft.shopsystem.commands.StatisticCommands.ToplistTimespan;
import com.avarioncraft.shopsystem.statistics.TransactionEvent;
import com.avarioncraft.shopsystem.statistics.TransactionOccasion;
import com.avarioncraft.shopsystem.tradesystem.ItemWare;
import com.avarioncraft.shopsystem.tradesystem.PermissionWare;
import com.avarioncraft.shopsystem.tradesystem.json.ItemWareDeserializer;
import com.avarioncraft.shopsystem.tradesystem.json.ItemWareSerializer;
import com.avarioncraft.shopsystem.tradesystem.json.PermissionWareDeserializer;
import com.avarioncraft.shopsystem.tradesystem.json.PermissionWareSerializer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;
import net.crytec.api.util.UUIDFetcher;
import net.crytec.api.util.UtilInv;
import net.crytec.api.util.language.LanguageHelper;

public class DatabaseManager {

	public DatabaseManager(DatabaseConnectionHandler handler, AuctionHouse auctionHouse) {
		gson = new GsonBuilder()
				.registerTypeAdapter(ItemWare.class, new ItemWareSerializer())
				.registerTypeAdapter(ItemWare.class, new ItemWareDeserializer())
				.registerTypeAdapter(PermissionWare.class, new PermissionWareSerializer())
				.registerTypeAdapter(PermissionWare.class, new PermissionWareDeserializer())
				.disableInnerClassSerialization()
				.create();
		
		this.handler = handler;
		this.auctionHouse = auctionHouse;
	}

	private final DatabaseConnectionHandler handler;
	private final Gson gson;
	private final AuctionHouse auctionHouse;
	
	@Getter
	private final ImmutableMap<String, Type> types = new ImmutableMap.Builder<String, Type>()
			.put(ItemWare.class.getSimpleName(), new TypeToken<ItemWare>() { }.getType())
			.put(PermissionWare.class.getSimpleName(), new TypeToken<PermissionWare>() { }.getType())
			.build();

	private static final String AUCTION_INSERT = "INSERT INTO auctions (auction, item, initialprice, currentprice, auctionholder, bidder, runout, serverID, data, itemname, ownername, amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String AUCTION_DELETE = "DELETE FROM auctions WHERE auction = ?;";
	
	
	public LinkedList<RedisAuctionWrapper> auctionSearch(String search) {
	     LinkedList<RedisAuctionWrapper> auctions = Lists.newLinkedList();
	        Connection conn = null;
	        PreparedStatement stmt = null;
	        ResultSet res = null;

	        try {
	            conn = this.handler.getConnection();
	            stmt = conn.prepareStatement("SELECT data FROM auctions WHERE DATE_ADD(runout, INTERVAL -1 MINUTE) >= NOW() AND itemname LIKE ?;");
	            stmt.setString(1, "%" + search + "%");
	            res = stmt.executeQuery();
	            
	            while (res.next()) {
	                JsonElement element = gson.fromJson(res.getString("data"), JsonElement.class);
	                RedisAuctionWrapper entry = new RedisAuctionWrapper(element.getAsJsonObject(), this.auctionHouse);
	                auctions.add(entry);
	            }
	        } catch (SQLException ex) {
	            ex.printStackTrace();
	        } finally {
	            this.handler.close(conn, stmt, null);
	        }
	        return auctions;
	}
	
	
	public LinkedList<RedisAuctionWrapper> getAllAuctions(AuctionSortOrder order) {
		LinkedList<RedisAuctionWrapper> auctions = Lists.newLinkedList();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			conn = this.handler.getConnection();
			if (order == AuctionSortOrder.NONE) {
				stmt = conn.prepareStatement("SELECT * FROM auctions WHERE DATE_ADD(runout, INTERVAL -1 MINUTE) >= NOW();");
			} else {
				stmt = conn.prepareStatement("SELECT * FROM auctions WHERE DATE_ADD(runout, INTERVAL -1 MINUTE) >= NOW() ORDER BY " + order.getKey() + " " + order.getMode() + ";");
			}
			res = stmt.executeQuery();
			
			while (res.next()) {
				JsonElement element = gson.fromJson(res.getString("data"), JsonElement.class);
				RedisAuctionWrapper entry = new RedisAuctionWrapper(element.getAsJsonObject(), this.auctionHouse);
				auctions.add(entry);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			this.handler.close(conn, stmt, null);
		}
		return auctions;
	}
	
	public Set<RedisAuctionWrapper> loadAuctionsForServer(String server) {

		Set<RedisAuctionWrapper> auctions = Sets.newHashSet();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement("SELECT data FROM auctions WHERE serverID = ?");
			stmt.setString(1, server);
			res = stmt.executeQuery();

			while (res.next()) {
				JsonElement element = gson.fromJson(res.getString("data"), JsonElement.class);
				RedisAuctionWrapper entry = new RedisAuctionWrapper(element.getAsJsonObject(), this.auctionHouse);
				auctions.add(entry);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			this.handler.close(conn, stmt, null);
		}
		return auctions;
	}
	

	public Set<UUID> getAndDeleteExpiredAuctions(String server) {
		Set<UUID> auctions = Sets.newHashSet();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement("SELECT auction FROM auctions WHERE runout < NOW();");
			res = stmt.executeQuery();

			while (res.next()) {
				auctions.add(UUID.fromString(res.getString("auction")));
			}			
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			this.handler.close(conn, stmt, null);
		}

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement(AUCTION_DELETE);

			for (UUID id : auctions) {
				stmt.setString(1, id.toString());
				stmt.addBatch();
			}
			stmt.executeBatch();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			this.handler.close(conn, stmt, null);
		}

		return auctions;
	}

	public void createAuction(AuctionEntry auction) {
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement(AUCTION_INSERT);

			stmt.setString(1, auction.getAuctionID().toString());
			stmt.setString(2, UtilInv.serializeItemStack(auction.getWare().value));
			stmt.setDouble(3, auction.getInitialPrice());
			stmt.setDouble(4, auction.getCurrentPrice());
			stmt.setString(5, auction.getOwner().getPlayerID().toString());

			if (auction.getBidder() == null) {
				stmt.setString(6, null);
			} else {
				stmt.setString(6, auction.getBidder().getPlayerID().toString());
			}
			stmt.setTimestamp(7, Timestamp.valueOf(auction.getAuctionEnd()));
			stmt.setString(8, ShopCore.getInstance().getServername());
			stmt.setString(9, auction.getWrappedAuction().toString());
			stmt.setString(10, auction.getWare().getWareDisplayName());
			stmt.setString(11, auction.getOwnerName());
			stmt.setInt(12, auction.getWare().getAmount());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			this.handler.close(conn, stmt, null);
		}
	}
	
	public void updateAuction(AuctionEntry auction) {
		
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement("UPDATE auctions SET currentprice = ?, bidder = ?, data = ? WHERE auction = ?");

			stmt.setDouble(1, auction.getCurrentPrice());
			stmt.setString(2, auction.getBidder().getPlayerID().toString());
			stmt.setString(3, auction.getWrappedAuction().asJson().toString());
			stmt.setString(4, auction.getAuctionID().toString());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			this.handler.close(conn, stmt, null);
		}
		
		
	}

	public void deleteAuction(AuctionEntry auction) {
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement(AUCTION_DELETE);
			stmt.setString(1, auction.getAuctionID().toString());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			this.handler.close(conn, stmt, null);
		}
	}
	
		
	public List<String> getToplist(int limit, TransactionOccasion type, ToplistTimespan timespan) {
		ArrayList<String> toplist = Lists.newArrayList();
		
		String sql = "SELECT seller, SUM(money) AS total FROM shop_logs WHERE type = ? AND timestamp BETWEEN ? AND ? GROUP by seller LIMIT ?";
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement(sql);
			
			stmt.setString(1, type.toString());
			stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(timespan.getDays())) );
			stmt.setTimestamp(3, Timestamp.valueOf( LocalDateTime.now()) );
			stmt.setInt(4, limit);
			
			res = stmt.executeQuery();

			while (res.next()) {
				toplist.add(UUIDFetcher.getName(UUID.fromString(res.getString("seller"))) + ": " + res.getDouble("total"));
			}
			return toplist;
		} catch (SQLException ex) {
			ex.printStackTrace();
			return toplist;
		} finally {
			this.handler.close(conn, stmt, res);
		}
	}
	
	public List<String> getFinishedAuctions(UUID player, int limit) {
		List<String> result = Lists.newArrayList();
		
		String sql = "SELECT * FROM auction_log WHERE auctionholder = ? ORDER BY runout DESC LIMIT " + limit + " ";
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement(sql);

			stmt.setString(1, player.toString());			
						
			res = stmt.executeQuery();

			while (res.next()) {
				String item = LanguageHelper.getItemDisplayName(UtilInv.deserializeItemStack(res.getString("item")));
				String server = res.getString("serverID");
				LocalDateTime start = res.getTimestamp("creationtime").toLocalDateTime();
				LocalDateTime end = res.getTimestamp("runout").toLocalDateTime();
				String buyer = res.getString("bidder");
				double price = res.getDouble("currentprice");
				double startprice = res.getDouble("initialprice");
				
				result.add(item + "§9 >> §f" + server + " §9@§f" + startprice);
				result.add("§9  Zeit: §f" + start + "§9>> §f" + end);
				result.add("§9  Gekauft: §f" + buyer + " für " + price);
				
			}
			return result;
		} catch (SQLException ex) {
			ex.printStackTrace();
			return result;
		} finally {
			this.handler.close(conn, stmt, res);
		}
	}
	
	public List<String> getGlobalLog(@Nullable UUID player, int limit) {
		List<String> result = Lists.newArrayList();
		
		
		String sql = "SELECT * FROM shop_logs WHERE buyer = ? OR seller = ? ORDER BY timestamp DESC";
		
		if (player == null) {
			sql = "SELECT * FROM shop_logs ORDER BY timestamp DESC";
		}
		
		if(limit > 0) {
			sql += " LIMIT " + limit;
		}
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			conn = this.handler.getConnection();
			stmt = conn.prepareStatement(sql);
			
			if (player != null) {
				stmt.setString(1, player.toString());
				stmt.setString(2, player.toString());
			}
			
			res = stmt.executeQuery();

			while (res.next()) {
				String ware = res.getString("ware");
				double price = res.getDouble("money");
				
				UUID buyer = UUID.fromString(res.getString("buyer"));
				String seller = res.getString("seller");
				UUID shop = UUID.fromString(res.getString("shop"));
				LocalDateTime time = res.getTimestamp("timestamp").toLocalDateTime();
				TransactionOccasion type = TransactionOccasion.valueOf(res.getString("type"));
				int amount = res.getInt("amount");
				
				result.add((buyer + " (" + Bukkit.getOfflinePlayer(buyer).getName() + ") >>> " + seller + " (" 
						+ (seller.equals("Adminshop") ? seller : Bukkit.getOfflinePlayer(UUID.fromString(seller)).getName()) + ")"
						+ " | Preis: " + price + " | Typ: " + type.toString() + " | ShopID: " + shop.toString()
						+ " | Am: " + time.toString() + " | Menge: " + amount + " | Ware_RAW: " + ware).replace(" | ", "\n   > ") + "\n");
				
			}
			return result;
		} catch (SQLException ex) {
			ex.printStackTrace();
			return result;
		} finally {
			this.handler.close(conn, stmt, res);
		}
	}
	
	
	public void logAuction(AuctionEntry entry) {
		Bukkit.getScheduler().runTaskAsynchronously(ShopCore.getInstance(), () -> {

			String sql = "INSERT INTO auction_log (auction, item, initialprice, currentprice, auctionholder, bidder, creationtime, runout, serverID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";

			Connection conn = null;
			PreparedStatement stmt = null;

			try {
				conn = this.handler.getConnection();
				stmt = conn.prepareStatement(sql);
				
				stmt.setString(1, entry.getAuctionID().toString());
				stmt.setString(2, UtilInv.serializeItemStack(entry.getWare().value));
				stmt.setDouble(3, entry.getInitialPrice());
				stmt.setDouble(4, entry.getCurrentPrice());
				stmt.setString(5, entry.getOwner().getPlayerID().toString());
				stmt.setString(6, entry.getBidder().getPlayerID().toString());
				stmt.setTimestamp(7, Timestamp.valueOf(entry.getAuctionStart()));
				stmt.setTimestamp(8, Timestamp.valueOf(entry.getAuctionEnd()));
				stmt.setString(9, entry.getServer());
				
				stmt.executeUpdate();
			} catch (SQLException ex) {
				ex.printStackTrace();
			} finally {
				this.handler.close(conn, stmt, null);
			}
		});
	}
	
	
	public void logShopTransaction(TransactionEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(ShopCore.getInstance(), () -> {

			String sql = "INSERT INTO shop_logs (buyer, seller, shop, money, amount, ware, type) VALUES (?, ?, ?, ?, ?, ?, ?);";

			Connection conn = null;
			PreparedStatement stmt = null;

			try {
				conn = this.handler.getConnection();
				stmt = conn.prepareStatement(sql);

				stmt.setString(1, e.getBuyerID().toString());
				stmt.setString(2, e.getSellerID() == null ? "Adminshop" : e.getSellerID().toString() );
				stmt.setString(3, e.getShopID().toString());
				stmt.setDouble(4, e.getTransactionMoney());
				stmt.setInt(5, e.getTradeGood().amount);
				stmt.setString(6, e.getTradeGood().asJson().toString());
				stmt.setString(7, e.getOccasion().toString());
				stmt.executeUpdate();
			} catch (SQLException ex) {
				ex.printStackTrace();
			} finally {
				this.handler.close(conn, stmt, null);
			}
		});
	}
}