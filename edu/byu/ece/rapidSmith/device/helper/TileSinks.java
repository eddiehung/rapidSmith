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
import java.util.HashMap;

import edu.byu.ece.rapidSmith.device.SinkPin;

/**
 * A helper class to help reduce the memory usage and file size of
 * the Device class. 
 * @author Chris Lavin
 */
public class TileSinks implements Serializable{

	private static final long serialVersionUID = -4542976263775993364L;
	/** Sinks and mappings for the tile */
	public HashMap<Integer,SinkPin> sinks;

	/**
	 * Constructor
	 * @param sinks Mappings for this tileSink.
	 */
	public TileSinks(HashMap<Integer,SinkPin> sinks){
		this.sinks = sinks;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hash = 0;
		
		if(sinks == null){
			return hash;
		}
		else{
			Integer[] keys = new Integer[sinks.keySet().size()];
			keys = sinks.keySet().toArray(keys);
			Arrays.sort(keys);
			hash += Arrays.deepHashCode(keys);
			for(Integer key : keys) {
				hash += sinks.get(key).hashCode() * 7;
			}
			/*Arrays.sort(sinks);
			for(Integer i : sinks){
				hash += i * 7;
			}*/
			return hash;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileSinks other = (TileSinks) obj;
		if(other.sinks == null && sinks == null){
			return true;
		}
		if(other.sinks == null || sinks == null){
			return false;
		}
		if(other.sinks.size() != sinks.size()){
			return false;
		}
		Integer[] keys = new Integer[sinks.keySet().size()];
		Integer[] otherKeys = new Integer[sinks.keySet().size()];
		keys = sinks.keySet().toArray(keys);
		otherKeys = other.sinks.keySet().toArray(otherKeys);
		Arrays.sort(keys);
		Arrays.sort(otherKeys);
		if(!Arrays.deepEquals(keys, otherKeys)){
			return false;
		}
		for(int i = 0; i < otherKeys.length; i++){
			if(!sinks.get(keys[i]).equals(other.sinks.get(otherKeys[i]))){
				return false;
			}
		}

		return true;
	}
}
