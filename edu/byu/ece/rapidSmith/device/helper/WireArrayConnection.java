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

public class WireArrayConnection implements Serializable {
	
	private static final long serialVersionUID = 7838184542063263426L;
	public int wire;
	public int wireArrayEnum;
	public int enumeration;
	
	public WireArrayConnection(int w, int wae) {
		wire = w;
		wireArrayEnum = wae;
	}
	
	public Integer[] getIntegerArray(){
		Integer[] tmp = new Integer[2];
		tmp[0] = wire;
		tmp[1] = wireArrayEnum;
		return tmp;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + wire;
		result = prime * result + wireArrayEnum;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WireArrayConnection other = (WireArrayConnection) obj;
		if (wire != other.wire)
			return false;
		if (wireArrayEnum != other.wireArrayEnum)
			return false;
		return true;
	}
	
	
}
