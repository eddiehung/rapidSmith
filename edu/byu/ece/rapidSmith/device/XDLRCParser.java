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
package edu.byu.ece.rapidSmith.device;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.byu.ece.rapidSmith.device.helper.WireHashMap;
import edu.byu.ece.rapidSmith.primitiveDefs.Connection;
import edu.byu.ece.rapidSmith.primitiveDefs.Element;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefPin;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDef;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefList;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.PartNameTools;
import edu.byu.ece.rapidSmith.util.StringPool;

/**
 * This class is designed to parse the XDLRC files to create the compact device
 * files used by XDL Tools.  It also extracts the primitive definitions from the 
 * XDLRC.  This parser is an improved version of the JavaCC parser which is no
 * longer in use.
 * @author Chris Lavin
 * Created on: Jul 7, 2010
 */
public class XDLRCParser{
	/** This counts the number of tiles that have been processed */
	private int processedTiles = 0;
	/** This is the device object to be populated */
	private Device dev;
	/** The corresponding wire enumerator for this device */
	private WireEnumerator we;
	/** The list of extracted primitive definitions */
	private PrimitiveDefList defs;
	/** The current tile that is being parsed */
	private Tile currTile;
	/** A list to keep track of primitive sites in the current tile */
	private ArrayList<PrimitiveSite> tilePrimitiveSites = new ArrayList<PrimitiveSite>(4);
	/** This is the file input stream for reading the XDLRC */
	private BufferedReader br;
	/** The current line buffer */
	private String line;
	/** The current line split into parts by whitespace */
	private List<String> parts;
	/** A collection of all unique Strings (to help save memory) */
	private StringPool pool;
	
	/** 
	 * General Constructor
	 */
	public XDLRCParser(){
		dev = new Device();
		pool = new StringPool();
	}
	
	/** 
	 * Reads a line and splits it into parts.
	 * @return The next line from the file, null if EOF.
	 */
	private String readLine(){
		try{
			line = br.readLine();
		}
		catch(IOException e){
			MessageGenerator.briefErrorAndExit("Error parsing XDLRC file.");
		}
		if(line != null){
			parts = split(line);
		}
		return line;
	}

	private static List<String> split(String line) {
		List<String> parts = new ArrayList<>();
		int startIndex = 0, endIndex;

		if (line.length() > 0 && line.charAt(0) == '\t') {
			parts.add("");
			startIndex = 1;
			while (line.length() > startIndex && line.charAt(startIndex) == '\t')
				startIndex++;
		}

		while (line.length() > startIndex) {
			endIndex = line.indexOf(" ", startIndex);
			if (endIndex == -1) {
				parts.add(line.substring(startIndex));
				break;
			} else if (endIndex != startIndex) {
				parts.add(line.substring(startIndex, endIndex));
			}
			startIndex = endIndex + 1;
		}

		return parts;
	}

	/**
	 * Parses the XDLRC Wire construct and populates connections and
	 * wires accordingly.
	 */
	private void parseWire(){
		int currTileWire = we.getWireEnum(parts.get(2));
		boolean tileWireIsSource =
				we.getWireType(currTileWire) == WireType.SITE_SOURCE ||
				we.isPIPSinkWire(currTileWire);
		int wireConnCount = Integer.parseInt(parts.get(3).replace(")", ""));
		for(int i = 0; i < wireConnCount; i++){
			readLine();
			int currWire = we.getWireEnum(parts.get(3).substring(0, parts.get(3).length() - 1));
			boolean currWireIsSiteSink = we.getWireType(currWire) == WireType.SITE_SINK;
			boolean currWireIsPIPSource = we.isPIPSourceWire(currWire);
			boolean currWireIsSink = currWireIsSiteSink || currWireIsPIPSource;
			if(tileWireIsSource || currWireIsSink) {
				Tile t = dev.getTile(parts.get(2));
				WireConnection wire = dev.wirePool.add(new WireConnection(currWire,
									currTile.getRow() - t.getRow(),
									currTile.getColumn() - t.getColumn(),
									false));
				currTile.addConnection(currTileWire,wire);
			}
		}
	}
	
	/**
	 * Parses the primitive_site construct in XDLRC and creates 
	 * the appropriate objects. 
	 */
	private void parsePrimitiveSite(){
		PrimitiveSite currPrimitiveSite = new PrimitiveSite();
		currPrimitiveSite.setTile(currTile);
		currPrimitiveSite.setName(parts.get(2));
		currPrimitiveSite.setType(Utils.createPrimitiveType(parts.get(3)));
		dev.primitiveSites.put(parts.get(2), currPrimitiveSite);
		int pinWireCount = Integer.parseInt(parts.get(5));
		for(int i = 0; i < pinWireCount; i++){
			readLine();
			String extPin = parts.get(4).substring(0, parts.get(4).length() - 1);
			currPrimitiveSite.addPin(pool.getUnique(parts.get(2)), we.getWireEnum(extPin));
			if(parts.get(3).equals("input")){
				currTile.addSink(we.getWireEnum(extPin));
			}
			else{
				currTile.addSource(we.getWireEnum(extPin));
			}
		}
		tilePrimitiveSites.add(currPrimitiveSite);
	}
	
	/**
	 * Parses the primitive_def construct in XDLRC and creates
	 * the appropriate objects.
	 */
	private void parsePrimitiveDef(){
		PrimitiveDef def = new PrimitiveDef();
		def.setType(Utils.createPrimitiveType(parts.get(2)));
		int pinCount = Integer.parseInt(parts.get(3));
		int elementCount = Integer.parseInt(parts.get(4));
		ArrayList<PrimitiveDefPin> pins = new ArrayList<PrimitiveDefPin>(pinCount);
		ArrayList<Element> elements = new ArrayList<Element>(elementCount);
		for(int i = 0; i < pinCount; i++){
			readLine();
			PrimitiveDefPin p = new PrimitiveDefPin();
			p.setExternalName(parts.get(2));
			p.setInternalName(parts.get(3));
			p.setOutput(parts.get(4).equals("output)"));
			pins.add(p);
		}
		for(int i = 0; i < elementCount; i++){
			readLine();
			Element e = new Element();
			e.setName(parts.get(2));
			int elementPinCount = Integer.parseInt(parts.get(3).replace(")", ""));
			e.setBel(parts.size() > 5 && parts.get(4).equals("#") && parts.get(5).equals("BEL"));
			
			for(int j = 0; j < elementPinCount; j++){
				readLine();
				PrimitiveDefPin elementPin = new PrimitiveDefPin();
				elementPin.setInternalName(parts.get(2));
				elementPin.setOutput(parts.get(3).equals("output)"));
				e.addPin(elementPin);
			}
			while(!readLine().startsWith("\t\t)")){
				if(line.startsWith("\t\t\t(cfg ")){
					for(int k = 2; k < parts.size(); k++){
						e.addCfgOption(parts.get(k).replace(")", ""));
					}
				}
				else if(line.startsWith("\t\t\t(conn ")){
					Connection c = new Connection();
					c.setElement0(parts.get(2));
					c.setPin0(parts.get(3));
					c.setForwardConnection(parts.get(4).equals("==>"));
					c.setElement1(parts.get(5));
					c.setPin1(parts.get(6).substring(0, parts.get(6).length() - 1));
					e.addConnection(c);
				}
			}
			elements.add(e);
		}
		def.setPins(pins);
		def.setElements(elements);
		defs.add(def);
	}
	
	/**
	 * Parses the XDLRC file specified by fileName and populates the Device 
	 * and optionally the PrimitiveDefList based on extractPrimitiveDefs.
	 * @param fileName Name of the XDLRC file to parse.
	 * @param extractPrimitiveDefs A flag to indicate if the parser should extract
	 * and create the primitiveDefsList.  This can be obtained through a getPrimitiveDefs()
	 * method after this method returns.
	 * @return The populated device.
	 */
	public Device parseXDLRC(String fileName, boolean extractPrimitiveDefs){
		try{
			br = new BufferedReader(new FileReader(fileName));
		}
		catch(FileNotFoundException e){
			MessageGenerator.briefErrorAndExit("ERROR: Could not find file: " + fileName);
		}
		
		while((line = readLine()) != null){
			/////////////////////////////////////////////////////////////////////
			//		(pip CLB_X1Y63 CIN0 -> XMUX_PINWIRE0 (_ROUTETHROUGH-CIN-XMUX SLICEM))
			/////////////////////////////////////////////////////////////////////
			if(line.startsWith("\t\t(pip ")){
				String endWire = null; 
				WireConnection currWire = null;
				if(parts.get(5).endsWith(")")){
					endWire = parts.get(5).substring(0, parts.get(5).length() - 1);
					currWire = dev.wirePool.add(new WireConnection(we.getWireEnum(endWire), 0, 0, true));
				}
				else{ // This is a route-through PIP
					endWire = parts.get(5);
					currWire = dev.wirePool.add(new WireConnection(we.getWireEnum(endWire), 0, 0, true));
					PrimitiveType type = Utils.createPrimitiveType(parts.get(7).substring(0, parts.get(7).length() - 2));
					
					String[] tokens = parts.get(6).split("-");
					int wire0 = we.getWireEnum(tokens[1]);
					int wire1 = we.getWireEnum(tokens[2]);

					PIPRouteThrough currRouteThrough = new PIPRouteThrough(type, wire0, wire1);
					currRouteThrough = dev.routeThroughPool.add(currRouteThrough);
				    dev.routeThroughMap.put(currWire, currRouteThrough);
				}
				currTile.addConnection(we.getWireEnum(parts.get(3)), currWire);
			}
			/////////////////////////////////////////////////////////////////////
			// 		(wire SECONDARY_LOGIC_OUTS7_INT 1
			/////////////////////////////////////////////////////////////////////
			else if(line.startsWith("\t\t(wire ")){
				parseWire();
			}
			/////////////////////////////////////////////////////////////////////
			//	(tile 1 48 CLB_X22Y63 CLB 4
			/////////////////////////////////////////////////////////////////////
			else if(line.startsWith("\t(tile ")){
				int row = Integer.parseInt(parts.get(2));
				int col = Integer.parseInt(parts.get(3));
				currTile = dev.getTile(row, col);
				currTile.setName(parts.get(4));
				currTile.setType(Utils.createTileType(parts.get(5)));
				
			  	int total = (dev.getRows()*dev.getColumns())/100;
			  	if(!(currTile.getRow() == 0 && currTile.getColumn() == 0)){
			  		System.out.printf("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
			  		System.out.printf("  %02d%% done parsing tiles...",processedTiles/total);
			  	}
			  	else{
			  		MessageGenerator.printHeader("Parsing XDLRC Tiles");
			  		System.out.println("    Part Name: " + dev.getPartName());
			  		System.out.println("    Tile Rows: " + dev.getRows());
			  		System.out.println("    Tile Cols: " + dev.getColumns());
			  		System.out.println("  Total Tiles: " + (dev.getColumns()*dev.getRows()));
			  		System.out.println();
			  	}
			}
			/////////////////////////////////////////////////////////////////////
			//		(primitive_site SLICE_X34Y126 SLICEM internal 34
			/////////////////////////////////////////////////////////////////////
			else if(line.startsWith("\t\t(primitive_site ")){
				parsePrimitiveSite();
			}
			/////////////////////////////////////////////////////////////////////
			//		(tile_summary INT_X22Y63 INT 3 598 3312)
			/////////////////////////////////////////////////////////////////////
			else if(line.startsWith("\t\t(tile_summary ")){
				// Create an array of primitive sites (more compact than ArrayList)
				if(tilePrimitiveSites.size() > 0){
					PrimitiveSite[] ps = new PrimitiveSite[tilePrimitiveSites.size()];
					for(int i=0; i < tilePrimitiveSites.size(); i++){
						ps[i] = tilePrimitiveSites.get(i);
					}
					currTile.setPrimitiveSites(ps);
				}
				else{
					currTile.setPrimitiveSites(null);
				}
				tilePrimitiveSites.clear();
				dev.incrementalRemoveDuplicateTileResources(currTile, we);
			  	processedTiles++;
			}
			else if(line.startsWith("(tiles ")){
				dev.setRows(Integer.parseInt(parts.get(1)));
				dev.setColumns(Integer.parseInt(parts.get(2)));
				dev.createTileArray();
				for(Tile[] tiles : dev.tiles){
					for(Tile tile : tiles){
						tile.setWireHashMap(new WireHashMap());
						tile.setSinks(new HashMap<Integer, SinkPin>());
					}
				}
				dev.populateTileMap(DeviceFilesCreator.createDeviceTileMap(dev.getPartName()));
			}
			else if(line.startsWith("(xdl_resource_report ")){
				dev.setPartName(PartNameTools.removeSpeedGrade(parts.get(2)));
				we = FileTools.loadWireEnumerator(parts.get(2));
			}
			else if(line.startsWith("(primitive_defs ")){
				// Switch to primitive_defs parsing while loop
				break;
			}
		}
		
		if(extractPrimitiveDefs){
			defs = new PrimitiveDefList();
			while((line = readLine()) != null){
				/////////////////////////////////////////////////////////////////////
				//	(primitive_def BSCAN 8 10
				/////////////////////////////////////////////////////////////////////
				if(line.startsWith("\t(primitive_def ")){
					parsePrimitiveDef();
				}
				else if(line.startsWith("(summary ")){
					break;
				}
			}			
		}

		dev.createWireConnectionEnumeration();
		dev.removeDuplicatePrimitivePinMaps();
		for(Tile t : dev.getTileMap().values()){
			t.setDevice(dev);
		}
		dev.populateSinkPins(we);
		dev.removeDuplicateTileSinks(we);
		dev.debugPoolCounts();
		try{
			br.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return dev;
	}
	
	/**
	 * Gets and returns the device.  This should only be called after parseXDLRC()
	 * is first called.
	 * @return The device corresponding to this parser.
	 */
	public Device getDevice(){
		return dev;
	}
	
	/**
	 * Gets and returns the PrimitiveDefList.  This should only be called after parseXDLRC()
	 * is first called.
	 * @return The Primitive definition list corresponding to this parser.
	 */
	public PrimitiveDefList getPrimitiveDefs(){
		return defs;
	}
}
