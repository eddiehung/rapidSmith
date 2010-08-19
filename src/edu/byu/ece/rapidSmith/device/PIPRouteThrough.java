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

/**
 * This class is used to represent the Route-Through constructs of XDLRC/XDL
 * in association with PIPs.  Certain PIPs are defined as special configurations
 * of logic blocks which allow routing to pass through them.  This class defines
 * these kinds of routing constructs.
 * @author Chris Lavin
 */
public class PIPRouteThrough implements Serializable{

	private static final long serialVersionUID = 4112516033821686373L;
	/** The type of primitive where a route through exists */
	private PrimitiveType type;
	/** The input wire of the route through */
	private int inWire;
	/** The output wire of the route through */
	private int outWire;
	
	/**
	 * Constructor which creates a new PIPRouteThrough.
	 * @param type The type of primitive involved in the route through.
	 * @param inWire The input wire.
	 * @param outWire The output wire.
	 */
	public PIPRouteThrough(PrimitiveType type, int inWire, int outWire){
		this.type = type;
		this.inWire = inWire;
		this.outWire = outWire;
	}

	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + inWire;
		result = prime * result + outWire;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PIPRouteThrough other = (PIPRouteThrough) obj;
		if (inWire != other.inWire)
			return false;
		if (outWire != other.outWire)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/**
	 * Gets and returns the primitive type where this route through is found.
	 * @return The primitive type of this route through.
	 */
	public PrimitiveType getType(){
		return type;
	}

	/**
	 * Sets the type of primitive for this route through.
	 * @param type the type to set.
	 */
	public void setType(PrimitiveType type){
		this.type = type;
	}

	/**
	 * Gets and returns the input wire for this route through.
	 * @return the inWire The input wire for this route through.
	 */
	public int getInWire(){
		return inWire;
	}

	/**
	 * Sets the input wire for this route through.
	 * @param inWire the inWire to set.
	 */
	public void setInWire(int inWire){
		this.inWire = inWire;
	}

	/**
	 * Gets and returns the output wire for this route through.
	 * @return the inWire The output wire for this route through.
	 */
	public int getOutWire(){
		return outWire;
	}

	/**
	 * Sets the output wire for this route through.
	 * @param outWire the outWire to set
	 */
	public void setOutWire(int outWire){
		this.outWire = outWire;
	}
	
	/**
	 * Creates an XDLRC compatible string of this route through. 
	 * @param we The wire enumerator required to convert wires to names.
	 * @return The XDLRC compatible string of this route through.
	 */
	public String toString(WireEnumerator we){
		return this.type.toString() + "-" + we.getWireName(inWire) + "-" + we.getWireName(outWire);
 	}
}
