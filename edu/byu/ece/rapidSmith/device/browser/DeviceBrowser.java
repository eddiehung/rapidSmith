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

import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.Qt.DockWidgetArea;
import com.trolltech.qt.core.Qt.ItemDataRole;
import com.trolltech.qt.core.Qt.SortOrder;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QDockWidget;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QDockWidget.DockWidgetFeature;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class creates an interactive Xilinx FPGA device browser for all of the
 * devices currently installed on RapidSmith.  It provides the user with a 2D view
 * of all tile array in the device.  Allows each tile to be selected (double click)
 * and populate the primitive site and wire lists.  Wire connections can also be drawn
 * by selecting a specific wire in the tile (from the list) and the program will draw
 * all connections that can be made from that wire.  The wire positions on the tile
 * are determined by a hash and are not related to FPGA Editor positions.   
 * @author Chris Lavin and Marc Padilla
 * Created on: Nov 26, 2010
 */
public class DeviceBrowser extends QMainWindow{
	/** The Qt View for the browser */
	private DeviceBrowserView view;
	/** The Qt Scene for the browser */
	private DeviceBrowserScene scene;
	/** The label for the status bar at the bottom */
	private QLabel statusLabel;
	/** The current device loaded */
	Device device;
	/** The current wire enumerator loaded */
	WireEnumerator we;
	/** The current part name of the device loaded */
	private String currPart;
	/** This is the tree of parts to select */
	private QTreeWidget treeWidget;
	/** This is the list of primitive sites in the current tile selected */
	private QTreeWidget primitiveList;
	/** This is the list of wires in the current tile selected */
	private QTreeWidget wireList;
	/** This is the current tile that has been selected */
	private Tile currTile = null;
	
	/**
	 * Main method setting up the Qt environment for the program to run.
	 * @param args
	 */
	public static void main(String[] args){
		QApplication.initialize(args);
		DeviceBrowser testPTB = new DeviceBrowser(null);
		testPTB.show();
		QApplication.exec();
	}

	/**
	 * Constructor which initializes the GUI and loads the first part found.
	 * @param parent The Parent widget, used to add this window into other GUIs.
	 */
	public DeviceBrowser(QWidget parent){
		super(parent);
		
		// set the title of the window
		setWindowTitle("Device Browser");
		
		initializeSideBar();
		
		// Gets the available parts in RapidSmith and populates the selection tree
		ArrayList<String> parts = FileTools.getAvailableParts();
		if(parts.size() < 1){
			MessageGenerator.briefErrorAndExit("Error: No available parts. " +
					"Please generate part database files.");
		}
		if(parts.contains("xc4vlx100ff1148")){
			currPart = "xc4vlx100ff1148";
		}
		else{
			currPart = parts.get(0);
		}
		
		device = FileTools.loadDevice(currPart);
		we = FileTools.loadWireEnumerator(currPart);
		
		// Setup the scene and view for the GUI
		scene = new DeviceBrowserScene(device, we);
		view = new DeviceBrowserView(scene);
		setCentralWidget(view);

		// Setup some signals for when the user interacts with the view
		scene.updateStatus.connect(this, "updateStatus()");
		scene.updateTile.connect(this, "updateTile()");
		
		// Initialize the status bar at the bottom
		statusLabel = new QLabel("Status Bar");
		statusLabel.setText("Status Bar");
		QStatusBar statusBar = new QStatusBar();
		statusBar.addWidget(statusLabel);
		setStatusBar(statusBar);
		
		// Set the opening default window size to 800x600 pixels
		resize(800, 600);
	}

	/**
	 * Populates the treeWidget with the various parts and families of devices
	 * currently available in this installation of RapidSmith.  It also creates
	 * the windows for the primitive site list and wire list.
	 */
	private void initializeSideBar(){
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
		
		QTreeWidgetItem xc6 = new QTreeWidgetItem(treeWidget);
		xc6.setText(0, tr("Virtex6"));
		QTreeWidgetItem xc6vcx = new QTreeWidgetItem(xc6);
		xc6vcx.setText(0, tr("CX"));
		QTreeWidgetItem xc6vhx = new QTreeWidgetItem(xc6);
		xc6vhx.setText(0, tr("HX"));
		QTreeWidgetItem xc6vlx = new QTreeWidgetItem(xc6);
		xc6vlx.setText(0, tr("LX"));
		QTreeWidgetItem xc6vsx = new QTreeWidgetItem(xc6);
		xc6vsx.setText(0, tr("SX"));
		
		QTreeWidgetItem xc6s = new QTreeWidgetItem(treeWidget);
		xc6s.setText(0, tr("Spartan6"));
		
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
			else if(partName.startsWith("xc6vcx"))				
				partItem = new QTreeWidgetItem(xc6vcx);
			else if(partName.startsWith("xc6vhx"))
				partItem = new QTreeWidgetItem(xc6vhx);			
			else if(partName.startsWith("xc6vlx"))
				partItem = new QTreeWidgetItem(xc6vlx);
			else if(partName.startsWith("xc6vsx"))
				partItem = new QTreeWidgetItem(xc6vsx);
			else if(partName.startsWith("xc6s"))
				partItem = new QTreeWidgetItem(xc6s);
			else
				partItem = new QTreeWidgetItem(treeWidget);
	        partItem.setText(0, tr(partName));
	        partItem.setData(0, ItemDataRole.AccessibleDescriptionRole, partName);
		}
		
		treeWidget.doubleClicked.connect(this,"showPart(QModelIndex)");
		
		QDockWidget dockWidget = new QDockWidget(tr("Part Browser"), this);
		dockWidget.setWidget(treeWidget);
		dockWidget.setFeatures(DockWidgetFeature.DockWidgetMovable);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget);
		
		// Create the primitive site list window
		primitiveList = new QTreeWidget();
		primitiveList.setColumnCount(1);
		ArrayList<String> headerList = new ArrayList<String>();
		headerList.add("Site");
		primitiveList.setHeaderLabels(headerList);
		primitiveList.setSortingEnabled(true);
		
		QDockWidget dockWidget2 = new QDockWidget(tr("Primitive List"), this);
		dockWidget2.setWidget(primitiveList);
		dockWidget2.setFeatures(DockWidgetFeature.DockWidgetMovable);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget2);
		
		// Create the wire list window
		wireList = new QTreeWidget();
		wireList.setColumnCount(2);
		ArrayList<String> headerList2 = new ArrayList<String>();
		headerList2.add("Wire");
		headerList2.add("Sink Connections");
		wireList.setHeaderLabels(headerList2);
		wireList.setSortingEnabled(true);
		QDockWidget dockWidget3 = new QDockWidget(tr("Wire List"), this);
		dockWidget3.setWidget(wireList);
		dockWidget3.setFeatures(DockWidgetFeature.DockWidgetMovable);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget3);

		// Draw wire connections when the wire name is double clicked
		wireList.doubleClicked.connect(this, "wireDoubleClicked(QModelIndex)");
	}
	
	/**
	 * This method will draw all of the wire connections based on the wire given.
	 * @param index The index of the wire in the wire list.
	 */
	public void wireDoubleClicked(QModelIndex index){
		scene.clearCurrentLines();
		if(currTile == null) return;
		int currWire = we.getWireEnum(index.data().toString());
		if(currWire < 0) return;
		if(currTile.getWireConnections(we.getWireEnum(index.data().toString())) == null) return;
		for(Wire wire : currTile.getWireConnections(we.getWireEnum(index.data().toString()))){
			scene.drawWire(currTile, currWire, wire.getTile(device, currTile), wire.getWire());
		}
	}
	
	/**
	 * This method gets called each time a user double clicks on a tile.
	 */
	protected void updateTile(){
		updatePrimitiveList();
		updateWireList();
	}
	
	/**
	 * This will update the primitive list window based on the current
	 * selected tile.
	 */
	protected void updatePrimitiveList(){
		int x = (int) scene.getCurrX();
		int y = (int) scene.getCurrY();
		if (x >= 0 && x < device.getColumns() && y >= 0 && y < device.getRows()){
			Tile tile = device.getTile(y, x);
			currTile = tile;
			primitiveList.clear();
			if(tile == null || tile.getPrimitiveSites() == null) return;
			for(PrimitiveSite ps : tile.getPrimitiveSites()){
				QTreeWidgetItem treeItem = new QTreeWidgetItem();
				treeItem.setText(0, ps.getName());
				primitiveList.insertTopLevelItem(0, treeItem);
			}
		}
	}

	/**
	 * This will update the wire list window based on the current
	 * selected tile.
	 */
	protected void updateWireList(){
		int x = (int) scene.getCurrX();
		int y = (int) scene.getCurrY();
		if (x >= 0 && x < device.getColumns() && y >= 0 && y < device.getRows()){
			Tile tile = device.getTile(y, x);		
			wireList.clear();
			if(tile == null || tile.getWires() == null) return;
			for(Integer wire : tile.getWires().keySet()) {
				QTreeWidgetItem treeItem = new QTreeWidgetItem();
				treeItem.setText(0, we.getWireName(wire));
				Wire[] connections = tile.getWireConnections(wire);
				treeItem.setText(1, String.format("%3d", connections == null ? 0 : connections.length));
				wireList.insertTopLevelItem(0, treeItem);
			}
			wireList.sortByColumn(0, SortOrder.AscendingOrder);
		}
	}

	/**
	 * This method loads a new device based on the part name selected in the 
	 * treeWidget.
	 * @param qmIndex The index of the part to load.
	 */
	protected void showPart(QModelIndex qmIndex){
		Object data = qmIndex.data(ItemDataRole.AccessibleDescriptionRole);
		if( data != null){
			if(currPart.equals(data))
				return;
			currPart = (String) data;			
			device = FileTools.loadDevice(currPart);
			we = FileTools.loadWireEnumerator(currPart);
			scene.setDevice(device, we);
			statusLabel.setText("Loaded: "+currPart.toUpperCase());
		}
	}
	
	/**
	 * Loads a device based on the given part name.
	 * @param partName Name of the device to load.
	 */
	protected void showPart(String partName){
		if(currPart.equals(partName))
			return;
		currPart = partName;			
		device = FileTools.loadDevice(currPart);
		we = FileTools.loadWireEnumerator(currPart);
		scene.setDevice(device, we);
		statusLabel.setText("Loaded: "+currPart.toUpperCase());
	}
	
	/**
	 * This method updates the status bar each time the mouse moves from a 
	 * different tile.
	 */
	protected void updateStatus(){
		int x = (int) scene.getCurrX();
		int y = (int) scene.getCurrY();
		if (x >= 0 && x < device.getColumns() && y >= 0 && y < device.getRows()){
			String tileName = device.getTile(y, x).getName();
			statusLabel.setText("Part: "+currPart.toUpperCase() +"  Tile: "+ tileName+" ("+x+","+y+")");
		}
	}
}
