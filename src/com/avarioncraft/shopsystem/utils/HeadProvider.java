package com.avarioncraft.shopsystem.utils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class HeadProvider {
	
	public HeadProvider(JavaPlugin plugin) {
		this.headBase = Maps.newHashMap();
		
		this.create("BagOfGold", "http://textures.minecraft.net/texture/396ce13ff6155fdf3235d8d22174c5de4bf5512f1adeda1afa3fc28180f3f7");
		this.create("ChestHead", "http://textures.minecraft.net/texture/cdbca4b69eaf8dcb7ac3728228de8a64440787013342ddaabc1b00eeb8eec1e2");
		this.create("DropHead", "http://textures.minecraft.net/texture/8dd0cd158c2bb6618650e3954b2d29237f5b4c0ddc7d258e17380ab6979f071");
		this.create("ItemHead", "http://textures.minecraft.net/texture/8dd0cd158c2bb6618650e3954b2d29237f5b4c0ddc7d258e17380ab6979f071");
		this.create("MoneyHead", "http://textures.minecraft.net/texture/e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852");
		this.create("ExpHead", "http://textures.minecraft.net/texture/18950d1a0b8ccf063e7519fe369cad15cce32056fa472a99aa30b5fb448ff614");
		this.create("GoldBlock", "http://textures.minecraft.net/texture/54bf893fc6defad218f7836efefbe636f1c2cc1bb650c82fccd99f2c1ee6");
		this.create("GreenClock", "http://textures.minecraft.net/texture/7bd47dd7c3336e75a66391cdf9c935faeca8ce38ae22a1b27895e30b45245a8");
		this.create("BlueClock", "http://textures.minecraft.net/texture/3a8a604939177fd45df15f351f33f134a3f05186823dd3aed57c9fb228d417");
		this.create("PurpleClock", "http://textures.minecraft.net/texture/9d61846587dcfe74c8d45019aa9f77376cfbee49d048f2c6ae38fd1ba131f8");
		this.create("RedClock", "http://textures.minecraft.net/texture/f12f787c54dd89d12698dd17b5651294cfb8017d6ad4d26ee6a91cf1d0c1c4");
		this.create("CommandHead", "http://textures.minecraft.net/texture/8d19c68461666aacd7628e34a1e2ad39fe4f2bde32e231963ef3b35533");
		this.create("Oak_A", "http://textures.minecraft.net/texture/a67d813ae7ffe5be951a4f41f2aa619a5e3894e85ea5d4986f84949c63d7672e");
		this.create("Oak_Z", "http://textures.minecraft.net/texture/90582b9b5d97974b11461d63eced85f438a3eef5dc3279f9c47e1e38ea54ae8d");
		this.create("GoldArrowUp", "http://textures.minecraft.net/texture/dfeb39d71ef8e6a42646593393a5753ce26a1bee27a0ca8a32cb637b1ffae");
		this.create("GoldArrowDown", "http://textures.minecraft.net/texture/99e938181d8c96b4f58f6332d3dd233ec5fb851b5a840438eacdbb619a3f5f");
		this.create("OakArrowUp", "http://textures.minecraft.net/texture/3040fe836a6c2fbd2c7a9c8ec6be5174fddf1ac20f55e366156fa5f712e10");
		this.create("OakArrowDown", "http://textures.minecraft.net/texture/7437346d8bda78d525d19f540a95e4e79daeda795cbc5a13256236312cf");
		this.create("StevePlushie", "http://textures.minecraft.net/texture/730fea44349f073f3f93ce86ec9eee82f84a1c5e2967eb1c19b7c35f9a5e5f99");
		this.create("HerobrinePlushie", "http://textures.minecraft.net/texture/752e7c682f7a8cf7d70cebf6269424b8d7f18d5ebb39b96f57d0e2d6d7fca5");
		this.create("BaseMonitor", "http://textures.minecraft.net/texture/333dcfb4da10177264968b449e724adebee3bc33b72bae85842b4aab9bd9c4db");
		this.create("PaperStack", "http://textures.minecraft.net/texture/d806440f558864947dc093265006ea80d714524442b8a00906f2fb075077ceb3");
		this.create("LetterBox", "http://textures.minecraft.net/texture/d6aaef0120af71ba3b83fbddabc334bc63f2311599698a318243be69f0607da3");
		this.create("LaserOrb", "http://textures.minecraft.net/texture/56a7d2195ff7674bbb12e2f7578a2a63c54a980e64744450ac6656e05a790499");
	}
	
	private final Map<String, ItemStack> headBase;
	
	public ItemStack create(String key, String url) {
		
		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		
		SkullMeta headMeta = (SkullMeta) head.getItemMeta();
		
		GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		
		byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
		
		profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
		
		Field profileField = null;
		
		try {
            profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
            e1.printStackTrace();
        }
		
		head.setItemMeta(headMeta);
		
		this.headBase.put(key, head);
		return head.clone();
	}
	
	public ItemStack get(String key) {
		if(!this.headBase.containsKey(key)) return new ItemStack(Material.PLAYER_HEAD);
		return this.headBase.get(key).clone();
	}
}
