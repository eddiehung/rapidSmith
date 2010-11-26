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
package edu.byu.ece.rapidSmith.design.explorer;

import com.trolltech.qt.core.QSize;
import com.trolltech.qt.core.Qt.AspectRatioMode;
import com.trolltech.qt.gui.QGridLayout;
import com.trolltech.qt.gui.QWidget;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.browser.DeviceBrowserView;

/**
 * This class is used for the tile window tab of the design explorer.
 * It could also be used for other applications as well.
 * @author Chris Lavin
 */
public class TileWindow extends QWidget{
	/** Associated view with this window */
	protected DeviceBrowserView view;
	/** Associated scene with this window */
	protected TileScene scene;
	/** The current design */
	protected Design design;
	/** The layout for the window */
	private QGridLayout layout;
	
	/**
	 * Constructor
	 * @param parent 
	 */
	public TileWindow(QWidget parent){
		super(parent);
		scene = new TileScene();
		view = new DeviceBrowserView(scene);
		layout = new QGridLayout();
		layout.addWidget(view);
		this.setLayout(layout);
	}
	
	/**
	 * Updates the design.
	 * @param design New design to set.
	 */
	public void setDesign(Design design){
		this.design = design;
		scene.setCurrDesign(this.design);
		scene.init();
	}
	
	/**
	 * Moves the cursor to a new tile in the tile array.
	 * @param tile The new tile to move the cursor to.
	 */
	public void moveToTile(String tile){
		Tile t = design.getDevice().getTile(tile);
		int tileSize = scene.getTileSize();
		QSize size = this.frameSize();
		view.fitInView(scene.getDrawnTileX(t)*tileSize - size.width()/2,
				scene.getDrawnTileY(t)*tileSize - size.height()/2, 
				size.width(), size.height(), AspectRatioMode.KeepAspectRatio);
		view.zoomIn(); view.zoomIn();
		view.zoomIn(); view.zoomIn();		
		scene.updateCurrXY(scene.getDrawnTileX(t), scene.getDrawnTileY(t));
		scene.updateCursor();
	}
}
