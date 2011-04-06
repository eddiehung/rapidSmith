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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.core.Qt.PenStyle;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsLineItem;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPen;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.gui.TileScene;
import edu.byu.ece.rapidSmith.router.Node;

/**
 * This class was written specifically for the DeviceBrowser class.  It
 * provides the scene content of the 2D tile array.
 */
public class DeviceBrowserScene extends TileScene{
	private WireEnumerator we;
	public Signal1<Tile> updateTile = new Signal1<Tile>();
	private QPen wirePen;
	private ArrayList<QGraphicsLineItem> currLines;
	private DeviceBrowser browser;
	private Tile reachabilityTile;
	public DeviceBrowserScene(Device device, WireEnumerator we, boolean hideTiles, boolean drawPrimitives, DeviceBrowser browser){
		super(device, hideTiles, drawPrimitives);
		setWireEnumerator(we);
		currLines = new ArrayList<QGraphicsLineItem>();
		wirePen = new QPen(QColor.yellow, 0.25, PenStyle.SolidLine);
		this.browser = browser;
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
		double x1 = (double) tileXMap.get(src)*tileSize  + (wireSrc%tileSize);
		double y1 = (double) tileYMap.get(src)*tileSize  + (wireSrc*tileSize)/enumSize;
		double x2 = (double) tileXMap.get(dst)*tileSize  + (wireDst%tileSize);
		double y2 = (double) tileYMap.get(dst)*tileSize  + (wireDst*tileSize)/enumSize;
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
		for(WireConnection w : tile.getWireConnections(wire)){
			drawWire(tile, wire, w.getTile(tile), w.getWire());
		}
	}

	private HashMap<Tile, Integer> findReachability(Tile t, Integer hops){
		HashMap<Tile, Integer> reachabilityMap = new HashMap<Tile, Integer>();
		
		Queue<Node> queue = new LinkedList<Node>();
		for(Integer wire : t.getWires()){
			WireConnection[] connections = t.getWireConnections(wire);
			if(connections == null) continue;
			for(WireConnection wc : connections){
				queue.add(wc.createNode(t));
			}
		}
		
		while(!queue.isEmpty()){
			Node currNode = queue.poll();
			Integer i = reachabilityMap.get(currNode.getTile());
			if(i == null){
				i = new Integer(1);
				reachabilityMap.put(currNode.getTile(), i);
			}
			else{
				reachabilityMap.put(currNode.getTile(), i+1);						
			}
			if(currNode.getLevel() > hops-1){
				WireConnection[] connections = currNode.getConnections();
				if(connections != null){
					for(WireConnection wc : connections){
						wc.createNode(currNode);
					}
				}
			}
		}
		
		/*
		for(Integer wire : t.getWires()){
			WireConnection[] connections = t.getWireConnections(wire);
			if(connections == null) continue;
			for(WireConnection wc : connections){
				Tile connTile = wc.getTile(t.getDevice(), t);
				if(!t.equals(connTile)){
					Integer i = reachabilityMap.get(connTile);
					if(i == null){
						i = new Integer(1);
						reachabilityMap.put(connTile, i);
					}
					else{
						reachabilityMap.put(connTile, i+1);						
					}
					if(hops > 1){
						if(connTile.getWireConnections(wc.getWire()) != null){
							for(WireConnection wc2 : connTile.getWireConnections(wc.getWire())){
								Tile connTile2 = wc2.getTile(connTile.getDevice(), connTile);
								if(!t.equals(connTile2)){
									Integer i2 = reachabilityMap.get(connTile2);
									if(i2 == null){
										i2 = new Integer(1);
										reachabilityMap.put(connTile2, i2);
									}
									else{
										reachabilityMap.put(connTile2, i2+1);						
									}
									if(hops > 2){
										if(connTile.getWireConnections(wc2.getWire()) != null){
											for(WireConnection wc3 : connTile.getWireConnections(wc2.getWire())){
												Tile connTile3 = wc3.getTile(connTile2.getDevice(), connTile2);
												if(!t.equals(connTile3)){
													Integer i3 = reachabilityMap.get(connTile3);
													if(i3 == null){
														i3 = new Integer(1);
														reachabilityMap.put(connTile3, i3);
													}
													else{
														reachabilityMap.put(connTile3, i3+1);						
													}
												}
											}										
										}
									}
								}
							}
						}
					}
				}
			}
		}*/
		return reachabilityMap;
	}
	
	private void drawReachability(HashMap<Tile, Integer> map){
		QPainter painter = new QPainter(qImage);
		int offset = 1;
		
		for(Tile t : map.keySet()){
			int x = tileXMap.get(t);
			int y = tileYMap.get(t);
			painter.fillRect(x * tileSize, y * tileSize,
					tileSize - 2 * offset, tileSize - 2 * offset, new QBrush(QColor.white));
			painter.drawText(x * tileSize + (tileSize/4), y * tileSize + (tileSize/2), map.get(t).toString());
		}
		painter.end();
	}
	
	@SuppressWarnings("unused")
	private void menuReachability1(){
		System.out.println("menu1");
		//drawReachability(findReachability(reachabilityTile, 1));
	}

	@SuppressWarnings("unused")
	private void menuReachability2(){
		System.out.println("menu2");
		//drawReachability(findReachability(reachabilityTile, 2));
	}
	
	@SuppressWarnings("unused")
	private void menuReachability3(){
		System.out.println("menu3");
		//drawReachability(findReachability(reachabilityTile, 3));
	}
	
	@Override
	public void mouseDoubleClickEvent(QGraphicsSceneMouseEvent event){
		Tile t = getTile(event);
		this.updateTile.emit(t);
		super.mouseDoubleClickEvent(event);
	}
	
	@Override
	public void mouseReleaseEvent(QGraphicsSceneMouseEvent event){
		if(event.button().equals(MouseButton.RightButton)){
			if(browser.view.hasPanned){
				browser.view.hasPanned = false;

			}
			else{
				reachabilityTile = getTile(event);
				QMenu menu = new QMenu();
				QAction action1 = new QAction("Draw Reachability (1 Hop)", this);
				QAction action2 = new QAction("Draw Reachability (2 Hops)", this);
				QAction action3 = new QAction("Draw Reachability (3 Hops)", this);
				action1.triggered.connect(this, "menuReachability1()");
				action2.triggered.connect(this, "menuReachability2()");
				action3.triggered.connect(this, "menuReachability3()");
				menu.addAction(action1);
				menu.addAction(action2);
				menu.addAction(action3);
				menu.exec(event.screenPos());			
			}
		}
		
		
		super.mouseReleaseEvent(event);
	}
}
