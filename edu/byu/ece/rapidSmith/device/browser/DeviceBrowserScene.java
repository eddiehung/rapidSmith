/*
 * Copyright (c) 2010 Brigham Young University
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

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.QSize;
import com.trolltech.qt.core.Qt.PenStyle;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsLineItem;
import com.trolltech.qt.gui.QGraphicsPixmapItem;
import com.trolltech.qt.gui.QGraphicsRectItem;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPen;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QImage.Format;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

/**
 * This class was written specifically for the DeviceBrowser class.  It
 * provides the scene content of the 2D tile array.
 */
public class DeviceBrowserScene extends QGraphicsScene {
	double currX, currY, prevX, prevY;
	int tileSize, numCols, numRows;
	double lineWidth;
	Device device;
	WireEnumerator we;
	public Signal0 updateStatus = new Signal0();
	public Signal0 updateTile = new Signal0();
	private QGraphicsRectItem highlit;
	private QImage qImage;
	private QPainter painter;
	private QPen wirePen;
	private ArrayList<QGraphicsLineItem> currLines;
	
	public DeviceBrowserScene(Device device, WireEnumerator we){
		this.device = device;
		this.we = we;
		currLines = new ArrayList<QGraphicsLineItem>();
		this.highlit = null;
		this.prevX = 0;
		this.prevY = 0;
		this.tileSize = 20;
		this.lineWidth = 1;
		if (device != null) {
			this.numRows = device.getRows();
			this.numCols = device.getColumns();
		} else {
			this.numRows = 8;
			this.numCols = 8;
		}
		setSceneRect(new QRectF(0, 0, (numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)));
		drawTileBackground();
	}

	public void setDevice(Device newDevice, WireEnumerator newWireEnumerator){
		this.device = newDevice;
		this.we = newWireEnumerator;
		this.highlit = null;
		this.prevX = 0;
		this.prevY = 0;
		if (device != null) {
			this.numRows = device.getRows();
			this.numCols = device.getColumns();
		} else {
			this.numRows = 8;
			this.numCols = 8;
		}
		this.clear();
		setSceneRect(new QRectF(0, 0, (numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)));
		drawTileBackground();
	}

	private void drawTileBackground(){
		setBackgroundBrush(new QBrush(QColor.black));
		//Create transparent QPixmap that accepts hovers 
		//  so that moveMouseEvent is triggered
		QPixmap qpm = new QPixmap(new QSize((numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)));
		qpm.fill(new QColor(255, 255,255, 0));
		QGraphicsPixmapItem background = addPixmap(qpm);
		background.setAcceptsHoverEvents(true);
		background.setZValue(-1);
		// Draw colored tiles onto QImage		
		qImage = new QImage(new QSize((numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)), Format.Format_RGB16);
		painter = new QPainter(qImage);
		wirePen = new QPen(QColor.yellow, 0.25, PenStyle.SolidLine);
		painter.setPen(new QPen(QColor.black, lineWidth));
		// Draw lines between tiles
		for (int i = 0; i <= numCols; i++){
			painter.drawLine((i) * tileSize, tileSize, (i) * tileSize,
					(numRows) * tileSize);
		}

		for (int j = 0; j <= numRows; j++){
			painter.drawLine(tileSize, (j) * tileSize, (numCols) * tileSize,
					(j) * tileSize);
		}

		for (int i = 0; i < numRows; i++){
			for (int j = 0; j < numCols; j++){
				Tile tile = device.getTile(i, j);
				String name = tile.getName();
				int hash = name.hashCode();
				int idx = name.indexOf("_");
				if (idx != -1) {
					hash = name.substring(0, idx).hashCode();
				}
				
				QColor color = QColor.fromRgb(hash);

				if(name.contains("HCLK")){
					color = QColor.cyan;
				} else if(name.startsWith("DSP")){
					color = QColor.darkCyan;
				} else if (name.startsWith("BRAM")){
					color = QColor.darkMagenta;
				} else if (name.startsWith("INT")){
					color = QColor.darkYellow;
				} else if (name.startsWith("CLB") || name.startsWith("CLEX")){
					color = QColor.blue;
				} else if (name.startsWith("DCM") || name.startsWith("CMT")){
					color = QColor.darkRed;
				} else if (name.startsWith("IOB") || name.startsWith("IOI")){
					color = QColor.darkGreen;
				} else if (name.startsWith("EMPTY") || name.startsWith("NULL")){
					color = QColor.fromRgb(240, 141, 45);
				}
				painter.fillRect(j * tileSize, i * tileSize, tileSize - 2, tileSize - 2, new QBrush(color));
			}
		}

		painter.end();
		
	}
	
	public void drawBackground(QPainter painter, QRectF rect){
		super.drawBackground(painter, rect);
		painter.drawImage(0, 0, qImage);
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
	public void mouseMoveEvent(QGraphicsSceneMouseEvent event){
		QPointF mousePos = event.scenePos();
		currX = Math.floor((mousePos.x()) / tileSize);
		currY = Math.floor((mousePos.y()) / tileSize);
		if (currX >= 0 && currY >= 0 && currX < numCols && currY < numRows
				&& (currX != prevX || currY != prevY)) {
			this.updateStatus.emit();
			updateCursor();
			prevX = currX;
			prevY = currY;
		}
		
		super.mouseMoveEvent(event);
	}

	@Override
	public void mouseDoubleClickEvent(QGraphicsSceneMouseEvent event){
		this.updateTile.emit();
		super.mouseDoubleClickEvent(event);
	}
	
	private void updateCursor(){
		if (highlit == null) {
			QPen cursorPen = new QPen(QColor.yellow, 1);
			highlit = addRect(currX * tileSize, currY * tileSize, tileSize - 2,
					tileSize - 2, cursorPen);
		} else {
			highlit.moveBy((currX - prevX) * tileSize, (currY - prevY)
					* tileSize);
		}
	}

	public double getCurrX(){
		return currX;
	}

	public double getCurrY(){
		return currY;
	}

}
