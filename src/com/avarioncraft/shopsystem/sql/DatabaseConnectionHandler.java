package com.avarioncraft.shopsystem.sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.avarioncraft.shopsystem.ShopCore;

import net.crytec.shaded.hikaricp.hikari.HikariConfig;
import net.crytec.shaded.hikaricp.hikari.HikariDataSource;

public class DatabaseConnectionHandler {

	private HikariDataSource dataSource;

	private final int minimumConnections;
	private final int maximumConnections;
	private final long connectionTimeout;

	public DatabaseConnectionHandler(ShopCore plugin) {
		this.minimumConnections = 4;
		this.maximumConnections = 8;
		this.connectionTimeout = 2500;
		this.setupPool(plugin.getConfig().getString("mysql.host"),
				plugin.getConfig().getString("mysql.port"),
				plugin.getConfig().getString("mysql.user"),
				plugin.getConfig().getString("mysql.password"),
				plugin.getConfig().getString("mysql.database")
				);
	}

	private void setupPool(String hostname, String port, String username, String password, String database) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + hostname + ":" + port + "/" + database);
		config.setDriverClassName("com.mysql.jdbc.Driver");
		config.setUsername(username);
		config.setPassword(password);
		config.setMinimumIdle(minimumConnections);
		config.setMaximumPoolSize(maximumConnections);
		config.setConnectionTimeout(connectionTimeout);
		config.setConnectionTestQuery("SELECT 1;");

		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "100");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		config.addDataSourceProperty("useLocalSessionState", "true");
		config.addDataSourceProperty("useLocalTransactionState", "true");
		config.addDataSourceProperty("cacheResultSetMetadata", "true");
		config.addDataSourceProperty("cacheServerConfiguration", "true");
		config.addDataSourceProperty("cacheResultSetMetadata", "true");

		dataSource = new HikariDataSource(config);
		
		
		Connection conn = null;

		try {
			conn = this.getConnection();
			Statement stmt = conn.createStatement();
			
			stmt.addBatch(this.readFile("auction_log.txt"));
			stmt.addBatch(this.readFile("auctions.txt"));
			stmt.addBatch(this.readFile("shop_logs.txt"));
			stmt.executeBatch();
			
		} catch (SQLException | IOException ex) {
			ex.printStackTrace();
		} finally {
			this.close(conn, null, null);
		}
		
		
	}

	public void close(Connection conn, PreparedStatement ps, ResultSet res) {
		if (conn != null)
			try {
				conn.close();
			} catch (SQLException ignored) {
			}
		if (ps != null)
			try {
				ps.close();
			} catch (SQLException ignored) {
			}
		if (res != null)
			try {
				res.close();
			} catch (SQLException ignored) {
			}
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void shutdown() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}
	
	private String readFile(String fileName) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ShopCore.getInstance().getResource("sql/" + fileName)))) {
			StringBuilder sb = new StringBuilder();
			String line = reader.readLine();

			while (line != null) {
				sb.append(line);
				line = reader.readLine();
			}
			return sb.toString();
		}
	}
}