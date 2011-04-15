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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.helper.WireHashMap;


/**
 * This class represents the same information found and described in XDLRC files concerning
 * Xilinx FPGA tiles.  The representation given by XDLRC files is that every FPGA is described
 * as a 2D array of Tiles, each with a set of primitive sites, and hence sources, sinks and wires
 * to connect tiles together and wires within. 
 * @author Chris Lavin
 * Created on: Apr 22, 2010
 */
public class Tile implements Serializable{
	/** Allows Serializable functionality  */
	private static final long serialVersionUID = 2470255220468954877L;
	/** XDL Name of the tile */
	private String name;
	/** XDL Tile Type (INT,CLB,...)*/
	private TileType type;
	/** This is a list of the sinks within the tile (generally in the primitives) */
	private HashMap<Integer,SinkPin> sinks;
	/** This is a list of the sources within the tile (generally in the primitives) */
	private int[] sources;
	/** This variable holds all the wires and their connections within the tile */
	private WireHashMap wireConnections;
	/** An array of primitiveSites located within the tile (null if none) */
	private PrimitiveSite[] primitiveSites;
	/** Absolute tile row number - the index into the device Tiles[][] array */
	private int row;
	/** Absolute tile column number - the index into the device Tiles[][] array */
	private int column;
	/** This is the Y coordinate in the tile name (ex: 5 in INT_X0Y5) */
	private int tileYCoordinate;
	/** This is the X coordinate in the tile name (ex: 0 in INT_X0Y5) */
	private int tileXCoordinate;
	/** Reference to this tile's device object */
	private Device dev;

	/**
	 * Constructor for the tile class, initializes all the private variables to empty 
	 * data structures.
	 */
	public Tile(){
		sinks = null;
		sources = null;
		wireConnections = null;
		sinks = null;
		dev = null;
	}
	
	/**
	 * Sets the device which owns this tile.
	 * @param device The device to set
	 */
	public void setDevice(Device device){
		this.dev = device;
	}

	/**
	 * Gets the device to which this tile belongs.
	 * @return The device to which this tile belongs.
	 */
	public Device getDevice(){
		return dev;
	}

	/**
	 * Gets and returns the HashMap containing the sinks for this tile.  The keys are
	 * the actual sink wires and the values are the SinkPin objects.
	 * @return The HashMap of sink wire mappings in this tile.
	 */
	public HashMap<Integer,SinkPin> getSinks(){
		return sinks;
	}

	/**
	 * Gets the actual sink pin object from the sink wire.
	 * @param sink The sink wire.
	 * @return The sink pin object based on the given sink wire.
	 */
	public SinkPin getSinkPin(Integer sink){
		return sinks==null? null : sinks.get(sink);
	}
	
	/**
	 * This is used to populate the tile sinks and should probably not be called 
	 * during normal usage.
	 * @param sinks The new sinks to set for this tile.
	 */
	public void setSinks(HashMap<Integer,SinkPin> sinks){
		this.sinks = sinks;
	}
	
	/**
	 * Gets and returns the sources of all the primitive sites in this tile.
	 * @return The source wires found in this tile.
	 */
	public int[] getSources(){
		return sources;
	}

	/**
	 * This is used to populate the tile sources and should probably not be called 
	 * during normal usage.
	 * @param sources The new sources to set for this tile.
	 */
	public void setSources(int[] sources){
		this.sources = sources;
	}

	/**
	 * Gets and returns the wires HashMap for this tile.
	 * @return The wires HashMap for this tile.
	 */
	public WireHashMap getWireHashMap(){
		return wireConnections;
	}

	public Set<Integer> getWires(){
		return wireConnections.keySet();
	}
	
	/**
	 * This will get all of the wire connections that can be 
	 * made from the given wire in this tile.
	 * @param wire A wire in this tile to query its potential connections.
	 * @return An array of wires which connect to the given wire.
	 */
	public WireConnection[] getWireConnections(int wire){
		return wireConnections.get(wire);
	}
	
	/**
	 * This is used to populate the tile wires and should probably not be called 
	 * during normal usage.
	 * @param wires The new wires to set for this tile.
	 */
	public void setWireHashMap(WireHashMap wires){
		this.wireConnections = wires;
	}

	/**
	 * Sets the primitive sites present in this tile, should not be called during 
	 * normal usage.
	 * @param primitiveSites The new primitive sites.
	 */
	public void setPrimitiveSites(PrimitiveSite[] primitiveSites){
		this.primitiveSites = primitiveSites;
	}

	/**
	 * Gets and returns the primitive site array for this tile.
	 * @return An array of primitive sites present in this tile.
	 */
	public PrimitiveSite[] getPrimitiveSites(){
		return primitiveSites;
	}

	/**
	 * Used to compile the sinks for this tile during parsing, should not be called
	 * during normal usage.
	 * @param sink The new sink to add. The SinkPin created is initialized to -1,0.
	 */
	public void addSink(int sink){
		sinks.put(sink, new SinkPin(-1,0));
	}

	/**
	 * Used to compile the sources for this tile during parsing, should not be called
	 * during normal usage.
	 * @param source The new source to add.
	 */
	public void addSource(int source){
		if(this.sources == null){
			int[] tmp = new int[1];
			tmp[0] = source;
			this.sources = tmp;
		}
		else{
			int i;
			int[] tmp = new int[this.sources.length+1];
			for(i=0; i < this.sources.length; i++){
				tmp[i] = sources[i];
			}
			tmp[i] = source;
			this.sources = tmp;
		}
	}
	
	/**
	 * This method adds a key/value pair to the wires HashMap.
	 * @param src The wire (or key) of the HashMap to add.
	 * @param dest The actual wire to add to the value or Wire[] in the HashMap.
	 */
	public void addConnection(int src, WireConnection dest){
		// Add the wire if it doesn't already exist
		if(this.wireConnections.get(src) == null){
			WireConnection[] tmp = {dest};
			this.wireConnections.put(src, tmp);
		}
		else{
			WireConnection[] currentConnections = this.wireConnections.get(src);
			WireConnection[] tmp = new WireConnection[currentConnections.length+1];
			int i;
			for(i=0; i < currentConnections.length; i++){
				tmp[i] = currentConnections[i];
			}
			tmp[i] = dest;
			Arrays.sort(tmp);
			this.wireConnections.put(src, tmp);
		}
	}
	
	/**
	 * Checks if this tile contains a pip with the same connection
	 * as that provided.
	 * @param pip The pip connection to look for.
	 * @return True if the connection exists in this tile, false otherwise.
	 */
	public boolean hasPIP(PIP pip){
		return hasConnection(pip.getStartWire(), pip.getEndWire());
	}
	
	private boolean hasConnection(int startWire, int endWire){
		WireConnection[] wireConns = wireConnections.get(startWire);
		if(wireConns == null || wireConns.length == 0){
			return false;
		}
		for(WireConnection wc : wireConns){
			if(wc.getWire() == endWire && wc.isPIP()){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets the name of the tile (XDL name, such as INT_X0Y5).
	 * @param name The new name of the tile.
	 */
	public void setName(String name){
		// Set the name
		this.name = name;
		
		if(!name.contains("_X")){
			return;
		}
		
		// Populate the X and Y coordinates based on name
		int i = name.length();
		int end = i;
		
		while(name.charAt(i-1) != 'Y'){i--;}
		this.tileYCoordinate = Integer.parseInt(name.substring(i,end));  
		end = i - 1;
		
		while(name.charAt(i-1) != 'X'){i--;}
		this.tileXCoordinate = Integer.parseInt(name.substring(i,end));		
	}

	/**
	 * Gets and returns the name (XDL name) of the tile (such as INT_X0Y5).
	 * @return the name The XDL name of the tile.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Gets and returns the XDL tile name suffix (such as "_X0Y5"). 
	 * @return The tile coordinate name suffix with underscore.
	 */
	public String getTileNameSuffix(){
		return name.substring(name.lastIndexOf('_'));
	}

	/**
	 * Sets the type of tile this is.
	 * @param type The new type to set.
	 */
	public void setType(TileType type){
		this.type = type;
	}

	/**
	 * Gets the type of tile this is.
	 * @return the type The current type of this tile.
	 */
	public TileType getType(){
		return type;
	}

	/**
	 * The absolute row index (0 starting at top)
	 * @param row the row to set
	 */
	public void setRow(int row){
		this.row = row;
	}

	/**
	 * The absolute row index (0 starting at top)
	 * @return the row
	 */
	public int getRow(){
		return row;
	}

	/**
	 * The absolute column index (0 starting at the left)
	 * @param column the column to set
	 */
	public void setColumn(int column){
		this.column = column;
	}

	/**
	 * The absolute column index (0 starting at the left)
	 * @return the column
	 */
	public int getColumn(){
		return column;
	}

	/**
	 * Gets a unique integer address for this tile (useful for representing a tile
	 * as a single integer).
	 * @return The unique integer address of this tile.
	 */
	public int getUniqueAddress(){
		return dev.getColumns()*this.row + this.column; 
	}
	
	/**
	 * This is the Y Coordinate in the tile name (the 5 in INT_X0Y5)
	 * @return the tileRow
	 */
	public int getTileYCoordinate(){
		return tileYCoordinate;
	}

	/**
	 * This is the X Coordinate in the tile name (the 0 in INT_X0Y5)
	 * @return the tileColumn
	 */
	public int getTileXCoordinate(){
		return tileXCoordinate;
	}
	
	/**
	 * This method will create a new list of PIPs from those existing
	 * in the instance of this Tile object.  Use this method with caution as
	 * it will generate a lot of new PIPs each time it is called which may (or
	 * may not) use a lot of memory.
	 * @return A list of all PIPs in this tile.
	 */
	public ArrayList<PIP> getPIPs(){
		ArrayList<PIP> pips = new ArrayList<PIP>();
		for(Integer startWire : wireConnections.keySet()){
			for(WireConnection endWire : wireConnections.get(startWire)){
				if(endWire.isPIP()){
					pips.add(new PIP(this, startWire, endWire.getWire()));
				}
			}
		}
		return pips;
	}
	
	/**
	 * Calculates the Manhattan distance between this tile and the given tile.
	 * It calculates the distance based on tileXCoordinate and tileYCoordinate
	 * rather than the absolute indices of the tile.
	 * @param tile The tile to compare against.
	 * @return The integer Manhattan distance between this and the given tile.
	 */
	public int getManhattanDistance(Tile tile){
		return Math.abs(tile.tileXCoordinate - tileXCoordinate) +
			   Math.abs(tile.tileYCoordinate - tileYCoordinate);
	}

	@Override
	public String toString(){
		return getName();
	}
	
	@Override
	public boolean equals(Object obj){
		if (this==obj)
			return true;
		else if( (obj == null) || (obj.getClass() != this.getClass())){
			return false;
		}
		Tile tile = (Tile) obj;
		return (tile.getName() == this.getName());
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}
}
