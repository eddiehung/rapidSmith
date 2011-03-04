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
package edu.byu.ece.rapidSmith.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.QSize;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.PenStyle;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsPixmapItem;
import com.trolltech.qt.gui.QGraphicsRectItem;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPen;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QImage.Format;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;

/**
 * This class is used for the design explorer although, it could 
 * be used for building other applications as well.
 * @author Chris Lavin
 */
public class TileScene extends QGraphicsScene{
	
	private QGraphicsRectItem highlit;
	
	QPen cursorPen = new QPen(QColor.yellow, 2);
	
	int currX, currY, prevX, prevY;
	int tileSize, numCols, numRows;
	Tile[][] drawnTiles;
	HashMap<Tile,Integer> tileXMap;
	HashMap<Tile,Integer> tileYMap;
	double lineWidth;
	Device device;
	public Signal1<String> updateStatus = new Signal1<String>();
	public Signal0 mousePressed = new Signal0();
	protected ArrayList<QPointF> movingPosList;
	private Design currDesign;
	private QImage qImage;
	
	public TileScene(){
		this(null);
	}
	
	public TileScene(Design design){
		this.tileSize = 20;
		this.lineWidth = 1;
		this.currDesign = design;
		init();
	}
	
	public void init() {
		this.clear();
		System.gc();
		prevX = 0;
		prevY = 0;
		movingPosList = new ArrayList<QPointF>();
		this.device = (currDesign == null) ? null : currDesign.getDevice();
		if (device != null) {
			numRows = device.getRows();
			numCols = device.getColumns();
			setSceneRect(new QRectF(0, 0, (numCols + 1) * (tileSize + 1),
					(numRows + 1) * (tileSize + 1)));
			drawSliceBackground();
		} else {
			setSceneRect(new QRectF(0, 0, 1 * (tileSize + 1),
					1 * (tileSize + 1)));
		}
	}
	
	private void drawSliceBackground(){
		setBackgroundBrush(new QBrush(QColor.black));
		
		//Create transparent QPixmap that accepts hovers 
		//  so that moveMouseEvent is triggered
		QPixmap qpm = new QPixmap(new QSize((numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)));
		qpm.fill(new QColor(255, 255,255, 0));
		QGraphicsPixmapItem background = addPixmap(qpm);
		background.setAcceptsHoverEvents(true);
		background.setZValue(-1);

		// Draw colored tiles onto QPixMap
		qImage = new QImage(new QSize((numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)), Format.Format_RGB16);
		QPainter painter = new QPainter(qImage);
		
		TreeSet<Integer> colsToSkip = new TreeSet<Integer>();
		TreeSet<Integer> rowsToSkip = new TreeSet<Integer>();
		for (int y = 0; y < numRows; y++) {
			for (int x = 0; x < numCols; x++) {
				Tile tile = device.getTile(y, x);
				if(y > 0 && y < numRows-2){
					Tile nextRowTile = device.getTile(y+1,x);
					if(nextRowTile.getTileYCoordinate() == tile.getTileYCoordinate()){
						rowsToSkip.add(y);
						break;
					}
				}
				if(x > 2 && x < numCols-1){
					Tile prevColTile = device.getTile(y,x-2);
					if(prevColTile.getTileXCoordinate() == tile.getTileXCoordinate()){
						colsToSkip.add(x);
						continue;
					}
				}
			}
		}
		int i=0;
		int j=0;
		drawnTiles = new Tile[numRows-rowsToSkip.size()][numCols-colsToSkip.size()];
		tileXMap = new HashMap<Tile, Integer>();
		tileYMap = new HashMap<Tile, Integer>();
		for (int y = 0; y < numRows; y++) {
			if(rowsToSkip.contains(y))
				continue;
			for (int x = 0; x < numCols; x++) {
				if(colsToSkip.contains(x))
					continue;
				Tile tile = device.getTile(y, x);
				drawnTiles[i][j] = tile;
				tileXMap.put(tile, j);
				tileYMap.put(tile, i);
				j++;
			}
			i++;
			j=0;
		}
		numRows = numRows-rowsToSkip.size();
		numCols = numCols-colsToSkip.size();
		QPen cfgLine = new QPen(QColor.lightGray, 2, PenStyle.DashLine);
		painter.setPen(cfgLine);
		int cnt = 0;
		for(int col : colsToSkip){
			int realCol = col - cnt;
			painter.drawLine(tileSize*realCol-1, 0, tileSize*realCol-1, numRows*tileSize-3);
			cnt++;
		}
		cnt=0;
		for(int row : rowsToSkip){
			int realRow = row - cnt;
			painter.drawLine(0,tileSize*realRow-1, numCols*tileSize-3,tileSize*realRow-1);
			cnt++;
		}
		
		for (int y = 0; y < numRows; y++) {
			for (int x = 0; x < numCols; x++) {
				Tile tile = drawnTiles[y][x];
				String name = tile.getName();
				TileType tileType = tile.getType();
				int hash = name.hashCode();
				int idx = name.indexOf("_");
				if (idx != -1) {
					hash = name.substring(0, idx).hashCode();
				}
				QColor color = QColor.fromRgb(hash);

				if (name.startsWith("DSP")) {
					color = QColor.darkCyan;
				} else if (name.startsWith("BRAM")) {
					color = QColor.darkMagenta;
				} else if (name.startsWith("INT")) {
					color = QColor.darkYellow;
				} 

				int offset = (int) Math.ceil((lineWidth / 2.0));
				int rectX = x * tileSize;
				int rectY = y * tileSize;
				int rectSide = tileSize - 2 * offset;

				if (tileType.equals(TileType.CLB)) {
					painter.setPen(color);
					painter.drawRect(rectX, rectY, rectSide / 2 - 1,
							rectSide / 2 - 1);
					painter.drawRect(rectX + rectSide / 2,
							rectY + rectSide / 2, rectSide / 2 - 1,
							rectSide / 2 - 1);
					painter.drawRect(rectX, rectY + rectSide / 2,
							rectSide / 2 - 1, rectSide / 2 - 1);
					painter.drawRect(rectX + rectSide / 2, rectY,
							rectSide / 2 - 1, rectSide / 2 - 1);
				}else if (tileType.equals(TileType.CLBLL) || tileType.equals(TileType.CLBLM)) {
					painter.setPen(QColor.fromRgb("CLB".hashCode()));
					painter.drawRect(rectX, rectY + rectSide / 2,
							rectSide / 2 - 1, rectSide / 2 - 1);
					painter.drawRect(rectX + rectSide / 2, rectY,
							rectSide / 2 - 1, rectSide / 2 - 1);
				} else if (name.startsWith("INT")) {
					painter.setPen(color);
					painter.drawRect(rectX + rectSide / 6, rectY,
							4 * rectSide / 6 - 1, rectSide - 1);
				} else if (name.startsWith("EMPTY_DSP")
						|| name.startsWith("EMPTY_BRAM")) {
					// do nothing
				} else if (name.startsWith("DSP")) {
					painter.setPen(color);
					painter.drawRect(rectX, rectY - 3 * tileSize, rectSide - 1,
							4 * rectSide + 3 * 2 * offset - 1);
					painter.setPen(color.darker());
					painter
							.drawRect(rectX + 2, rectY - 3 * tileSize + 2,
									rectSide - 1 - 4, 2 * rectSide + 2 * offset
											- 1 - 2);
					painter
							.drawRect(rectX + 2, rectY - tileSize,
									rectSide - 1 - 4, 2 * rectSide + 2 * offset
											- 1 - 2);
				} else if (name.startsWith("BRAM")) {
					painter.setPen(color);
					painter.drawRect(rectX, rectY - 3 * tileSize, rectSide - 1,
							4 * rectSide + 3 * 2 * offset - 1);
					painter.setPen(color.darker());
					painter.drawRect(rectX + 2, rectY - 3 * tileSize + 2,
							rectSide / 2 - 4, 4 * (rectSide + offset) - 3);
					painter.drawRect(rectX + rectSide / 2 + 1, rectY - 3
							* tileSize + 2, rectSide / 2 - 4,
							4 * (rectSide + offset) - 3);

				} else {
					painter.fillRect(x * tileSize, y * tileSize, tileSize - 2
							* offset, tileSize - 2 * offset, new QBrush(color));
				}
			}
		}

		painter.end();
	}

	public void drawBackground(QPainter painter, QRectF rect){
		super.drawBackground(painter, rect);
		painter.drawImage(0, 0, qImage);
	}

	@Override
	public void mouseMoveEvent(QGraphicsSceneMouseEvent event) {
		QPointF mousePos = event.scenePos();
		if (device != null) {
			currX = (int) Math.floor((mousePos.x()) / tileSize);
			currY = (int) Math.floor((mousePos.y()) / tileSize);
			if (currX >= 0 && currY >= 0 && currX < numCols && currY < numRows
					&& (currX != prevX || currY != prevY)) {
				String tileName = device.getPartName() + " | " +  drawnTiles[currY][currX].getName() +
				" | " + drawnTiles[currY][currX].getType() + " (" + currX + "," + currY + ")";
				this.updateStatus.emit(tileName);
				prevX = currX;
				prevY = currY;
			}
		}
		super.mouseMoveEvent(event);
	}
	
	@Override
	public void mousePressEvent(QGraphicsSceneMouseEvent event){
		if(event.button().equals(Qt.MouseButton.LeftButton)){
			QPointF mousePos = event.scenePos();
			currX = (int) Math.floor((mousePos.x()) / tileSize);
			currY = (int) Math.floor((mousePos.y()) / tileSize);

			if (currX >= 0 && currY >= 0 && currX < numCols && currY < numRows){
				updateCursor();
			}			
		}
		super.mousePressEvent(event);
	}
	
	public void updateCursor(){
		if(highlit != null){
			highlit.dispose();
		}
		highlit = addRect(currX * tileSize, currY * tileSize, tileSize - 2,
				tileSize - 2, cursorPen);
	}
	
	public void updateCurrXY(int currX, int currY){
		this.currX = currX;
		this.currY = currY;
	}
	
	public int getDrawnTileX(Tile tile){
		Integer tmp = tileXMap.get(tile);
		if(tmp == null)
			return -1;
		return tmp;
	}
	
	public int getDrawnTileY(Tile tile){
		Integer tmp = tileYMap.get(tile);
		if(tmp == null)
			return -1;
		return tmp;
	}
	

	public Design getCurrDesign(){
		return currDesign;
	}

	public void setCurrDesign(Design currDesign){
		this.currDesign = currDesign;
	}

	public double getCurrX(){
		return currX;
	}

	public double getCurrY(){
		return currY;
	}
	
	public int getTileSize(){
		return tileSize;
	}
}
