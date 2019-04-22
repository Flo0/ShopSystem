package com.avarioncraft.shopsystem.auctionhouse;

import com.google.gson.JsonObject;

import net.crytec.api.redis.RedisManager.ServerType;
import net.crytec.api.redis.RedisPacketProvider;

//TODO Redis zeugs
public class AuctionPacket implements RedisPacketProvider {

	@Override
	public String getID() {
		return "auction";
	}
	
	public AuctionPacket(AuctionEntry auction) {
		
	}

	@Override
	public JsonObject getPacketData() {
		return null;
	}

	@Override
	public ServerType getReceiverType() {
		return ServerType.SPIGOT;
	}

	@Override
	public void handlePacketReceive(JsonObject data) {
		
	}

}
