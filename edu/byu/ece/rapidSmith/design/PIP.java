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
package edu.byu.ece.rapidSmith.design;

import java.io.Serializable;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

/**
 * This class represents the programmable-interconnect-points (PIPs) as found 
 * in XDL designs.  The wire names are stored as integers using the WireEnumerator
 * found in the edu.byu.ece.rapidSmith.device class.
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class PIP implements Comparable<Object>, Serializable{

	private static final long serialVersionUID = 122367735864726588L;
	/** The tile where this PIP is located */
	private Tile tile;
	/** The beginning wire of the PIP (ex: INT_X1Y1 START_WIRE -> END_WIRE) */
	private int startWire;
	/** The ending wire of the PIP (ex: INT_X1Y1 START_WIRE -> END_WIRE) */
	private int endWire;
	
	/**
	 * Creates an empty PIP.
	 */
	public PIP(){
		this.tile = null;
		this.startWire = -1;
		this.endWire = -1;
	}

	/**
	 * Constructor that creates a duplicate PIP from parameters.
	 * @param tile The tile of the PIP.
	 * @param startWire The start wire of the PIP.
	 * @param endWire The end wire of the PIP.
	 */
	public PIP(Tile tile, int startWire, int endWire){
		this.tile = tile;
		this.startWire = startWire;
		this.endWire = endWire;
	}
	
	/**
	 * Gets and returns the tile of this PIP.
	 * @return The tile where this PIP resides.
	 */
	public Tile getTile(){
		return tile;
	}

	/**
	 * Sets the tile where this PIP resides.  Useful for moving the PIP.
	 * @param tile The new tile of this PIP.
	 */
	public void setTile(Tile tile){
		this.tile = tile;
	}

	/**
	 * Gets the integer start wire of this PIP.
	 * @return The integer start wire.
	 */
	public int getStartWire(){
		return startWire;
	}

	/**
	 * Gets the name of the start wire of the PIP.
	 * @param we The wire enumerator corresponding to this FPGA family. This
	 * is needed for the name lookup.
	 * @return The name of the start wire of this PIP or null if invalid.
	 */
	public String getStartWireName(WireEnumerator we){
		return we.getWireName(startWire);
	}
	
	/**
	 * Gets the name of the end wire of the PIP.
	 * @param we The wire enumerator corresponding to this FPGA family. This
	 * is needed for the name lookup.
	 * @return The name of the end wire of this PIP or null if invalid.
	 */
	public String getEndWireName(WireEnumerator we){
		return we.getWireName(endWire);
	}

	/**
	 * Sets the start wire of this PIP.
	 * @param startWire the startWire to set.
	 */
	public void setStartWire(int startWire){
		this.startWire = startWire;
	}

	/**
	 * Gets and returns the end wire integer of this PIP.
	 * @return the endWire of this PIP.
	 */
	public int getEndWire(){
		return endWire;
	}

	/**
	 * This method will return an array of all possible wire connections that
	 * can be made from the start wire of this PIP.  Keep in mind that some 
	 * of the wire connections that leave the tile are not PIPs.
	 * @return An array of all possible end wire connections from the PIP's 
	 * start wire.   
	 */
	public WireConnection[] getAllPossibleEndWires(){
		return tile.getWireConnections(startWire);
	}
	
	/**
	 * Sets the end wire of this PIP.
	 * @param endWire the endWire to set.
	 */
	public void setEndWire(int endWire){
		this.endWire = endWire;
	}

	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + endWire;
		result = prime * result + startWire;
		result = prime * result + ((tile == null) ? 0 : tile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		PIP other = (PIP) obj;
		if(endWire != other.endWire)
			return false;
		if(startWire != other.startWire)
			return false;
		if(tile == null){
			if(other.tile != null)
				return false;
		}
		else if(!tile.equals(other.tile))
			return false;
		return true;
	}
	
	public int compareTo(Object o) throws ClassCastException {
		if (!(o instanceof PIP))
			throw new ClassCastException("This is not a design.PIP object, cannot compare properly.");
		PIP pip = ((PIP) o);  
		return (this.tile.getName() + this.getStartWire() + this.getEndWire()).compareTo(pip.tile.getName()+pip.getStartWire()+pip.getEndWire());
	}

	/**
	 * Creates a string representation of the PIP using the WireEnumerator
	 * class.  This method may be slower than toString(WireEnumerator).  
	 * @return An XDL-compatible string of the PIP.
	 */
	public String toString(){
		WireEnumerator we = tile.getDevice().getWireEnumerator();
		return "  pip " + tile.getName() + " " + we.getWireName(startWire) +
		" -> " + we.getWireName(endWire) + " ,  \n";		
	}
	
	/**
	 * Creates a string representation of the PIP using the WireEnumerator
	 * class.
	 * @param we The wire enumerator used for wire name translation from integers.
	 * @return An XDL-compatible string of the PIP.
	 */
	public String toString(WireEnumerator we){
		return "  pip " + tile.getName() + " " + we.getWireName(startWire) +
		" -> " + we.getWireName(endWire) + " ,  \n";		
	}
}
