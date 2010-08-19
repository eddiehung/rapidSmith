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
package edu.byu.ece.rapidSmith.examples;

import com.trolltech.qt.core.QPoint;
import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.core.Qt.Key;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsView;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QMouseEvent;
import com.trolltech.qt.gui.QResizeEvent;
import com.trolltech.qt.gui.QWheelEvent;

/**
 * @author marc
 *
 */
public class PartTileBrowserView extends QGraphicsView {

	QPointF currCenter;
	QPoint lastPan;
	private boolean rightPressed;
	double zoomMin;
	double zoomMax;

	public PartTileBrowserView(QGraphicsScene scene) {
		super(scene);
		setCenter(new QPointF(sceneRect().right() / 2, sceneRect().bottom() / 2));
		zoomMin = 0.05;
		zoomMax = 30;
	}

	public void setCenter(QPointF centerPoint) {
		// Get the rectangle of the visible area in scene coords
		QRectF visibleArea = mapToScene(rect()).boundingRect();

		// Get the scene area
		QRectF sceneBounds = sceneRect();

		double boundX = visibleArea.width() / 2.0;
		double boundY = visibleArea.height() / 2.0;
		double boundWidth = sceneBounds.width() - 2.0 * boundX;
		double boundHeight = sceneBounds.height() - 2.0 * boundY;

		// The max boundary that the centerPoint can be to
		QRectF bounds = new QRectF(boundX, boundY, boundWidth, boundHeight);

		if (bounds.contains(centerPoint)) {
			// We are within the bounds
			currCenter = centerPoint;
		} else {
			// We need to clamp or use the center of the screen
			if (visibleArea.contains(sceneBounds)) {
				// Use the center of scene ie. we can see the whole scene
				currCenter = sceneBounds.center();
			} else {

				currCenter = centerPoint;

				// We need to clamp the center. The centerPoint is too large
				if (centerPoint.x() > bounds.x() + bounds.width()) {
					currCenter.setX(bounds.x() + bounds.width());
				} else if (centerPoint.x() < bounds.x()) {
					currCenter.setX(bounds.x());
				}

				if (centerPoint.y() > bounds.y() + bounds.height()) {
					currCenter.setY(bounds.y() + bounds.height());
				} else if (centerPoint.y() < bounds.y()) {
					currCenter.setY(bounds.y());
				}

			}
		}

		// Update the scrollbars
		centerOn(currCenter);
	}

	public QPointF getCenter() {
		return currCenter;
	}

	public void mousePressEvent(QMouseEvent event) {
		if (event.button().equals(Qt.MouseButton.RightButton)) {
			// For panning the view
			rightPressed = true;
			lastPan = event.pos();
			setCursor(new QCursor(CursorShape.ClosedHandCursor));
		}
		super.mousePressEvent(event);
	}

	public void mouseReleaseEvent(QMouseEvent event) {
		if (event.button().equals(Qt.MouseButton.RightButton)) {
			rightPressed = false;
			setCursor(new QCursor(CursorShape.ArrowCursor));
		}
		super.mouseReleaseEvent(event);
	}

	public void mouseMoveEvent(QMouseEvent event) {
		if (rightPressed) {
			if (lastPan != null && !lastPan.isNull()) {
				// Get how much we panned
				QPointF s1 = mapToScene(new QPoint((int) lastPan.x(),
						(int) lastPan.y()));
				QPointF s2 = mapToScene(new QPoint((int) event.pos().x(),
						(int) event.pos().y()));
				QPointF delta = new QPointF(s1.x() - s2.x(), s1.y() - s2.y());
				lastPan = event.pos();

				// Update the center ie. do the pan
				setCenter(new QPointF(getCenter().x() + delta.x(), getCenter()
						.y()
						+ delta.y()));
			}
		}
		super.mouseMoveEvent(event);
	}

	public void wheelEvent(QWheelEvent event) {
		// Get the position of the mouse before scaling, in scene coords
		QPointF pointBeforeScale = mapToScene(event.pos());

		// Get the original screen centerpoint
		QPointF screenCenter = getCenter();

		// Scale the view ie. do the zoom
		double scaleFactor = 1.15; // How fast we zoom
		if (event.delta() > 0) {
			// Zoom in (if not at limit)
			if(this.matrix().m11() < zoomMax)
				scale(scaleFactor, scaleFactor);
		} else {
			// Zoom out (if not at limit)
			if(this.matrix().m11() > zoomMin)
				scale(1.0 / scaleFactor, 1.0 / scaleFactor);
		}

		// Get the position after scaling, in scene coords
		QPointF pointAfterScale = mapToScene(event.pos());

		// Get the offset of how the screen moved
		QPointF offset = new QPointF(
				pointBeforeScale.x() - pointAfterScale.x(), pointBeforeScale
						.y()
						- pointAfterScale.y());

		// Adjust to the new center for correct zooming
		QPointF newCenter = new QPointF(screenCenter.x() + offset.x(),
				screenCenter.y() + offset.y());
		setCenter(newCenter);
	}

	public void resizeEvent(QResizeEvent event) {
		// Get the rectangle of the visible area in scene coords
		QRectF visibleArea = mapToScene(rect()).boundingRect();
		setCenter(visibleArea.center());
		// Call the subclass resize so the scrollbars are updated correctly
		super.resizeEvent(event);
	}
	
	public void keyPressEvent(QKeyEvent event){
		double scaleFactor = 1.15; 
		if (event.key() == Key.Key_Equal.value()) {
			// Zoom in (if not at limit)
			if(this.matrix().m11() < zoomMax)
				scale(scaleFactor, scaleFactor);
		} else if(event.key() == Key.Key_Minus.value()){
			// Zoom out (if not at limit)
			if(this.matrix().m11() > zoomMin)
				scale(1.0 / scaleFactor, 1.0 / scaleFactor);
		}		
	}
}

