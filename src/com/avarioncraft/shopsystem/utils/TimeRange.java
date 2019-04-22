package com.avarioncraft.shopsystem.utils;

import java.util.Arrays;
import java.util.BitSet;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import lombok.Getter;

public class TimeRange {
	
	public TimeRange() {
		this.validTimes = TreeRangeSet.create();
	}
	
	@Getter
	private final RangeSet<Integer> validTimes;
	
	public void implementRanges(int[] hours) {
		
		this.validTimes.clear();
		
		this.hoursArray = hours;
		Arrays.sort(hours);
		
		BitSet bs = new BitSet();
		
		for (int i = 0; i < hours.length; i++) {
			bs.set(hours[i]);
		}
		
		int begin = 0;
		int setpos = -1;
		while ((setpos = bs.nextSetBit(begin)) >= 0) {
			begin = bs.nextClearBit(setpos);
			this.validTimes.add(Range.closed(setpos, (begin - 1)));
		}
		
	}
	
	@Getter
	private int[] hoursArray = new int[0];
	
	public boolean isInRange(int time) {
		return validTimes.contains(time);
	}
}
