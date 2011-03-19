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

import java.io.Serializable;
import java.util.Arrays;


/**
 * A helper class to help remove duplicate objects and reduce memory usage and file
 * size of the Device class. 
 * @author Chris Lavin
 */
public class TileWires implements Serializable{

	private static final long serialVersionUID = 1884345540813741720L;
	/** Unique set of wires which belong to one or more tiles */
	public WireHashMap wires;

	/**
	 * Constructor 
	 * @param wires The wires of a tile.
	 */
	public TileWires(WireHashMap wires){
		this.wires = wires;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		if(wires == null){
			return hash;
		}
		Integer[] keys = new Integer[this.wires.keySet().size()];
		keys = this.wires.keySet().toArray(keys);
		Arrays.sort(keys);
		for(Integer i : keys){
			hash += i * 7;
			if(this.wires.get(i) != null){
				hash += Arrays.deepHashCode(this.wires.get(i)) * 13;
			}
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileWires other = (TileWires) obj;
		if (wires == null) {
			if (other.wires != null)
				return false;
		} 
		
		if(wires == null && other.wires == null){
			return true;
		}
		if(wires == null || other.wires == null){
			return false;
		}
		Integer[] keys = new Integer[wires.keySet().size()];
		Integer[] keys2 = new Integer[other.wires.keySet().size()];
		keys = wires.keySet().toArray(keys);
		keys2 = other.wires.keySet().toArray(keys2);
		Arrays.sort(keys);
		Arrays.sort(keys2);
		if(!Arrays.deepEquals(keys, keys2)){
			return false;
		}
		for(Integer key : keys){
			if(!Arrays.deepEquals(wires.get(key), other.wires.get(key))){
				return false;
			}
		}
		return true;
	}
}
