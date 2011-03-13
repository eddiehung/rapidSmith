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

import edu.byu.ece.rapidSmith.device.WireConnection;

/**
 * A helper class to help remove duplicate objects and reduce memory usage and file
 * size of the Device class. 
 * @author Chris Lavin
 */
public class WireArray implements Serializable {

	private static final long serialVersionUID = 222495247665714923L;
	/** An array of wires */
	public WireConnection[] array;

	/**
	 * Constructor
	 * @param array The Array of wires that correspond to this object.
	 */
	public WireArray(WireConnection[] array){
		this.array = array;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(array);
		return result;
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
		WireArray other = (WireArray) obj;
		if (!Arrays.deepEquals(array, other.array))
			return false;
		return true;
	}
	

	
}
