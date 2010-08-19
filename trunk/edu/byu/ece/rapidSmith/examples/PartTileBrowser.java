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

import java.util.ArrayList;

import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.Qt.DockWidgetArea;
import com.trolltech.qt.core.Qt.ItemDataRole;
import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QDockWidget;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QProgressDialog;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QDockWidget.DockWidgetFeature;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class is an example of how RapidSmith could be used to build
 * interactive tools using Qt or other GUI packages.  This class
 * creates a zoom-able 2D array of the tiles found in the devices installed 
 * with RapidSmith.  This example requires the Qt Jambi (Qt for Java)
 * jars to run.
 * @author marc
 */
public class PartTileBrowser extends QMainWindow {

	private PartTileBrowserView view;
	private QLabel statusLabel;
	private PartTileBrowserScene scene;
	Device device;
	private String currPart;
	private QTreeWidget treeWidget;

	public static void main(String[] args) {
		QApplication.initialize(args);
		PartTileBrowser testPTB = new PartTileBrowser(null);
		testPTB.show();
		QApplication.exec();
	}

	public PartTileBrowser(QWidget parent) {
		super(parent);
		setWindowTitle("Part Tile Browser");

		createTreeView();
		ArrayList<String> parts = FileTools.getAvailableParts();
		if(parts.size() < 1){
			MessageGenerator.briefErrorAndExit("Error: No available parts. Please generate part database files.");
		}
		currPart = parts.get(0);
		device = FileTools.loadDevice(currPart);
		
		scene = new PartTileBrowserScene(device);

		view = new PartTileBrowserView(scene);

		setCentralWidget(view);
		

		
		scene.updateStatus.connect(this, "updateStatus()");
		statusLabel = new QLabel("Status Bar");
		statusLabel.setText("Status Bar");
		QStatusBar statusBar = new QStatusBar();
		statusBar.addWidget(statusLabel);
		setStatusBar(statusBar);

	}

	private void createTreeView() {
		treeWidget = new QTreeWidget();
		treeWidget.setColumnCount(1);
		treeWidget.setHeaderLabel("Select a part...");
		QTreeWidgetItem xc4 = new QTreeWidgetItem(treeWidget);
		xc4.setText(0, tr("Virtex4"));
		QTreeWidgetItem xc4vfx = new QTreeWidgetItem(xc4);
		xc4vfx.setText(0, tr("FX"));
		QTreeWidgetItem xc4vlx = new QTreeWidgetItem(xc4);
		xc4vlx.setText(0, tr("LX"));
		QTreeWidgetItem xc4vsx = new QTreeWidgetItem(xc4);
		xc4vsx.setText(0, tr("SX"));
		
		QTreeWidgetItem xc5 = new QTreeWidgetItem(treeWidget);
		xc5.setText(0, tr("Virtex5"));
		QTreeWidgetItem xc5vfx = new QTreeWidgetItem(xc5);
		xc5vfx.setText(0, tr("FX"));
		QTreeWidgetItem xc5vlx = new QTreeWidgetItem(xc5);
		xc5vlx.setText(0, tr("LX"));
		QTreeWidgetItem xc5vsx = new QTreeWidgetItem(xc5);
		xc5vsx.setText(0, tr("SX"));
		QTreeWidgetItem xc5vtx = new QTreeWidgetItem(xc5);
		xc5vtx.setText(0, tr("TX"));
		
		for(String partName : FileTools.getAvailableParts()){
			QTreeWidgetItem partItem= null;
			if(partName.startsWith("xc4vfx"))
				partItem = new QTreeWidgetItem(xc4vfx);
			else if(partName.startsWith("xc4vlx"))
				partItem = new QTreeWidgetItem(xc4vlx);
			else if(partName.startsWith("xc4vsx"))
				partItem = new QTreeWidgetItem(xc4vsx);
			else if(partName.startsWith("xc5vfx"))
				partItem = new QTreeWidgetItem(xc5vfx);
			else if(partName.startsWith("xc5vlx"))
				partItem = new QTreeWidgetItem(xc5vlx);
			else if(partName.startsWith("xc5vsx"))
				partItem = new QTreeWidgetItem(xc5vsx);
			else if(partName.startsWith("xc5vtx"))
				partItem = new QTreeWidgetItem(xc5vtx);
			else
				partItem = new QTreeWidgetItem(treeWidget);
	        partItem.setText(0, tr(partName));
	        partItem.setData(0, ItemDataRole.AccessibleDescriptionRole, partName);
		}
		
		treeWidget.doubleClicked.connect(this,"showPart(QModelIndex)");
		
		QDockWidget dockWidget = new QDockWidget(tr("Part Browser"), this);
		dockWidget.setAllowedAreas(DockWidgetArea.LeftDockWidgetArea);
		dockWidget.setWidget(treeWidget);
		dockWidget.setFeatures(DockWidgetFeature.NoDockWidgetFeatures);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget);
	}

	@SuppressWarnings("unused")
	private void showPart(QModelIndex qmIndex){
		Object data = qmIndex.data(ItemDataRole.AccessibleDescriptionRole);
		if( data != null){
			if(currPart.equals(data))
				return;
			currPart = (String) data;
			QProgressDialog progress = new QProgressDialog("Loading "+currPart.toUpperCase()+"...", "", 0, 100, this);
			progress.setWindowTitle("Load Progress");
			progress.setWindowModality(WindowModality.WindowModal);
			progress.setCancelButton(null);
			progress.show();
			progress.setValue(10);
			
			device = FileTools.loadDevice(currPart);
			progress.setValue(100);
			scene.setDevice(device);
			statusLabel.setText("Loaded: "+currPart.toUpperCase());

			
		}
	}
	void updateStatus() {
		int x = (int) scene.getCurrX();
		int y = (int) scene.getCurrY();
		if (x >= 0 && x < device.getColumns() && y >= 0 && y < device.getRows()){
			String tileName = device.getTile(y, x).getName();
			statusLabel.setText("Part: "+currPart.toUpperCase() +"  Tile: "+ tileName+" ("+x+","+y+")");
		}
	}

}
