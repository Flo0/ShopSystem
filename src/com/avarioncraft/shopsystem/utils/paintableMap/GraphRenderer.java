package com.avarioncraft.shopsystem.utils.paintableMap;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import net.crytec.shaded.org.apache.lang3.StringUtils;

public class GraphRenderer extends MapRenderer{
	
	public GraphRenderer(MapGraph mapGraph) {
		this.mapGraph = mapGraph;
	}
	
	private final MapGraph mapGraph;
	
	@Override
	public void render(MapView map, MapCanvas canvas, Player player) {
		
		if(mapGraph.checkUpdate()) {
			byte bCol = this.mapGraph.getBackColor();
			IntStream.range(0, 128).forEach(bx ->{
				IntStream.range(0, 128).forEach(by ->{
					canvas.setPixel(bx, by, bCol);
				});
			});
			
			ArrayList<int[]> graphs = this.mapGraph.getGraphValues();
			int upperBound = this.mapGraph.getUpperBound();
			
			for(int gID = 0; gID < graphs.size(); gID++) {
				byte graphColor = this.mapGraph.getGraphColors()[gID];
				int[] graph = graphs.get(gID);
				
				for(int barID = 0; barID < graph.length; barID++) {
					int barTop = 5 + (int) (122D - ((122D / (double) upperBound) * (double) graph[barID]));
					while(barTop < 128) {
						canvas.setPixel(barID, barTop, graphColor);
						barTop++;
					}
				}
			}
			
			canvas.drawText(0, 4, MinecraftFont.Font, mapGraph.getTitle() + " ------------------------");
			canvas.drawText(0, 62, MinecraftFont.Font, (mapGraph.getUpperBound() / 2) + mapGraph.getValueUnit() + "----------------------");
			canvas.drawText(0, 114, MinecraftFont.Font, mapGraph.getDynamicRange().getKey() + " " + mapGraph.getDynamicRange().getValue());
			canvas.drawText(0, 124, MinecraftFont.Font, StringUtils.repeat("|", 65));
			
		}
		
	}

}
