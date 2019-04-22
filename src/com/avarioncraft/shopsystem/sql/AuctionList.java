package com.avarioncraft.shopsystem.sql;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import com.avarioncraft.shopsystem.auctionhouse.AuctionHouseGUI.AuctionSortOrder;
import com.avarioncraft.shopsystem.auctionhouse.RedisAuctionWrapper;

import net.crytec.taskchain.TaskChainTasks.FutureFirstTask;

public class AuctionList implements FutureFirstTask<LinkedList<RedisAuctionWrapper>> {

	public AuctionList(DatabaseManager manager, AuctionSortOrder order) {
		this.manager = manager;
		this.order = order;
	}

	private final DatabaseManager manager;
	private final AuctionSortOrder order;

	@Override
	public CompletableFuture<LinkedList<RedisAuctionWrapper>> runFuture() {
		CompletableFuture<LinkedList<RedisAuctionWrapper>> future = new CompletableFuture<LinkedList<RedisAuctionWrapper>>();
		future.complete(this.manager.getAllAuctions(order));
		return future;
	}
}