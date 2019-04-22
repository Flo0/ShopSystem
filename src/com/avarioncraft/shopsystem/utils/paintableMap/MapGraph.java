package com.avarioncraft.shopsystem.utils.paintableMap;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

public interface MapGraph {
	
	/**
	 * The grahps title text
	 * @return
	 */
	public abstract String getTitle();
	
	/**
	 * The Background color.
	 * @return color from {@link https://minecraft.gamepedia.com/Map_item_format#1.12_Color_Table}
	 */
	public abstract byte getBackColor();
	/**
	 * The highest value that will be represented.
	 * @return the value.
	 */
	public abstract int getUpperBound();
	/**
	 * A list of int[] representing all values of
	 * some graphs. int[] has to be of size 0-128
	 * @return An array of graph values.
	 */
	public abstract ArrayList<int[]> getGraphValues();
	/**
	 * The size has to be the amount of Graphs.
	 * @return An ordered
	 */
	public abstract byte[] getGraphColors();
	/**
	 * The unit of the graphs y-axis
	 * @return
	 */
	public abstract String getValueUnit();
	/**
	 * If the graphs have new values.
	 * Should set the update state to false
	 * after this is called.
	 * @return true if graph has new values.
	 */
	public abstract boolean checkUpdate();
	/**
	 * Returns the numeric range between two graph values
	 * plus the the unit of the graphs x-axis
	 * @return
	 */
	public abstract Pair<Integer, String> getDynamicRange();
}