package com.avarioncraft.shopsystem.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;

public class TimeInput {
	
	private static final Range<Integer> NUMS = Range.closed(48, 57);
	private static final ImmutableMap<Character, Long> TIME_VALUES = ImmutableMap.<Character, Long>builder()
			.put('s', 1000L)
			.put('m', 60000L)
			.put('h', 3600000L)
			.put('d', 86400000L)
			.put('w', 604800000L)
			.build();
	
	public static long parse(String string) {
		if(!NUMS.contains((int)string.charAt(0))) return 0;
		
		long time = 0;
		
		String value = "";
		
		for(int i = 0; i < string.length(); i++) {
			Character c = string.charAt(i);
			if(NUMS.contains((int)c)) {
				value += c;
			}else {
				if(!TIME_VALUES.containsKey(c)) return 0;
				time += TIME_VALUES.get(c) * Long.valueOf(value);
				value = "";
			}
		}
		
		return time;
	}
	

	
}
