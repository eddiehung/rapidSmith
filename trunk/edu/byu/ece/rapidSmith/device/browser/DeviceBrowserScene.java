/*
 * Copyright (c) 2010-2011 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.device.browser;


import java.util.ArrayList;

import com.trolltech.qt.gui.QGraphicsLineItem;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QPen;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.gui.TileScene;

/**
 * This class was written specifically for the DeviceBrowser class.  It
 * provides the scene content of the 2D tile array.
 */
public class DeviceBrowserScene extends TileScene{
	private WireEnumerator we;
	public Signal0 updateTile = new Signal0();
	private QPen wirePen;
	private ArrayList<QGraphicsLineItem> currLines;
	
	public DeviceBrowserScene(Device device, WireEnumerator we, boolean hideTiles, boolean drawPrimitives){
		super(device, hideTiles, drawPrimitives);
	}
	
	public WireEnumerator getWireEnumerator(){
		return we;
	}

	public void setWireEnumerator(WireEnumerator we){
		this.we = we;
	}

	public void drawWire(Tile src, Tile dst){
		QGraphicsLineItem line = new QGraphicsLineItem(
				src.getColumn()*tileSize  + tileSize/2,
				src.getRow()*tileSize + tileSize/2,
				dst.getColumn()*tileSize + tileSize/2,
				dst.getRow()*tileSize + tileSize/2);
		line.setPen(wirePen);
		addItem(line);
	}

	public void clearCurrentLines(){
		for(QGraphicsLineItem line : currLines){
			this.removeItem(line);
			line.dispose();
		}
		currLines.clear();
	}
	
	public void drawWire(Tile src, int wireSrc, Tile dst, int wireDst){
		double enumSize = we.getWires().length;
		
		double x1 = (double) src.getColumn()*tileSize  + (wireSrc%tileSize);
		double y1 = (double) src.getRow()*tileSize  + (wireSrc*tileSize)/enumSize;
		double x2 = (double) dst.getColumn()*tileSize  + (wireDst%tileSize);
		double y2 = (double) dst.getRow()*tileSize  + (wireDst*tileSize)/enumSize;
		
		WireConnectionLine line = new WireConnectionLine(x1,y1,x2,y2, this, dst, wireDst);
		line.setToolTip(src.getName() + " " + we.getWireName(wireSrc) + " -> " +
				dst.getName() + " " + we.getWireName(wireDst));
		line.setPen(wirePen);
		line.setAcceptHoverEvents(true);
		addItem(line);
		currLines.add(line);
	}

	public void drawConnectingWires(Tile tile, int wire){
		clearCurrentLines();
		if(tile == null) return;
		if(tile.getWireConnections(wire) == null) return;
		for(Wire w : tile.getWireConnections(wire)){
			drawWire(tile, wire, w.getTile(device, tile), w.getWire());
		}
	}

	@Override
	public void mouseDoubleClickEvent(QGraphicsSceneMouseEvent event){
		this.updateTile.emit();
		super.mouseDoubleClickEvent(event);
	}
}
