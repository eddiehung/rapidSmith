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
package edu.byu.ece.rapidSmith.device.helper;

import edu.byu.ece.rapidSmith.device.Wire;


/**
 * Small object class to help with backward edge removal.
 * @author Chris Lavin
 * Created on: Jul 15, 2010
 */
public class Connection{
	
	/** Start wire */
	private int wire;
	/** Destination wire */
	private Wire dest;
	
	public Connection(int wire, Wire dest){
		this.wire = wire;
		this.dest = dest;
	}

	/**
	 * @return the wire
	 */
	public int getWire(){
		return wire;
	}

	/**
	 * @return the dest
	 */
	public Wire getDestinationWire(){
		return dest;
	}
}
