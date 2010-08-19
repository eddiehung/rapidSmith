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
import java.util.HashMap;

/**
 * This class represents the primitive sites found in XDLRC files.  Theses
 * represent places on the FPGA where a particular instance of a primitive (or
 * XDL 'inst') can reside.
 * @author Chris Lavin
 */
public class PrimitiveSite implements Serializable{

	private static final long serialVersionUID = 4891980590392076374L;
	/** Name of the primitive instance with X and Y coordinate (ie. SLICE_X0Y0) */
	protected String name;
	/** The type of the instance */
	protected PrimitiveType type;
	/** The tile where this instance resides */
	protected Tile tile;
	/** Keeps track of all the in/out pins in the primitive with their wire enumeration value */
	protected HashMap<String,Integer> pins;
	/** A list of valid primitive types that can reside in a RAMBFIFO36 primitive site */
	public static java.util.HashSet<PrimitiveType> v5RAMBFIFO36Compatible;
	static{
		v5RAMBFIFO36Compatible = new java.util.HashSet<PrimitiveType>(8);
		v5RAMBFIFO36Compatible.add(PrimitiveType.FIFO36_72_EXP);
		v5RAMBFIFO36Compatible.add(PrimitiveType.FIFO36_EXP);
		v5RAMBFIFO36Compatible.add(PrimitiveType.RAMB18X2);
		v5RAMBFIFO36Compatible.add(PrimitiveType.RAMB18X2SDP);
		v5RAMBFIFO36Compatible.add(PrimitiveType.RAMB36SDP_EXP);
		v5RAMBFIFO36Compatible.add(PrimitiveType.RAMB36_EXP);
		v5RAMBFIFO36Compatible.add(PrimitiveType.RAMBFIFO18);
		v5RAMBFIFO36Compatible.add(PrimitiveType.RAMBFIFO18_36);
	}
	
	/**
	 * Constructor for a new PrimitiveSite
	 */
	public PrimitiveSite(){
		name = null;
		tile = null;
		pins = new HashMap<String,Integer>();
	}
	
	/**
	 * Gets and returns the name of this primitive site (ex: SLICE_X4Y6).
	 * @return The unique name of this primitive site.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Sets the name of this primitive site (ex: SLICE_X5Y7).
	 * @param name the name to set.
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Gets and returns the Tile where this primitive site resides.
	 * @return The tile where this primitive site is.
	 */
	public Tile getTile(){
		return tile;
	}
	
	/**
	 * Sets the tile where this primitive site is.
	 * @param location the tile location to set.
	 */
	public void setTile(Tile location){
		this.tile = location;
	}
	
	/**
	 * Gets the input/output pins of this primitive site and
	 * the internal to external name mappings.
	 * @return the pins of this primitive site.
	 */
	public HashMap<String, Integer> getPins(){
		return pins;
	}
	
	/**
	 * Sets the input/output pins of this primitive site and
	 * the internal to external name mappings. 
	 * @param pins The mapping of input/output pins from internal to 
	 * external names.
	 */
	public void setPins(HashMap<String, Integer> pins) {
		this.pins = pins;
	} 
	
	/**
	 * Adds a pin mapping to the primitive site.
	 * @param internalPinName The name of the wire internal to the 
	 * primitive site.
	 * @param externalPin The external wire integer of the mapping.
	 */
	public void addPin(String internalPinName, Integer externalPin){
		this.pins.put(internalPinName, externalPin);
	}
	
	/**
	 * Gets the external wire enumeration of the name of the wire corresponding to the internal wire
	 * name.
	 * @param internalName The internal wire name in the primitive.
	 * @return The corresponding external wire enum (Integer) name of the internal wire name. 
	 */
	public Integer getExternalPinName(String internalName){
		return this.pins.get(internalName);
	}
	
	/**
	 * Sets the native type of primitive of this primitive site.
	 * @param type the type to set.
	 */
	public void setType(PrimitiveType type){
		this.type = type;
	}
	/**
	 * Gets and returns the native type of primitive of this primitive site.
	 * @return the type
	 */
	public PrimitiveType getType(){
		return type;
	}

	/**
	 * This method will check if the PrimitiveType otherType can be placed
	 * at this primitive site.  Most often only if they are
	 * equal can this be true.  However there are a few special cases that require
	 * extra handling.  For example a SLICEL can reside in a SLICEM site but not 
	 * vice versa.  
	 * @param otherType The primitive type to try to place on this site.
	 * @return True if otherType can be placed at this primitive site, false otherwise.
	 */
	public boolean isCompatiblePrimitiveType(PrimitiveType otherType){
		if(type.equals(otherType)){
			return true;
		}
		else if(otherType.equals(PrimitiveType.SLICEL) && type.equals(PrimitiveType.SLICEM)){
			return true;
		}
		else if(otherType.equals(PrimitiveType.IOB) && 
				(type.equals(PrimitiveType.IOBM) || type.equals(PrimitiveType.IOBS))){
			return true;
		}
		else if(type.equals(PrimitiveType.RAMBFIFO36) && v5RAMBFIFO36Compatible.contains(otherType)){
			return true;
		}
		else if(type.equals(PrimitiveType.BUFGCTRL) && otherType.equals(PrimitiveType.BUFG)){
			return true;
		}
		return false;
	}
	
	/**
	 * This method gets the type of otherSite and calls the other method
	 * public boolean isCompatiblePrimitiveType(PrimitiveType otherType);
	 * See that method for more information.
	 * @param otherSite The other site to see if its type is compatible with this site.
	 * @return True if compatible, false otherwise.
	 */
	public boolean isCompatiblePrimitiveType(PrimitiveSite otherSite){
		return isCompatiblePrimitiveType(otherSite.type);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		PrimitiveSite other = (PrimitiveSite) obj;
		if(name == null){
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		if(type == null){
			if(other.type != null)
				return false;
		}
		else if(!type.equals(other.type))
			return false;
		return true;
	}
}
