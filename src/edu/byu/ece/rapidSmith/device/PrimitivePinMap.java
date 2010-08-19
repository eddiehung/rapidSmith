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
import java.util.Arrays;
import java.util.HashMap;

/**
 * This is a helper class to help keep the Device files compact.
 * @author Chris Lavin
 */
public class PrimitivePinMap implements Serializable {

	private static final long serialVersionUID = -6398806038703155389L;
	public HashMap<String,Integer> pins;
	
	public PrimitivePinMap(HashMap<String, Integer> pins) {
		this.pins = pins;
	}

	@Override
	public int hashCode() {
		if(pins == null){
			return 0;
		}
		int hash;
		String[] names = new String[pins.keySet().size()];
		names = pins.keySet().toArray(names);
		
		Arrays.sort(names);
		hash = Arrays.deepHashCode(names);
		
		Integer[] values = new Integer[names.length];
		for(int i=0; i < names.length; i++){
			values[i] = pins.get(names[i]);
		}
		
		hash += Arrays.deepHashCode(values);
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
		PrimitivePinMap other = (PrimitivePinMap) obj;

		if(pins == null && other.pins == null){
			return true;
		}
		if(pins == null || other.pins == null){
			return false;
		}
		if(pins.size() != other.pins.size()){
			return false;
		}
		String[] map1Strings = new String[pins.size()];
		map1Strings = pins.keySet().toArray(map1Strings);
		
		String[] map2Strings = new String[other.pins.size()];
		map2Strings = other.pins.keySet().toArray(map2Strings);
		
		Arrays.sort(map1Strings);
		Arrays.sort(map2Strings);
		
		if(!Arrays.deepEquals(map1Strings, map2Strings)){
			return false;
		}
		Integer[] map1Integers = new Integer[pins.size()];
		Integer[] map2Integers = new Integer[other.pins.size()];
		
		for(int i=0; i < map1Strings.length; i++){
			map1Integers[i] = pins.get(map1Strings[i]);
			map2Integers[i] = other.pins.get(map2Strings[i]);
		}
		if(!Arrays.deepEquals(map1Integers, map2Integers)){
			return false;
		}
		return true;
	}
}
