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
package edu.byu.ece.rapidSmith.design.explorer;

import java.io.File;

import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.core.Qt.PenStyle;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsPathItem;
import com.trolltech.qt.gui.QGraphicsSceneHoverEvent;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QPainterPath;
import com.trolltech.qt.gui.QPen;
import com.trolltech.qt.gui.QSound;

import edu.byu.ece.rapidSmith.timing.PathDelay;

public class PathItem extends QGraphicsPathItem {
	/** Keeps a red pen handy for highlighting wire connections on mouse over */
	private static QPen highlighted  = new QPen(QColor.red, 0.75, PenStyle.SolidLine);
	/** Keeps a yellow pen for drawing the wire connections */
	private static QPen unHighlighted = new QPen(QColor.yellow, 0.75, PenStyle.SolidLine);
	
	private boolean selected = false;
	
	private PathDelay pd;
	
	public PathItem(QPainterPath path, PathDelay pd){
		super(path);
		this.setPath(pd);
	}

	@Override
	public void hoverEnterEvent(QGraphicsSceneHoverEvent event){
		this.setPen(highlighted);
		this.setZValue(this.zValue()+1);
		//QSound.play(DesignExplorer.rsrcPath + File.separator + "bugZapper.wav");
	}
	
	@Override
	public void hoverLeaveEvent(QGraphicsSceneHoverEvent event){
		if(!selected){
			this.setPen(unHighlighted);
			this.setZValue(this.zValue()-1);			
		}
	}
	
	public void setHighlighted(){
		
	}
	
	@Override
	public void mousePressEvent(QGraphicsSceneMouseEvent event){
		if(!event.button().equals(MouseButton.LeftButton)) return;
		if(selected){
			this.setPen(unHighlighted);
			selected = false;			
		}
		else{
			this.setPen(highlighted);
			selected = true;
		}
	}

	/**
	 * @param pd the pd to set
	 */
	public void setPath(PathDelay pd) {
		this.pd = pd;
	}

	/**
	 * @return the pd
	 */
	public PathDelay getPath() {
		return pd;
	}
}
